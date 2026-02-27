using System;
using System.Collections.Generic;
using System.Globalization;
using UnityEngine;

/// <summary>
/// Unity C# version of SceneMutator.
/// Handles voice command execution on the AR scene.
/// Migrated from: app/src/main/java/com/arinterior/engine/ar/SceneMutator.kt
/// </summary>
public class SceneMutator : MonoBehaviour
{
    private ARSceneManager _arSceneManager;
    private Dictionary<string, GameObject> _furnitureCache = new Dictionary<string, GameObject>();

    // Voice action types
    public enum VoiceAction
    {
        MOVE,
        RECOLOR,
        REMOVE,
        RESIZE,
        ROTATE,
        RESET,
        UNDO
    }

    // Direction types
    public enum Direction
    {
        LEFT,
        RIGHT,
        FORWARD,
        BACKWARD
    }

    private void Awake()
    {
        _arSceneManager = FindObjectOfType<ARSceneManager>();
    }

    /// <summary>
    /// Apply a voice command to the scene.
    /// </summary>
    /// <param name="commandJson">JSON serialized VoiceCommand</param>
    public void ApplyCommand(string commandJson)
    {
        Debug.Log($"[SceneMutator] ApplyCommand: {commandJson}");

        // Support both JSON format (from Android Gson) and pipe-delimited format
        string actionStr;
        string target;
        string[] extraParams;

        if (commandJson.TrimStart().StartsWith("{"))
        {
            // Parse JSON format: {"action":"MOVE","target":"sofa","amount":0.3,"direction":"LEFT"}
            var parsed = ParseJsonCommand(commandJson);
            actionStr = parsed.action;
            target = parsed.target;
            extraParams = parsed.extras;
        }
        else
        {
            // Pipe-delimited: action|target|param1|param2|...
            string[] parts = commandJson.Split('|');
            if (parts.Length < 2)
            {
                Debug.LogError($"[SceneMutator] Invalid command format: {commandJson}");
                return;
            }
            actionStr = parts[0];
            target = parts[1];
            extraParams = parts.Length > 2 ? parts[2..] : new string[0];
        }

        VoiceAction action = ParseAction(actionStr);

        switch (action)
        {
            case VoiceAction.MOVE:
                float amount = extraParams.Length > 0 ? float.Parse(extraParams[0], CultureInfo.InvariantCulture) : 0.3f;
                Direction direction = extraParams.Length > 1 ? ParseDirection(extraParams[1]) : Direction.RIGHT;
                MoveTarget(target, direction, amount);
                break;

            case VoiceAction.RECOLOR:
                string color = extraParams.Length > 0 ? extraParams[0] : "#FFFFFF";
                RecolorWall(color);
                break;

            case VoiceAction.REMOVE:
                RemoveTarget(target);
                break;

            case VoiceAction.RESIZE:
                float scaleFactor = extraParams.Length > 0 ? float.Parse(extraParams[0], CultureInfo.InvariantCulture) : 1.2f;
                ResizeTarget(target, scaleFactor);
                break;

            case VoiceAction.ROTATE:
                float degrees = extraParams.Length > 0 ? float.Parse(extraParams[0], CultureInfo.InvariantCulture) : 45f;
                RotateTarget(target, degrees);
                break;

            case VoiceAction.RESET:
                ResetScene();
                break;

            case VoiceAction.UNDO:
                UndoLastCommand();
                break;
        }
    }

    /// <summary>
    /// Minimal JSON parser for voice commands.
    /// Extracts action, target, and optional parameters.
    /// </summary>
    private (string action, string target, string[] extras) ParseJsonCommand(string json)
    {
        // Use JsonUtility-compatible approach
        string action = ExtractJsonString(json, "action");
        string target = ExtractJsonString(json, "target");
        
        List<string> extras = new List<string>();
        string amount = ExtractJsonString(json, "amount");
        if (!string.IsNullOrEmpty(amount)) extras.Add(amount);
        string direction = ExtractJsonString(json, "direction");
        if (!string.IsNullOrEmpty(direction)) extras.Add(direction);
        string color = ExtractJsonString(json, "color");
        if (!string.IsNullOrEmpty(color)) extras.Add(color);

        return (action, target, extras.ToArray());
    }

    /// <summary>
    /// Simple JSON string value extractor.
    /// </summary>
    private string ExtractJsonString(string json, string key)
    {
        string searchKey = $"\"{key}\"";
        int keyIndex = json.IndexOf(searchKey, StringComparison.OrdinalIgnoreCase);
        if (keyIndex < 0) return null;

        int colonIndex = json.IndexOf(':', keyIndex + searchKey.Length);
        if (colonIndex < 0) return null;

        // Skip whitespace after colon
        int valueStart = colonIndex + 1;
        while (valueStart < json.Length && char.IsWhiteSpace(json[valueStart]))
            valueStart++;

        if (valueStart >= json.Length) return null;

        // Check if value is a string (quoted) or number
        if (json[valueStart] == '"')
        {
            int valueEnd = json.IndexOf('"', valueStart + 1);
            if (valueEnd < 0) return null;
            return json.Substring(valueStart + 1, valueEnd - valueStart - 1);
        }
        else
        {
            // Number or boolean — read until comma, }, or whitespace
            int valueEnd = valueStart;
            while (valueEnd < json.Length && json[valueEnd] != ',' && json[valueEnd] != '}' && !char.IsWhiteSpace(json[valueEnd]))
                valueEnd++;
            return json.Substring(valueStart, valueEnd - valueStart);
        }
    }

    private void MoveTarget(string target, Direction direction, float amount)
    {
        Vector3 offset = Vector3.zero;
        
        switch (direction)
        {
            case Direction.LEFT:
                offset = new Vector3(-amount, 0, 0);
                break;
            case Direction.RIGHT:
                offset = new Vector3(amount, 0, 0);
                break;
            case Direction.FORWARD:
                offset = new Vector3(0, 0, -amount);
                break;
            case Direction.BACKWARD:
                offset = new Vector3(0, 0, amount);
                break;
        }

        // Find and move furniture matching target
        var furniture = FindFurnitureByTarget(target);
        if (furniture != null)
        {
            Vector3 newPos = furniture.transform.position + offset;
            _arSceneManager?.MoveFurniture($"{GetFurnitureId(furniture)}|{newPos.x}|{newPos.y}|{newPos.z}");
        }
    }

    private void RecolorWall(string colorHex)
    {
        _arSceneManager?.RecolorWall(colorHex);
    }

    private void RemoveTarget(string target)
    {
        var furniture = FindFurnitureByTarget(target);
        if (furniture != null)
        {
            string itemId = GetFurnitureId(furniture);
            _arSceneManager?.RemoveFurniture(itemId);
            _furnitureCache.Remove(itemId);
        }
    }

    private void ResizeTarget(string target, float scaleFactor)
    {
        var furniture = FindFurnitureByTarget(target);
        if (furniture != null)
        {
            string itemId = GetFurnitureId(furniture);
            _arSceneManager?.ScaleFurniture($"{itemId}|{scaleFactor}");
        }
    }

    private void RotateTarget(string target, float degrees)
    {
        var furniture = FindFurnitureByTarget(target);
        if (furniture != null)
        {
            string itemId = GetFurnitureId(furniture);
            _arSceneManager?.RotateFurniture($"{itemId}|{degrees}");
        }
    }

    private void ResetScene()
    {
        _arSceneManager?.ClearScene();
        _furnitureCache.Clear();
    }

    private void UndoLastCommand()
    {
        // Implement command history for undo functionality
        Debug.Log("[SceneMutator] Undo not yet implemented - requires command history stack");
    }

    // ─────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────

    private GameObject FindFurnitureByTarget(string target)
    {
        string normalizedTarget = target.ToLower().Replace(" ", "_");
        
        // First check cache
        foreach (var kvp in _furnitureCache)
        {
            if (MatchesTarget(kvp.Key, normalizedTarget))
                return kvp.Value;
        }

        // Search in scene
        var furnitureDataList = FindObjectsOfType<FurnitureData>();
        foreach (var fd in furnitureDataList)
        {
            if (MatchesTarget(fd.itemId, normalizedTarget))
            {
                _furnitureCache[fd.itemId] = fd.gameObject;
                return fd.gameObject;
            }
        }

        return null;
    }

    private bool MatchesTarget(string itemId, string target)
    {
        string normalizedId = itemId.ToLower();
        
        return normalizedId.Contains(target) ||
               target.Contains(normalizedId.Split('_')[0]);
    }

    private string GetFurnitureId(GameObject furniture)
    {
        var fd = furniture.GetComponent<FurnitureData>();
        return fd != null ? fd.itemId : furniture.name;
    }

    private VoiceAction ParseAction(string actionStr)
    {
        switch (actionStr.ToUpper())
        {
            case "MOVE": return VoiceAction.MOVE;
            case "RECOLOR": return VoiceAction.RECOLOR;
            case "REMOVE": return VoiceAction.REMOVE;
            case "RESIZE": return VoiceAction.RESIZE;
            case "ROTATE": return VoiceAction.ROTATE;
            case "RESET": return VoiceAction.RESET;
            case "UNDO": return VoiceAction.UNDO;
            default: return VoiceAction.MOVE;
        }
    }

    private Direction ParseDirection(string dirStr)
    {
        switch (dirStr.ToUpper())
        {
            case "LEFT": return Direction.LEFT;
            case "RIGHT": return Direction.RIGHT;
            case "FORWARD": return Direction.FORWARD;
            case "BACKWARD": return Direction.BACKWARD;
            default: return Direction.RIGHT;
        }
    }
}
