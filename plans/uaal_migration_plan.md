# Unity as a Library (UAAL) Migration Plan

## Overview
This document outlines the technical approach for migrating the AR Interior Engine from ARCore + SceneView to Unity AR Foundation + Universal Render Pipeline (URP) as an embedded library.

---

## 1. Architecture Overview

### Hybrid Android + Unity Architecture
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        Android App (Kotlin + Compose)                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────┐    ┌──────────────────┐    ┌─────────────────────────────┐ │
│  │   UI Layer      │    │   JNI Bridge     │    │      Unity AR Module        │ │
│  │ (Jetpack        │◄──►│   (Kotlin ↔ C#) │◄──►│   (AR Foundation + URP)     │ │
│  │  Compose)       │    │                  │    │                             │ │
│  │                 │    │  • UnityPlayer   │    │  • Plane Detection          │ │
│  │  • PromptBar    │    │  • AR Commands   │    │  • Anchoring                │ │
│  │  • ImagePicker  │    │  • Callbacks     │    │  • Hit Testing              │ │
│  │  • CandidateCard│    │                  │    │  • Object Placement          │ │
│  │  • WallColorPkr │    │                  │    │  • Scene Manipulation       │ │
│  │                 │    │                  │    │  • 3D Rendering (URP)        │ │
│  └─────────────────┘    └──────────────────┘    └─────────────────────────────┘ │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                      Android Services (Preserved)                          │ │
│  │  • ML Kit (background removal)  • SpeechRecognizer (voice capture)         │ │
│  │  • AWS API Client (Retrofit)    • Design Intent Processing                  │ │
│  │  • Data Models (Kotlin)         • Thermal Monitoring                       │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTPS (REST)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            AWS Cloud Backend                                    │
│  • Amazon API Gateway  • AWS Lambda  • Amazon Bedrock  • Amazon S3/CloudFront  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Mapping

### Current → Unity Mapping

| Current Component (ARCore/SceneView) | New Component (Unity) | Notes |
|---------------------------------------|----------------------|-------|
| `ARSceneView` | Unity AR Foundation `ARSession` | Managed in Unity |
| `PlaneRenderer` | `ARPlaneManager` | Detects horizontal/vertical planes |
| `AnchorNode` | `ARAnchor` | Unity uses native ARCore anchors |
| `ModelNode` | `GameObject` (URP) | 3D furniture with custom shaders |
| `LightEstimate` | `ARLightEstimation` | Ambient lighting from ARCore |
| `Filament materials` | URP Lit/Unlit shaders | For wall colors, furniture |
| SceneView interaction | `ARRaycastManager` | Hit-testing for placement |

---

## 3. Unity Project Structure

### Required Unity Packages
- **AR Foundation** (`com.unity.xr.arfoundation`) - Cross-platform AR abstraction
- **ARCore XR Plugin** (`com.unity.xr.arcore`) - Android ARCore provider
- **Universal RP** (`com.unity.render-pipelines.universal`) - URP rendering
- **Android Logcat** (optional) - Debug logging

### Unity Project Directory Structure
```
UnityProject/
├── Assets/
│   ├── Scripts/
│   │   ├── AR/
│   │   │   ├── ARSceneManager.cs          # Main AR lifecycle
│   │   │   ├── PlaneDetectionHandler.cs   # Plane detection events
│   │   │   ├── AnchorManager.cs           # Object anchoring
│   │   │   ├── HitTestManager.cs          # Touch-to-place
│   │   │   └── LightEstimationHandler.cs  # Lighting
│   │   ├── Rendering/
│   │   │   ├── FurnitureRenderer.cs      # Load/place furniture GLB
│   │   │   ├── WallMaterialRenderer.cs   # Wall color application
│   │   │   └── LightingManager.cs         # Shadows, ambient
│   │   ├── SceneManipulation/
│   │   │   ├── SceneMutator.cs           # Voice command execution
│   │   │   ├── AutoSnapEngine.cs          # Smart placement
│   │   │   └── ObjectSelector.cs          # Tap to select
│   │   └── Bridge/
│   │       ├── UnityToAndroidBridge.cs   # Send events to Android
│   │       └── AndroidToUnityBridge.cs    # Receive commands from Android
│   ├── Prefabs/
│   │   ├── FurniturePlaceholder.prefab    # Loading placeholder
│   │   └── SelectionIndicator.prefab      # Tap selection visual
│   ├── Materials/
│   │   ├── FurnitureMat.mat               # URP furniture material
│   │   ├── TransparentWallMat.mat        # Wall preview material
│   │   └── EmissiveLight.mat             # Sconce/lamp material
│   └── Models/
│       └── (furniture GLB files)
├── Packages/
├── ProjectSettings/
└── Plugins/
    └── Android/
        └── (Unity AAR export config)
```

---

## 4. JNI Bridge Protocol

### Communication Flow
```
┌─────────────────┐     JNI Calls      ┌─────────────────┐
│    Android     │ ◄────────────────► │     Unity       │
│   (Kotlin)     │                    │     (C#)        │
├─────────────────┤                    ├─────────────────┤
│ ARViewModel     │ ── InitAR() ─────► │ ARSceneManager  │
│                 │ ── PlaceItem() ──► │ FurnitureRender │
│                 │ ── MoveItem() ───► │ SceneMutator    │
│                 │ ── RecolorWall() ►│ WallRenderer    │
│                 │ ── RemoveItem() ► │ SceneMutator    │
│                 │ ◄─ OnFloorDetect─ │ PlaneDetection  │
│                 │ ◄─ OnWallDetect─ │ PlaneDetection   │
│                 │ ◄─ OnObjectTap───│ ObjectSelector  │
│                 │ ◄─ OnPlaneCount─ │ PlaneDetection  │
└─────────────────┘                    └─────────────────┘
```

### Kotlin → Unity Commands (Interface)
```kotlin
interface UnityARBridge {
    fun initializeAR()
    fun pauseAR()
    fun resumeAR()
    fun placeFurniture(itemId: String, position: Vector3, rotation: Float)
    fun moveFurniture(itemId: String, newPosition: Vector3)
    fun rotateFurniture(itemId: String, degrees: Float)
    fun scaleFurniture(itemId: String, scaleFactor: Float)
    fun removeFurniture(itemId: String)
    fun recolorWall(colorHex: String)
    fun clearScene()
    fun loadCandidates(candidateJson: String)
}
```

### Unity → Kotlin Callbacks (via UnityPlayer.UnitySendMessage)
```kotlin
// Registered in Unity as: "AndroidBridge", "OnFloorDetected", ""
class UnityCallbacks {
    @JvmStatic
    fun onFloorDetected() { /* notify ARViewModel */ }
    
    @JvmStatic
    fun onWallDetected() { /* notify ARViewModel */ }
    
    @JvmStatic
    fun onPlaneCountChanged(count: Int) { /* update UI */ }
    
    @JvmStatic
    fun onObjectTapped(itemId: String) { /* enter editing mode */ }
    
    @JvmStatic
    fun onTrackingStateChanged(state: String) { /* TRACKING/NOT_TRACKING */ }
}
```

---

## 5. Implementation Steps

### Step 1: Unity Project Setup
1. Create new Unity project with URP template
2. Install AR Foundation + ARCore XR Plugin packages
3. Configure URP for mobile (disable HDR, reduce quality settings)
4. Create base AR scene with ARSession, ARCamera, ARSessionOrigin
5. Build as AAR for Android embedding

### Step 2: Android Project Configuration
1. Remove SceneView/ARCore direct dependencies (keep ARCore for Unity)
2. Add Unity AAR dependency in `app/build.gradle.kts`
3. Configure ProGuard for Unity
4. Set up UnityPlayerActivity integration

### Step 3: JNI Bridge Implementation
1. Create Kotlin interface for Unity commands
2. Implement UnityPlayerView Composable
3. Register callback methods in Unity
4. Test bidirectional communication

### Step 4: AR Feature Migration
1. Plane detection → ARPlaneManager events
2. Anchoring → ARAnchor manipulation
3. Model loading → Addressables or Resources.Load
4. Light estimation → ARLightEstimation data

### Step 5: Scene Manipulation Migration
1. Port SceneMutator logic to C#
2. Port AutoSnapEngine logic to C#
3. Implement gesture handling (tap, drag, pinch)

### Step 6: UI Integration
1. Replace ARSceneComposable with UnityPlayerView
2. Keep all other Compose UI (PromptBar, CandidateCarousel, etc.)
3. Connect UI events to Unity via JNI bridge

---

## 6. Data Model Compatibility

### DesignCandidate JSON (shared between Kotlin and Unity)
```json
{
  "candidateId": "cand_001",
  "isBestTake": true,
  "score": 87,
  "furniture": [
    {
      "itemId": "sofa_modern_01",
      "positionX": 0.0,
      "positionY": 0.0,
      "positionZ": -1.5,
      "rotationY": 180.0,
      "scale": 1.0
    }
  ],
  "wallElements": [
    {
      "itemId": "art_abstract_01",
      "positionX": 0.0,
      "positionY": 1.5,
      "positionZ": -3.0,
      "scale": 0.8
    }
  ],
  "wallDesign": {
    "primaryColor": "#F5F5DC"
  }
}
```

---

## 7. Build Pipeline

### Gradle Changes Required
```kotlin
// app/build.gradle.kts
dependencies {
    // Remove old AR dependencies
    // implementation("io.github.sceneview:arsceneview:2.1.0")
    
    // Add Unity AAR
    implementation(files("libs/unity-classes.aar"))
}
```

### Unity Build Steps
1. Switch platform to Android
2. Configure Player Settings for AAR export
3. Strip engine code (reduce size)
4. Build `classes.jar` + `libs/` → rename to `.aar`
5. Copy AAR to Android project `app/libs/`

---

## 8. Error Handling & Fallbacks

| Scenario | Handling |
|----------|----------|
| ARCore not available | Show error, disable AR features, allow image-based design |
| Unity AR session fails | Fallback to non-AR 3D view or 2D image view |
| Model loading fails | Show placeholder, log error, try next candidate |
| Thermal throttling | Unity provides thermal callbacks; pause AR, show warning |
| Permission denied | Camera permission required for AR; show rationale dialog |

---

## 9. Performance Considerations

| Metric | Target | Notes |
|--------|--------|-------|
| Frame rate | 60 FPS | Unity URP optimized for mobile |
| Memory | < 512MB | Compressed textures, model LOD |
| AR initialization | < 3 seconds | Preload Unity in background |
| Touch latency | < 50ms | Direct Unity input handling |

---

## 10. Testing Strategy

### Unit Tests (Kotlin)
- ARViewModel state transitions
- SceneMutator command application
- JNI bridge serialization

### Integration Tests
- Kotlin ↔ Unity communication
- AR session lifecycle (permissions, pause/resume)
- Candidate loading and placement

### Manual Tests
- Plane detection on various surfaces
- Furniture placement accuracy
- Voice command responsiveness
- Thermal throttling behavior

---

## 11. Migration Checklist

- [ ] Unity project created with AR Foundation + URP
- [ ] ARSession, ARCamera, ARPlaneManager configured
- [ ] Unity exports as AAR library
- [ ] Android project references Unity AAR
- [ ] UnityPlayerView Composable implemented
- [ ] Kotlin → Unity command bridge working
- [ ] Unity → Kotlin callback bridge working
- [ ] Plane detection events flowing to ARViewModel
- [ ] Furniture placement working in Unity
- [ ] Scene manipulation (move/rotate/scale) working
- [ ] Auto-snap logic migrated to C#
- [ ] Compose UI integrated with Unity AR view
- [ ] Thermal monitoring continues to work
- [ ] Build pipeline produces working APK

---

## 12. References

- [Unity as a Library - Android](https://docs.unity3d.com/Manual/UnityasaLibrary-Android.html)
- [AR Foundation Documentation](https://docs.unity3d.com/Packages/com.unity.xr.arfoundation@latest)
- [ARCore XR Plugin](https://docs.unity3d.com/Packages/com.unity.xr.arcore@latest)
- [URP for Mobile](https://docs.unity3d.com/Packages/com.unity.render-pipelines.universal@latest)
