using System.Collections.Generic;
using UnityEngine;
using UnityEngine.XR.ARFoundation;

/// <summary>
/// Unity C# version of AutoSnapEngine.
/// Applies smart placement rules based on furniture type and room geometry.
/// Migrated from: app/src/main/java/com/arinterior/engine/ar/AutoSnapEngine.kt
/// </summary>
public class AutoSnapEngine : MonoBehaviour
{
    [Header("Snap Parameters")]
    [SerializeField] private float _wallGap = 0.10f;      // 10cm gap from wall
    [SerializeField] private float _eyeLevelHeight = 1.5f; // Painting center height
    [SerializeField] private float _sconceHeight = 1.8f;   // Sconce mounting height
    [SerializeField] private float _flankOffset = 0.6f;    // Sconce offset from artwork center
    [SerializeField] private float _tableOffset = 0.8f;    // Table distance in front of sofa

    private ARPlaneManager _planeManager;

    private void Awake()
    {
        _planeManager = FindObjectOfType<ARPlaneManager>();
    }

    /// <summary>
    /// Snap a placed item to the best position based on its category and room geometry.
    /// </summary>
    public Vector3 SnapItem(string itemId, string category, Vector3 currentPosition, List<Vector3> existingPositions)
    {
        switch (category.ToLower())
        {
            case "seating":
            case "sofa":
            case "chair":
                return SnapToWall(currentPosition);

            case "table":
            case "coffee_table":
                return SnapToCenterOfSeating(currentPosition, existingPositions);

            case "rug":
                return SnapUnderFurniture(currentPosition, existingPositions);

            default:
                return currentPosition;
        }
    }

    /// <summary>
    /// Public entry point called from AndroidToUnityBridge.
    /// Finds the furniture GameObject by itemId and applies auto-snap.
    /// </summary>
    public void ApplyAutoSnap(string itemId, string category)
    {
        // Find furniture by name or FurnitureData component
        GameObject furniture = GameObject.Find(itemId);
        if (furniture == null)
        {
            Debug.LogWarning($"[AutoSnapEngine] Furniture not found: {itemId}");
            return;
        }

        // Gather existing furniture positions
        var existingPositions = new List<Vector3>();
        var allFurniture = FindObjectsOfType<Transform>();
        foreach (var t in allFurniture)
        {
            if (t.gameObject != furniture && t.CompareTag("Furniture"))
            {
                existingPositions.Add(t.position);
            }
        }

        Vector3 snappedPos = SnapItem(itemId, category, furniture.transform.position, existingPositions);
        furniture.transform.position = snappedPos;
        Debug.Log($"[AutoSnapEngine] Applied auto-snap to {itemId}: {snappedPos}");
    }

    /// <summary>
    /// Snap wall items to appropriate heights.
    /// </summary>
    public Vector3 SnapWallItem(string itemId, string category, Vector3 currentPosition, string wallId)
    {
        switch (category.ToLower())
        {
            case "wall_art":
            case "painting":
            case "art":
                return new Vector3(currentPosition.x, _eyeLevelHeight, currentPosition.z);

            case "wall_light":
            case "sconce":
                return SnapSconceToArt(currentPosition, wallId);

            default:
                return currentPosition;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Snap Strategies
    // ─────────────────────────────────────────────────────────────

    /// <summary>
    /// Sofas and large furniture snap to nearest wall with ~10cm gap.
    /// </summary>
    private Vector3 SnapToWall(Vector3 currentPosition)
    {
        if (_planeManager == null) return currentPosition;

        // Find the closest wall plane
        ARPlane nearestWall = null;
        float minDistance = float.MaxValue;

        foreach (var plane in _planeManager.trackables)
        {
            if (plane.alignment == PlaneAlignment.Vertical)
            {
                float distance = Vector3.Distance(plane.transform.position, currentPosition);
                if (distance < minDistance)
                {
                    minDistance = distance;
                    nearestWall = plane;
                }
            }
        }

        if (nearestWall != null)
        {
            // Snap to wall with gap
            Vector3 wallNormal = -nearestWall.transform.forward;
            Vector3 snappedPosition = nearestWall.transform.position + wallNormal * _wallGap;
            
            // Keep X and Y, adjust Z to wall
            return new Vector3(
                currentPosition.x,
                currentPosition.y,
                snappedPosition.z
            );
        }

        // No wall detected - place at default position
        return currentPosition;
    }

    /// <summary>
    /// Coffee tables snap to center of seating arrangement.
    /// </summary>
    private Vector3 SnapToCenterOfSeating(Vector3 currentPosition, List<Vector3> existingPositions)
    {
        if (existingPositions == null || existingPositions.Count == 0)
            return currentPosition;

        // Find seating positions (items with "sofa" or "chair" in their ID)
        // For now, average all positions
        Vector3 sum = Vector3.zero;
        foreach (var pos in existingPositions)
        {
            sum += pos;
        }
        
        Vector3 center = sum / existingPositions.Count;
        
        // Add offset to place in front of seating
        return new Vector3(
            center.x,
            currentPosition.y, // Keep original height
            center.z + _tableOffset
        );
    }

    /// <summary>
    /// Rugs snap centered under furniture grouping.
    /// </summary>
    private Vector3 SnapUnderFurniture(Vector3 currentPosition, List<Vector3> existingPositions)
    {
        if (existingPositions == null || existingPositions.Count == 0)
            return currentPosition;

        // Calculate centroid of existing furniture
        Vector3 sum = Vector3.zero;
        foreach (var pos in existingPositions)
        {
            sum += pos;
        }

        Vector3 center = sum / existingPositions.Count;

        return new Vector3(
            center.x,
            0.01f, // Just above floor for rug
            center.z
        );
    }

    /// <summary>
    /// Snap sconce to flank existing artwork.
    /// </summary>
    private Vector3 SnapSconceToArt(Vector3 currentPosition, string wallId)
    {
        // Find artwork on the same wall
        var furnitureDataList = FindObjectsOfType<FurnitureData>();
        ARPlane artWall = FindWallById(wallId);

        if (artWall != null)
        {
            foreach (var fd in furnitureDataList)
            {
                // Check if it's artwork on the same wall
                if (fd.itemId.Contains("art") || fd.itemId.Contains("painting"))
                {
                    // Position sconce flanking the artwork
                    return new Vector3(
                        fd.transform.position.x + _flankOffset,
                        _sconceHeight,
                        fd.transform.position.z
                    );
                }
            }
        }

        // No artwork found - place at default sconce height
        return new Vector3(
            currentPosition.x,
            _sconceHeight,
            currentPosition.z
        );
    }

    /// <summary>
    /// Apply auto-snap to an existing furniture item.
    /// </summary>
    public void ApplyAutoSnap(string itemId, string category)
    {
        Debug.Log($"[AutoSnapEngine] Applying auto-snap to {itemId} category {category}");

        var furniture = FindFurnitureById(itemId);
        if (furniture == null)
        {
            Debug.LogWarning($"[AutoSnapEngine] Furniture not found: {itemId}");
            return;
        }

        // Get existing furniture positions
        var positions = new List<Vector3>();
        foreach (var fd in FindObjectsOfType<FurnitureData>())
        {
            if (fd.itemId != itemId)
            {
                positions.Add(fd.transform.position);
            }
        }

        // Determine if this is a wall item or floor item
        bool isWallItem = IsWallCategory(category);
        
        Vector3 newPosition;
        if (isWallItem)
        {
            newPosition = SnapWallItem(itemId, category, furniture.transform.position, "");
        }
        else
        {
            newPosition = SnapItem(itemId, category, furniture.transform.position, positions);
        }

        // Move the furniture
        furniture.transform.position = newPosition;

        // Notify Android
        AndroidToUnityBridge.SendObjectTapped(itemId);
    }

    // ─────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────

    private GameObject FindFurnitureById(string itemId)
    {
        var furnitureDataList = FindObjectsOfType<FurnitureData>();
        foreach (var fd in furnitureDataList)
        {
            if (fd.itemId == itemId)
                return fd.gameObject;
        }
        return null;
    }

    private ARPlane FindWallById(string wallId)
    {
        if (_planeManager == null) return null;

        foreach (var plane in _planeManager.trackables)
        {
            if (plane.alignment == PlaneAlignment.Vertical)
            {
                // In production, compare wall IDs
                return plane;
            }
        }
        return null;
    }

    private bool IsWallCategory(string category)
    {
        string lower = category.ToLower();
        return lower.Contains("wall") || 
               lower.Contains("art") || 
               lower.Contains("painting") ||
               lower.Contains("sconce") ||
               lower.Contains("light");
    }
}
