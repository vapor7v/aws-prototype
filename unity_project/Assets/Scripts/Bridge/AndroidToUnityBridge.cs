using UnityEngine;
#if UNITY_ANDROID
using UnityEngine.Android;
#endif

/// <summary>
/// Bridge script for receiving commands from Android and sending events back.
/// This script handles the Android → Unity direction of communication.
/// 
/// Methods in this class are called from Android via UnityPlayer.UnitySendMessage().
/// The GameObject this script is attached to must be named "AndroidBridge".
/// </summary>
public class AndroidToUnityBridge : MonoBehaviour
{
    private static AndroidToUnityBridge _instance;
    private ARSceneManager _arSceneManager;
    private string _lastCandidatesJson;

    private void Awake()
    {
        _instance = this;
        
        // Find the AR scene manager
        _arSceneManager = FindObjectOfType<ARSceneManager>();
        
        if (_arSceneManager == null)
        {
            Debug.LogError("[AndroidToUnityBridge] ARSceneManager not found!");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Android → Unity Commands
    // These methods are called from Android via JNI
    // ─────────────────────────────────────────────────────────────

    /// <summary>
    /// Initialize AR session.
    /// Called from: UnityARBridge.initializeAR()
    /// </summary>
    public void InitializeAR(string _)
    {
        Debug.Log("[AndroidToUnityBridge] InitializeAR");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.InitializeAR();
        }
        
        // Request camera permission on Android
        RequestCameraPermission();
    }

    /// <summary>
    /// Start plane detection.
    /// </summary>
    public void StartPlaneDetection(string _)
    {
        Debug.Log("[AndroidToUnityBridge] StartPlaneDetection");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.StartPlaneDetection();
        }
    }

    /// <summary>
    /// Stop plane detection.
    /// </summary>
    public void StopPlaneDetection(string _)
    {
        Debug.Log("[AndroidToUnityBridge] StopPlaneDetection");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.StopPlaneDetection();
        }
    }

    /// <summary>
    /// Place furniture item.
    /// Format: "itemId|positionX|positionY|positionZ|rotationY|scale"
    /// </summary>
    public void PlaceFurniture(string data)
    {
        Debug.Log($"[AndroidToUnityBridge] PlaceFurniture: {data}");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.PlaceFurniture(data);
        }
    }

    /// <summary>
    /// Move furniture item.
    /// Format: "itemId|positionX|positionY|positionZ"
    /// </summary>
    public void MoveFurniture(string data)
    {
        Debug.Log($"[AndroidToUnityBridge] MoveFurniture: {data}");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.MoveFurniture(data);
        }
    }

    /// <summary>
    /// Rotate furniture item.
    /// Format: "itemId|degrees"
    /// </summary>
    public void RotateFurniture(string data)
    {
        Debug.Log($"[AndroidToUnityBridge] RotateFurniture: {data}");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.RotateFurniture(data);
        }
    }

    /// <summary>
    /// Scale furniture item.
    /// Format: "itemId|scaleFactor"
    /// </summary>
    public void ScaleFurniture(string data)
    {
        Debug.Log($"[AndroidToUnityBridge] ScaleFurniture: {data}");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.ScaleFurniture(data);
        }
    }

    /// <summary>
    /// Remove furniture item.
    /// Format: "itemId"
    /// </summary>
    public void RemoveFurniture(string itemId)
    {
        Debug.Log($"[AndroidToUnityBridge] RemoveFurniture: {itemId}");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.RemoveFurniture(itemId);
        }
    }

    /// <summary>
    /// Apply wall color.
    /// Format: "#RRGGBB"
    /// </summary>
    public void RecolorWall(string colorHex)
    {
        Debug.Log($"[AndroidToUnityBridge] RecolorWall: {colorHex}");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.RecolorWall(colorHex);
        }
    }

    /// <summary>
    /// Clear all furniture.
    /// </summary>
    public void ClearScene(string _)
    {
        Debug.Log("[AndroidToUnityBridge] ClearScene");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.ClearScene();
        }
    }

    /// <summary>
    /// Load design candidates from JSON.
    /// </summary>
    public void LoadCandidates(string json)
    {
        Debug.Log($"[AndroidToUnityBridge] LoadCandidates: {json.Substring(0, Mathf.Min(100, json.Length))}...");
        
        // Store candidates JSON for later use
        _lastCandidatesJson = json;
    }

    /// <summary>
    /// Select a candidate to display.
    /// </summary>
    public void SelectCandidate(string candidateId)
    {
        Debug.Log($"[AndroidToUnityBridge] SelectCandidate: {candidateId}");
        
        // Clear current scene and load the selected candidate
        if (_arSceneManager != null)
        {
            _arSceneManager.ClearScene();
        }
    }

    /// <summary>
    /// Enable/disable object selection.
    /// </summary>
    public void SetObjectSelectionEnabled(string enabled)
    {
        Debug.Log($"[AndroidToUnityBridge] SetObjectSelectionEnabled: {enabled}");
    }

    /// <summary>
    /// Set editing mode.
    /// </summary>
    public void SetEditingMode(string mode)
    {
        Debug.Log($"[AndroidToUnityBridge] SetEditingMode: {mode}");
        
        if (_arSceneManager != null)
        {
            _arSceneManager.SetEditingMode(mode);
        }
    }

    /// <summary>
    /// Apply auto-snap to an item.
    /// Format: "itemId|category"
    /// </summary>
    public void ApplyAutoSnap(string data)
    {
        Debug.Log($"[AndroidToUnityBridge] ApplyAutoSnap: {data}");
        
        string[] parts = data.Split('|');
        if (parts.Length >= 2)
        {
            string itemId = parts[0];
            string category = parts[1];
            
            // Apply auto-snap logic
            ApplyAutoSnapToItem(itemId, category);
        }
    }

    /// <summary>
    /// Process voice command.
    /// </summary>
    public void ProcessVoiceCommand(string commandJson)
    {
        Debug.Log($"[AndroidToUnityBridge] ProcessVoiceCommand: {commandJson}");
        
        // Forward to SceneMutator
        var sceneMutator = FindObjectOfType<SceneMutator>();
        if (sceneMutator != null)
        {
            sceneMutator.ApplyCommand(commandJson);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Unity → Android Callbacks
    // These methods send events back to Android
    // ─────────────────────────────────────────────────────────────

    /// <summary>
    /// Send floor detected event to Android.
    /// </summary>
    public static void SendFloorDetected()
    {
#if UNITY_ANDROID
        CallAndroidStaticMethod("onFloorDetected", "");
#endif
    }

    /// <summary>
    /// Send wall detected event to Android.
    /// </summary>
    public static void SendWallDetected()
    {
#if UNITY_ANDROID
        CallAndroidStaticMethod("onWallDetected", "");
#endif
    }

    /// <summary>
    /// Send plane count change event to Android.
    /// </summary>
    public static void SendPlaneCountChanged(int count)
    {
#if UNITY_ANDROID
        CallAndroidStaticMethod("onPlaneCountChanged", count.ToString());
#endif
    }

    /// <summary>
    /// Send object tapped event to Android.
    /// </summary>
    public static void SendObjectTapped(string itemId)
    {
#if UNITY_ANDROID
        CallAndroidStaticMethod("onObjectTapped", itemId);
#endif
    }

    /// <summary>
    /// Send tracking state change event to Android.
    /// </summary>
    public static void SendTrackingStateChanged(string state)
    {
#if UNITY_ANDROID
        CallAndroidStaticMethod("onTrackingStateChanged", state);
#endif
    }

    /// <summary>
    /// Send AR session error event to Android.
    /// </summary>
    public static void SendARSessionError(string error)
    {
#if UNITY_ANDROID
        CallAndroidStaticMethod("onARSessionError", error);
#endif
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────

    private void ApplyAutoSnapToItem(string itemId, string category)
    {
        // Forward to AutoSnapEngine
        var autoSnap = FindObjectOfType<AutoSnapEngine>();
        if (autoSnap != null)
        {
            autoSnap.ApplyAutoSnap(itemId, category);
        }
        else
        {
            Debug.LogWarning($"[AndroidToUnityBridge] AutoSnapEngine not found for {itemId}");
        }
    }

#if UNITY_ANDROID
    private static void CallAndroidStaticMethod(string methodName, string param)
    {
        try
        {
            using (var bridgeClass = new AndroidJavaClass("com.arinterior.engine.unity.UnityARBridge"))
            {
                if (string.IsNullOrEmpty(param))
                {
                    bridgeClass.CallStatic(methodName);
                }
                else
                {
                    // Try to parse as int first (for onPlaneCountChanged)
                    int intParam;
                    if (int.TryParse(param, out intParam))
                    {
                        bridgeClass.CallStatic(methodName, intParam);
                    }
                    else
                    {
                        bridgeClass.CallStatic(methodName, param);
                    }
                }
            }
        }
        catch (System.Exception e)
        {
            UnityEngine.Debug.LogError($"[AndroidToUnityBridge] Error calling Android: {e.Message}");
        }
    }
#endif

    private void RequestCameraPermission()
    {
#if UNITY_ANDROID
        if (!Permission.HasUserAuthorizedPermission(Permission.Camera))
        {
            Permission.RequestUserPermission(Permission.Camera);
        }
#endif
    }
}
