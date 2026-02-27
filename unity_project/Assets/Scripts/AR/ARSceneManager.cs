using System;
using System.Collections.Generic;
using System.Globalization;
using UnityEngine;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;

/// <summary>
/// Main AR scene manager that handles AR session lifecycle and coordinates AR subsystems.
/// This is the entry point for Unity AR operations triggered from Android.
/// </summary>
public class ARSceneManager : MonoBehaviour
{
    [Header("AR Components")]
    [SerializeField] private ARSession _arSession;
    [SerializeField] private ARCameraManager _arCameraManager;
    [SerializeField] private ARPlaneManager _planeManager;
    [SerializeField] private ARRaycastManager _raycastManager;

    [Header("Prefabs")]
    [SerializeField] private GameObject _furniturePlaceholderPrefab;
    [SerializeField] private GameObject _selectionIndicatorPrefab;

    // Event callbacks for Android
    public static event Action OnFloorDetected;
    public static event Action OnWallDetected;
    public static event Action<int> OnPlaneCountChanged;
    public static event Action<string> OnObjectTapped;
    public static event Action<string> OnTrackingStateChanged;
    public static event Action<string> OnARSessionError;

    // State tracking
    private bool _floorDetected = false;
    private bool _wallDetected = false;
    private int _planeCount = 0;
    private GameObject _selectionIndicator;

    // Furniture dictionary
    private Dictionary<string, GameObject> _placedFurniture = new Dictionary<string, GameObject>();

    private void Awake()
    {
        // Get or create AR components
        if (_arSession == null)
            _arSession = FindObjectOfType<ARSession>();
        
        if (_arCameraManager == null)
            _arCameraManager = FindObjectOfType<ARCameraManager>();
        
        if (_planeManager == null)
            _planeManager = FindObjectOfType<ARPlaneManager>();
        
        if (_raycastManager == null)
            _raycastManager = FindObjectOfType<ARRaycastManager>();

        // Create selection indicator
        if (_selectionIndicatorPrefab != null)
        {
            _selectionIndicator = Instantiate(_selectionIndicatorPrefab);
            _selectionIndicator.SetActive(false);
        }
    }

    private void OnEnable()
    {
        // Subscribe to AR events
        if (_planeManager != null)
        {
            _planeManager.trackablesChanged += OnTrackablesChanged;
        }

        if (_arCameraManager != null)
        {
            _arCameraManager.frameReceived += OnFrameReceived;
        }

        ARSession.stateChanged += OnARSessionStateChanged;

        // Subscribe to static events to relay to Android
        OnFloorDetected += HandleFloorDetected;
        OnWallDetected += HandleWallDetected;
        OnPlaneCountChanged += HandlePlaneCountChanged;
        OnObjectTapped += HandleObjectTapped;
        OnTrackingStateChanged += HandleTrackingStateChanged;
        OnARSessionError += HandleARSessionError;
    }

    private void OnDisable()
    {
        // Unsubscribe from AR events
        if (_planeManager != null)
        {
            _planeManager.trackablesChanged -= OnTrackablesChanged;
        }

        if (_arCameraManager != null)
        {
            _arCameraManager.frameReceived -= OnFrameReceived;
        }

        ARSession.stateChanged -= OnARSessionStateChanged;

        // Unsubscribe static event relays
        OnFloorDetected -= HandleFloorDetected;
        OnWallDetected -= HandleWallDetected;
        OnPlaneCountChanged -= HandlePlaneCountChanged;
        OnObjectTapped -= HandleObjectTapped;
        OnTrackingStateChanged -= HandleTrackingStateChanged;
        OnARSessionError -= HandleARSessionError;
    }

    // ─────────────────────────────────────────────────────────────
    // Android → Unity Commands (called from AndroidBridge)
    // ─────────────────────────────────────────────────────────────

    /// <summary>
    /// Initialize AR session with default configuration.
    /// </summary>
    public void InitializeAR()
    {
        Debug.Log("[ARSceneManager] InitializeAR called");
        
        if (_arSession != null)
        {
            _arSession.enabled = true;
        }

        // Start plane detection
        StartPlaneDetection();
    }

    /// <summary>
    /// Start detecting planes.
    /// </summary>
    public void StartPlaneDetection()
    {
        Debug.Log("[ARSceneManager] StartPlaneDetection called");
        
        if (_planeManager != null)
        {
            _planeManager.enabled = true;
            
            foreach (var plane in _planeManager.trackables)
            {
                plane.gameObject.SetActive(true);
            }
        }
    }

    /// <summary>
    /// Stop detecting planes.
    /// </summary>
    public void StopPlaneDetection()
    {
        Debug.Log("[ARSceneManager] StopPlaneDetection called");
        
        if (_planeManager != null)
        {
            _planeManager.enabled = false;
        }
    }

    /// <summary>
    /// Place a furniture item at the specified position.
    /// </summary>
    public void PlaceFurniture(string data)
    {
        Debug.Log($"[ARSceneManager] PlaceFurniture: {data}");
        
        string[] parts = data.Split('|');
        if (parts.Length < 6)
        {
            Debug.LogError($"[ARSceneManager] Invalid PlaceFurniture data: {data}");
            return;
        }

        string itemId = parts[0];
        float posX = float.Parse(parts[1], CultureInfo.InvariantCulture);
        float posY = float.Parse(parts[2], CultureInfo.InvariantCulture);
        float posZ = float.Parse(parts[3], CultureInfo.InvariantCulture);
        float rotationY = float.Parse(parts[4], CultureInfo.InvariantCulture);
        float scale = float.Parse(parts[5], CultureInfo.InvariantCulture);

        Vector3 position = new Vector3(posX, posY, posZ);
        Quaternion rotation = Quaternion.Euler(0, rotationY, 0);

        PlaceFurnitureObject(itemId, position, rotation, scale);
    }

    /// <summary>
    /// Move an existing furniture item.
    /// </summary>
    public void MoveFurniture(string data)
    {
        Debug.Log($"[ARSceneManager] MoveFurniture: {data}");
        
        string[] parts = data.Split('|');
        if (parts.Length < 4) return;

        string itemId = parts[0];
        float posX = float.Parse(parts[1], CultureInfo.InvariantCulture);
        float posY = float.Parse(parts[2], CultureInfo.InvariantCulture);
        float posZ = float.Parse(parts[3], CultureInfo.InvariantCulture);

        if (_placedFurniture.TryGetValue(itemId, out GameObject furniture))
        {
            furniture.transform.position = new Vector3(posX, posY, posZ);
        }
    }

    /// <summary>
    /// Rotate a furniture item.
    /// </summary>
    public void RotateFurniture(string data)
    {
        Debug.Log($"[ARSceneManager] RotateFurniture: {data}");
        
        string[] parts = data.Split('|');
        if (parts.Length < 2) return;

        string itemId = parts[0];
        float degrees = float.Parse(parts[1], CultureInfo.InvariantCulture);

        if (_placedFurniture.TryGetValue(itemId, out GameObject furniture))
        {
            furniture.transform.Rotate(0, degrees, 0);
        }
    }

    /// <summary>
    /// Scale a furniture item.
    /// </summary>
    public void ScaleFurniture(string data)
    {
        Debug.Log($"[ARSceneManager] ScaleFurniture: {data}");
        
        string[] parts = data.Split('|');
        if (parts.Length < 2) return;

        string itemId = parts[0];
        float scaleFactor = float.Parse(parts[1], CultureInfo.InvariantCulture);

        if (_placedFurniture.TryGetValue(itemId, out GameObject furniture))
        {
            furniture.transform.localScale *= scaleFactor;
        }
    }

    /// <summary>
    /// Remove a furniture item.
    /// </summary>
    public void RemoveFurniture(string itemId)
    {
        Debug.Log($"[ARSceneManager] RemoveFurniture: {itemId}");
        
        if (_placedFurniture.TryGetValue(itemId, out GameObject furniture))
        {
            Destroy(furniture);
            _placedFurniture.Remove(itemId);
        }
    }

    /// <summary>
    /// Apply wall color.
    /// </summary>
    public void RecolorWall(string colorHex)
    {
        Debug.Log($"[ARSceneManager] RecolorWall: {colorHex}");
        
        // Find wall planes and apply color
        if (_planeManager != null)
        {
            foreach (var plane in _planeManager.trackables)
            {
                if (plane.alignment == PlaneAlignment.Vertical)
                {
                    var renderer = plane.GetComponent<MeshRenderer>();
                    if (renderer != null)
                    {
                        Color color;
                        if (ColorUtility.TryParseHtmlString(colorHex, out color))
                        {
                            renderer.material.color = color;
                        }
                    }
                }
            }
        }
    }

    /// <summary>
    /// Clear all furniture.
    /// </summary>
    public void ClearScene()
    {
        Debug.Log("[ARSceneManager] ClearScene called");
        
        foreach (var kvp in _placedFurniture)
        {
            Destroy(kvp.Value);
        }
        _placedFurniture.Clear();
        
        _floorDetected = false;
        _wallDetected = false;
    }

    /// <summary>
    /// Enable/disable object selection.
    /// </summary>
    public void SetObjectSelectionEnabled(string enabled)
    {
        Debug.Log($"[ARSceneManager] SetObjectSelectionEnabled: {enabled}");
        // Object selection is always enabled in this implementation
    }

    /// <summary>
    /// Set editing mode.
    /// </summary>
    public void SetEditingMode(string mode)
    {
        Debug.Log($"[ARSceneManager] SetEditingMode: {mode}");
    }

    // ─────────────────────────────────────────────────────────────
    // Private Methods
    // ─────────────────────────────────────────────────────────────

    private void PlaceFurnitureObject(string itemId, Vector3 position, Quaternion rotation, float scale)
    {
        // Remove existing item with same ID
        if (_placedFurniture.ContainsKey(itemId))
        {
            Destroy(_placedFurniture[itemId]);
        }

        // Create furniture object (in production, load from Addressables or Resources)
        GameObject furniture = CreateFurnitureModel(itemId);
        
        if (furniture != null)
        {
            furniture.transform.position = position;
            furniture.transform.rotation = rotation;
            furniture.transform.localScale = Vector3.one * scale;
            
            _placedFurniture[itemId] = furniture;
            
            // Create anchor on plane if available
            CreateAnchorForFurniture(furniture, position);
        }
    }

    private GameObject CreateFurnitureModel(string itemId)
    {
        // In production, this would load GLB/GLTF from Addressables or Resources
        // For now, create a placeholder cube
        GameObject model = GameObject.CreatePrimitive(PrimitiveType.Cube);
        model.name = itemId;
        
        // Add collider for tap detection
        var collider = model.GetComponent<Collider>();
        if (collider == null)
        {
            model.AddComponent<BoxCollider>();
        }

        // Add furniture data component
        var furnitureData = model.AddComponent<FurnitureData>();
        furnitureData.itemId = itemId;

        return model;
    }

    private void CreateAnchorForFurniture(GameObject furniture, Vector3 position)
    {
        // Add an ARAnchor component directly to the furniture to stabilize it
        // ARAnchor will attach to the nearest trackable automatically
        if (furniture.GetComponent<ARAnchor>() == null)
        {
            furniture.AddComponent<ARAnchor>();
        }
    }

    private void OnTrackablesChanged(ARTrackablesChangedEventArgs<ARPlane> args)
    {
        foreach (var plane in args.added)
        {
            CheckPlaneType(plane);
        }

        foreach (var plane in args.updated)
        {
            CheckPlaneType(plane);
        }

        // Update plane count
        if (_planeManager != null)
        {
            int newCount = 0;
            foreach (var plane in _planeManager.trackables)
            {
                if (plane.trackingState == TrackingState.Tracking)
                    newCount++;
            }
            
            if (newCount != _planeCount)
            {
                _planeCount = newCount;
                OnPlaneCountChanged?.Invoke(_planeCount);
            }
        }
    }

    private void CheckPlaneType(ARPlane plane)
    {
        if (plane.trackingState != TrackingState.Tracking) return;

        // Check for horizontal plane (floor)
        if (!_floorDetected && plane.alignment == PlaneAlignment.HorizontalUp)
        {
            _floorDetected = true;
            Debug.Log("[ARSceneManager] Floor detected!");
            OnFloorDetected?.Invoke();
        }

        // Check for vertical plane (wall)
        if (!_wallDetected && plane.alignment == PlaneAlignment.Vertical)
        {
            _wallDetected = true;
            Debug.Log("[ARSceneManager] Wall detected!");
            OnWallDetected?.Invoke();
        }
    }

    private void OnFrameReceived(ARCameraFrameEventArgs args)
    {
        // Light estimation data is available via args.lightEstimation
        // Apply to scene directional light if needed
        if (args.lightEstimation.averageBrightness.HasValue)
        {
            // Could adjust ambient intensity here
        }
    }

    private void OnARSessionStateChanged(ARSessionStateChangedEventArgs args)
    {
        Debug.Log($"[ARSceneManager] AR Session state: {args.state}");
        
        string stateStr = args.state.ToString();
        OnTrackingStateChanged?.Invoke(stateStr);

        if (args.state == ARSessionState.Error)
        {
            OnARSessionError?.Invoke("AR Session Error");
        }
    }

    // Handle touch for object selection
    private void Update()
    {
        if (Input.touchCount > 0)
        {
            Touch touch = Input.GetTouch(0);
            
            if (touch.phase == TouchPhase.Began)
            {
                HandleTap(touch.position);
            }
        }
    }

    private void HandleTap(Vector2 screenPosition)
    {
        // Use Physics.Raycast from camera to detect furniture objects (GameObjects)
        // instead of ARRaycastManager which only hits AR trackables (planes)
        Camera arCamera = Camera.main;
        if (arCamera == null) return;

        Ray ray = arCamera.ScreenPointToRay(screenPosition);
        RaycastHit hit;

        if (Physics.Raycast(ray, out hit, 100f))
        {
            var furnitureData = hit.collider.GetComponent<FurnitureData>();
            if (furnitureData == null)
            {
                furnitureData = hit.collider.GetComponentInParent<FurnitureData>();
            }

            if (furnitureData != null)
            {
                Debug.Log($"[ARSceneManager] Tapped on furniture: {furnitureData.itemId}");
                OnObjectTapped?.Invoke(furnitureData.itemId);

                // Show selection indicator
                ShowSelectionIndicator(furnitureData.transform.position);
                return;
            }
        }

        // Hide selection if tapped elsewhere
        if (_selectionIndicator != null)
        {
            _selectionIndicator.SetActive(false);
        }
    }

    private void ShowSelectionIndicator(Vector3 position)
    {
        if (_selectionIndicator != null)
        {
            _selectionIndicator.transform.position = position;
            _selectionIndicator.SetActive(true);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Unity → Android Event Relays
    // ─────────────────────────────────────────────────────────────

    private static void HandleFloorDetected()
    {
        AndroidToUnityBridge.SendFloorDetected();
    }

    private static void HandleWallDetected()
    {
        AndroidToUnityBridge.SendWallDetected();
    }

    private static void HandlePlaneCountChanged(int count)
    {
        AndroidToUnityBridge.SendPlaneCountChanged(count);
    }

    private static void HandleObjectTapped(string itemId)
    {
        AndroidToUnityBridge.SendObjectTapped(itemId);
    }

    private static void HandleTrackingStateChanged(string state)
    {
        AndroidToUnityBridge.SendTrackingStateChanged(state);
    }

    private static void HandleARSessionError(string error)
    {
        AndroidToUnityBridge.SendARSessionError(error);
    }
}

/// <summary>
/// Data component attached to furniture objects.
/// </summary>
public class FurnitureData : MonoBehaviour
{
    public string itemId;
}
