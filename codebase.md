# AR Interior Engine — Codebase Overview

## Project Summary

An **AI-powered mobile AR application** that designs entire rooms — walls, furniture, lighting, and art — in real-time augmented reality. Users describe their dream room via text/voice, or upload a screenshot of furniture they like, and the system automatically generates complete interior designs anchored to their physical space.

**Tech Stack:** Kotlin · ARCore · SceneView (Filament) · Jetpack Compose · AWS (Lambda + API Gateway + Bedrock + S3 + DynamoDB + Rekognition)

---

## Project Structure

```
AWS Proto/
├── app/                                    ← Android Application
│   ├── build.gradle.kts                    ← Dependencies & build configuration
│   └── src/main/
│       ├── AndroidManifest.xml             ← Permissions, ARCore metadata
│       └── java/com/arinterior/engine/
│           ├── MainActivity.kt             ← Entry point, theme, thermal monitoring
│           ├── ui/
│           │   ├── ARInteriorApp.kt        ← Root composable, state wiring
│           │   └── components/
│           │       ├── ARSceneComposable.kt ← ARCore + SceneView integration
│           │       ├── PromptBar.kt        ← Bottom floating input bar
│           │       ├── CandidateCarousel.kt← Design option cards
│           │       ├── OverlayComponents.kt← Scanning, status, loading, onboarding
│           │       └── WallColorPicker.kt  ← Color selection dialog
│           ├── viewmodel/
│           │   ├── ARViewModel.kt          ← App state machine + scan management
│           │   └── DesignViewModel.kt      ← AI pipeline + candidate generation
│           ├── data/
│           │   ├── model/
│           │   │   ├── RoomGeometry.kt     ← Room data + AppState enum
│           │   │   ├── DesignIntent.kt     ← Bedrock response model
│           │   │   ├── DesignCandidate.kt  ← Scored design option + placed items
│           │   │   ├── CatalogItem.kt      ← DynamoDB furniture item
│           │   │   └── VoiceCommand.kt     ← Parsed voice action
│           │   └── api/
│           │       ├── AwsApiService.kt    ← Retrofit REST interface
│           │       └── ApiClient.kt        ← Singleton Retrofit client
│           └── ar/
│               ├── AutoSnapEngine.kt       ← Smart placement rules
│               ├── SceneMutator.kt         ← Voice command → scene changes
│               ├── VoiceCommandManager.kt  ← Android SpeechRecognizer wrapper
│               └── BackgroundRemover.kt    ← ML Kit subject segmentation
├── lambda/                                 ← AWS SAM Backend
│   ├── template.yaml                       ← CloudFormation (API GW, Lambda, DDB, S3)
│   ├── samconfig.toml                      ← SAM deployment config
│   ├── seed_catalog.py                     ← Script to populate DynamoDB
│   ├── intent_extractor/handler.py         ← Bedrock Claude: prompt → DesignIntent JSON
│   ├── voice_command_parser/handler.py     ← Bedrock Claude: voice → VoiceCommand JSON
│   └── object_labeler/handler.py           ← Rekognition: image → catalog match
├── requirements.md                         ← Functional + non-functional requirements
├── design.md                               ← Architecture, pipelines, UI design
└── build.gradle.kts                        ← Root Gradle config
```

---

## Application State Machine

```
LAUNCHING → SCANNING → READY → PROCESSING → DISPLAYING → EDITING
    │            │          │         │             │           │
    │ Permissions │ Floor    │ Prompt  │ Bedrock     │ Tap obj   │
    │ granted    │ detected │ / image │ completes   │           │
    └────────────┘          │ submit  └─────────────┘           │
                            │                                    │
                            └──── User taps "Clear" ────────────┘
```

Managed by `ARViewModel`. Timeout auto-advances from SCANNING → READY after 20 seconds.

---

## Layer-by-Layer Breakdown

### 1. Entry Point — `MainActivity.kt` (101 lines)

| Responsibility | Implementation |
|---|---|
| Theme | Dark color scheme: `#00D4FF` primary, `#FF9F43` secondary, `#1A1A2E` surface |
| ARCore check | `ArCoreApk.checkAvailabilityAsync()` — shows toast if unsupported |
| Thermal monitoring | `PowerManager.addThermalStatusListener()` (API 29+) — warns on overheat |
| Compose root | `ARInteriorApp()` composable |

---

### 2. UI Layer — Jetpack Compose

#### `ARInteriorApp.kt` (183 lines) — Root Composable

Wires together all UI components in a `Box` overlay:

```
┌─────────────────────────────────────────────────┐
│  StatusBar (top)                                │
│  ┌───────────────────────────────────────────┐  │
│  │                                           │  │
│  │         ARSceneComposable                 │  │
│  │         (full-screen camera + 3D)         │  │
│  │                                           │  │
│  │    ScanningOverlay (during SCANNING)      │  │
│  │    LoadingOverlay  (during PROCESSING)    │  │
│  │    VoiceFeedbackChip (after voice cmd)    │  │
│  │                                           │  │
│  │    CandidateCarousel (when candidates)    │  │
│  │                                           │  │
│  └───────────────────────────────────────────┘  │
│  PromptBar (bottom — text/camera/mic)           │
└─────────────────────────────────────────────────┘
```

**Key wirings:**
- `onPromptSubmit` → `DesignViewModel.extractIntent()` → `generateCandidates()` → `ARViewModel.onCandidatesReady()`
- `onFloorDetected` → `ARViewModel.onFloorDetected()` → starts 3.5s stabilization delay → READY
- `onVoiceInput` → `DesignViewModel.parseVoiceCommand()` → shows feedback chip

#### `ARSceneComposable.kt` (166 lines) — AR Camera + 3D Rendering

- Wraps `ARSceneView` via `AndroidView` (Compose ↔ View bridge)
- `onSessionUpdated` callback: checks planes every 30 frames, logs tracking state
- `updateScene()`: places furniture and wall elements as `ModelNode` children of `AnchorNode`
- Post-detection stabilization: waits 3s then disables plane finding + enables light estimation

#### `PromptBar.kt` (128 lines) — Floating Input Bar

Glassmorphic dark bar with 3 inputs:
- 📷 Camera button → triggers Pipeline B (image upload)
- Text field → "Describe your dream room..." placeholder
- 🎤/➤ Mic button (when empty) or Send button (when text entered)

#### `CandidateCarousel.kt` (135 lines) — Design Cards

- `LazyRow` of `CandidateCard`s
- Best Take: gold border `#D4AF37` + ★ badge
- Selected: cyan border `#00D4FF`
- Shows composite score

#### `OverlayComponents.kt` (198 lines) — Status Overlays

| Component | Purpose |
|---|---|
| `ScanningOverlay` | Pulsing "Scanning…" text + `CircularProgressIndicator` + plane count chip |
| `StatusBar` | Top semi-transparent bar showing current state message |
| `LoadingOverlay` | "Processing…" modal during AI pipeline |
| `VoiceFeedbackChip` | Cyan chip showing "move sofa ✓" feedback |
| `OnboardingDialog` | First-launch instruction dialog |

#### `WallColorPicker.kt` (91 lines)

Grid dialog with 16 preset colors (Warm White, Sage Green, Terracotta, Navy, etc.) rendered as clickable circles.

---

### 3. ViewModel Layer

#### `ARViewModel.kt` (172 lines) — State Machine Controller

**State flows:**
- `appState: StateFlow<AppState>` — current state (LAUNCHING/SCANNING/READY/etc.)
- `floorDetected / wallsDetected` — plane detection flags
- `planeCount` — live count for scanning overlay
- `candidates / selectedCandidate` — design results
- `statusMessage` — human-readable status text
- `isProcessing / errorMessage` — loading and error states

**Key logic:**
- 20-second scan timeout (`SCAN_TIMEOUT_MS`) with auto-advance to READY
- `onFloorDetected()`: cancels timeout, starts 3.5s delay before READY
- `onRetryScan()`: resets all flags and re-enters SCANNING

#### `DesignViewModel.kt` (186 lines) — AI Pipeline

**Pipeline A — Prompt → Design:**
1. `extractIntent(prompt)` → calls Bedrock via Retrofit → returns `DesignIntent`
2. `generateCandidates(intent, roomGeometry)` → creates 3 variations:
   - "Bold & Centered" (0° rotation offset)
   - "Asymmetric Flow" (30° offset)
   - "Cozy Corner" (-15° offset)
3. `scoreCandidates()` → scores on 6 dimensions (color harmony 20%, spatial fit 25%, wall cohesion 15%, lighting 15%, style 15%, constraints 10%)

**Voice parsing:**
- `parseVoiceCommand(text)` → calls Bedrock → returns `VoiceCommand`

---

### 4. Data Models

#### `RoomGeometry.kt` (47 lines)

```kotlin
data class RoomGeometry(
    floorPlanes: List<DetectedPlaneData>,
    wallPlanes: List<DetectedPlaneData>,
    estimatedDimensions: RoomDimensions,    // width, length, height
    ambientLight: AmbientLight              // intensity, colorTemperature
)

enum class AppState { LAUNCHING, SCANNING, READY, PROCESSING, DISPLAYING, EDITING }
```

#### `DesignIntent.kt` (57 lines) — Bedrock Response

```kotlin
data class DesignIntent(
    style: String,              // "minimalist", "modern", "cozy", etc.
    palette: Palette?,          // primary, secondary, accent hex colors
    objects: List<ObjectRequest>?,     // furniture items
    wallTreatment: WallTreatment?,    // color, texture, accent wall
    wallDecor: List<WallDecorRequest>?, // paintings, mirrors, clocks
    accentLighting: List<LightingRequest>?, // sconces, LEDs, spotlights
    constraints: DesignConstraints?    // budget, currency
)
```

#### `DesignCandidate.kt` (58 lines)

```kotlin
data class DesignCandidate(
    id: String, name: String,
    furniture: List<PlacedItem>,      // floor items with position/rotation/scale
    wallDesign: WallDesign,           // primaryColor, accentWall, texture
    wallElements: List<PlacedWallItem>, // wall art/lights with height/wall ID
    scores: CandidateScores,          // 6-dimension scoring (composite 0-100)
    isBestTake: Boolean
)
```

#### `CatalogItem.kt` (29 lines)

DynamoDB item: `id, name, category, placement (FLOOR/WALL), styleTags, dimensions, modelS3Key, priceInr, colorHex, emissive`

#### `VoiceCommand.kt` (24 lines)

```kotlin
data class VoiceCommand(
    target: String,                   // "sofa", "wall", "lamp"
    action: VoiceAction,              // MOVE, RECOLOR, REMOVE, RESIZE, ROTATE, UNDO, RESET
    direction: Direction?,            // LEFT, RIGHT, FORWARD, BACKWARD, UP, DOWN
    amount: Float?, color: String?, scaleFactor: Float?, rotationDegrees: Float?
)
```

---

### 5. API Layer

#### `ApiClient.kt` (40 lines)

Singleton Retrofit client with:
- 15s connect / 30s read+write timeouts
- Debug logging interceptor
- Base URL from `BuildConfig.API_BASE_URL`

#### `AwsApiService.kt` (60 lines)

| Endpoint | Request | Response |
|---|---|---|
| `POST /intent` | `{ prompt: String }` | `{ design_intent: Map, raw_prompt: String }` |
| `POST /voice-cmd` | `{ text: String }` | `{ command: Map, raw_text: String }` |
| `POST /label` | `{ image: String (base64) }` | `{ labels: [], matched_label, catalog_match, model_url }` |

---

### 6. AR Utilities

#### `AutoSnapEngine.kt` (123 lines) — Smart Placement

| Object Type | Snap Behavior |
|---|---|
| Seating (sofa, chair) | Snap to nearest wall with 10cm gap |
| Tables | Snap to center of seating arrangement, 80cm forward |
| Rugs | Center under furniture grouping, 1cm above floor |
| Wall art | Eye level at 1.5m, centered on wall |
| Wall lights | Sconce height at 1.8m, flanking artwork by 60cm |

#### `SceneMutator.kt` (108 lines) — Voice → Scene Changes

Applies `VoiceCommand` to `DesignCandidate` immutably:
- **MOVE**: Translates item by `amount` meters in `direction`
- **RECOLOR**: Changes `wallDesign.primaryColor`
- **REMOVE**: Filters out matching items from furniture/wallElements
- **RESIZE**: Multiplies `scale` by `scaleFactor`
- **ROTATE**: Adds `rotationDegrees` to `rotationY`
- **RESET**: Clears all items, resets walls to white

Uses fuzzy matching: `"sofa"` matches `"sofa_minimal_01"`.

#### `VoiceCommandManager.kt` (98 lines) — Speech Recognition

Wraps Android `SpeechRecognizer` (on-device, free, unlimited). Exposes:
- `isListening: StateFlow<Boolean>`
- `lastResult: StateFlow<String?>`
- `startListening(onResult)` / `stopListening()` / `destroy()`

#### `BackgroundRemover.kt` (51 lines) — ML Kit Segmentation

Uses `SubjectSegmentation` (Google ML Kit, on-device) to isolate furniture from screenshots. Returns foreground bitmap with transparent background.

---

### 7. AWS Lambda Backend

**Infrastructure** (`template.yaml`, 148 lines):

```
API Gateway (ARInteriorAPI)
├── POST /intent  → IntentExtractorFunction
├── POST /voice-cmd → VoiceCommandParserFunction
└── POST /label   → ObjectLabelerFunction

DynamoDB: FurnitureCatalog (category HASH, id RANGE)
S3: ar-interior-models-{AccountId} (public read for GLB models)
```

#### `intent_extractor/handler.py` (100 lines)

- Calls **Bedrock Claude 3 Haiku** with system prompt defining JSON schema
- Extracts: style, palette, objects (3-5 furniture items), wall treatment, wall decor, accent lighting, budget constraints
- Returns structured `DesignIntent` JSON

#### `voice_command_parser/handler.py` (83 lines)

- Calls **Bedrock Claude 3 Haiku** with voice parsing rules
- Converts "move the sofa left" → `{ target: "sofa", action: "MOVE", direction: "LEFT", amount: 0.3 }`
- Includes confidence scoring

#### `object_labeler/handler.py` (127 lines)

- Calls **Amazon Rekognition** `DetectLabels` (max 15, min 70% confidence)
- Maps Rekognition labels to catalog categories (e.g., "Armchair" → "seating")
- Queries **DynamoDB** for matching catalog items
- Returns label, category, catalog match, and model URL

#### `seed_catalog.py` (script, not Lambda)

Populates DynamoDB `FurnitureCatalog` with initial furniture items.

---

### 8. Configuration

#### `build.gradle.kts` — Dependencies

| Category | Libraries |
|---|---|
| **AR/3D** | `io.github.sceneview:arsceneview:2.1.0` (bundles ARCore 1.42.0 + Filament 1.51.0) |
| **Compose** | BOM `2024.01.00`, Material 3, Material Icons Extended |
| **Networking** | Retrofit 2.9.0, OkHttp 4.12.0, Gson converter |
| **ML** | ML Kit Subject Segmentation `16.0.0-beta1` |
| **Image** | Coil Compose 2.5.0 |
| **Core** | Coroutines 1.7.3, LifecycleViewModel Compose 2.7.0 |

#### `AndroidManifest.xml`

- Permissions: `CAMERA`, `RECORD_AUDIO`, `INTERNET`
- `android.hardware.camera.ar` required feature
- `com.google.ar.core` = `required` (overrides SceneView's default "optional")
- Portrait-only, edge-to-edge

---

## Data Flow Diagrams

### Pipeline A: Prompt → AR Design

```
User types prompt
       │
       ▼
PromptBar.onPromptSubmit(prompt)
       │
       ▼
DesignViewModel.extractIntent(prompt)
       │ HTTPS POST /intent
       ▼
Lambda → Bedrock Claude Haiku → DesignIntent JSON
       │
       ▼
DesignViewModel.generateCandidates(intent, roomGeometry)
       │ Creates 3 variations with different layouts
       ▼
DesignViewModel.scoreCandidates()
       │ Scores on 6 dimensions, sorts by composite
       ▼
ARViewModel.onCandidatesReady(candidates)
       │ State → DISPLAYING
       ▼
ARSceneComposable.updateScene()
       │ Places ModelNodes on AnchorNodes
       ▼
CandidateCarousel shows 3 cards with scores
```

### Pipeline B: Screenshot → 3D Model

```
User uploads image
       │
       ▼
BackgroundRemover.removeBackground(bitmap)
       │ ML Kit Subject Segmentation (on-device)
       ▼
base64 encode → POST /label
       │
       ▼
Lambda → Rekognition DetectLabels
       │ "Armchair" → category: "seating"
       ▼
DynamoDB query → catalog match
       │
       ▼
Return model_url (S3/CloudFront GLB)
       │
       ▼
ARSceneComposable → ModelNode placed on floor anchor
```

---

## File Statistics

| Category | Files | Total Lines |
|---|---|---|
| Android — UI | 6 | 913 |
| Android — ViewModels | 2 | 358 |
| Android — Data Models | 5 | 215 |
| Android — API | 2 | 100 |
| Android — AR Utilities | 4 | 380 |
| Android — Config | 3 | 213 |
| Lambda — Handlers | 3 | 310 |
| Lambda — Config | 2 | ~160 |
| Documentation | 2 | 516 |
| **Total** | **29** | **~3,165** |
