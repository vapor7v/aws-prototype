# Prompt-to-Reality AR Interior Engine — Design Document

## 1. System Architecture

```
┌──────────────────── Android App (Kotlin + ARCore + SceneView) ────────────────────────────┐
│                                                                                           │
│   ┌─────────────────┐    ┌──────────────────┐    ┌────────────────────────────────────┐   │
│   │    UI Layer      │    │    AR Layer       │    │       3D Rendering Layer           │   │
│   │ (Jetpack Compose)│    │    (ARCore)       │    │   (SceneView + Google Filament)    │   │
│   │                  │    │                   │    │                                    │   │
│   │  • PromptBar     │    │  • PlaneDetection │    │  • FurnitureRenderer               │   │
│   │  • ImagePicker   │    │  • Anchoring      │    │  • WallRenderer (Filament material)│   │
│   │  • CandidateCard │    │  • HitTesting     │    │  • AccentLightRenderer             │   │
│   │  • LoadingStates │    │  • Tracking        │    │  • WallArtPlacer                   │   │
│   │  • WallColorPkr  │    │  • SceneSemantics │    │  • LightEstimation/Shadows         │   │
│   └───────┬─────────┘    └────────┬──────────┘    └──────────────┬─────────────────────┘   │
│           │                       │                               │                        │
│   ┌───────┴───────────────────────┴───────────────────────────────┴─────────────────────┐  │
│   │                          App Controller (State Machine)                              │  │
│   │   States: SCANNING → READY → PROCESSING → DISPLAYING → EDITING                     │  │
│   └───────────────────────────────┬─────────────────────────────────────────────────────┘  │
│                                   │                                                       │
│   ┌───────────────────────────────┴─────────────────────────────────────────────────────┐  │
│   │                              AI Pipeline Layer                                       │  │
│   │                                                                                      │  │
│   │   Pipeline A (Prompt → Design)          Pipeline B (Screenshot → 3D)                 │  │
│   │   ┌──────────────────────────┐          ┌─────────────────────────────┐               │  │
│   │   │ 1. Intent Extraction     │          │ 1. Background Removal       │               │  │
│   │   │    (AWS Bedrock)         │          │    (ML Kit — on-device)     │               │  │
│   │   │ 2. Candidate Generation  │          │ 2. Object Labeling          │               │  │
│   │   │ 3. Scoring & Best Take   │          │    (AWS Rekognition)        │               │  │
│   │   │ 4. Layout Placement      │          │ 3. 3D Model Fetch (S3)     │               │  │
│   │   └──────────────────────────┘          │ 4. Model Optimization       │               │  │
│   │                                         └─────────────────────────────┘               │  │
│   └──────────────────────────────────────────────────────────────────────────────────────┘  │
│                                   │                                                       │
│   ┌───────────────────────────────┴─────────────────────────────────────────────────────┐  │
│   │                           Voice Command Layer                                        │  │
│   │     SpeechRecognizer (on-device) → Bedrock parse → Scene mutation → Feedback        │  │
│   └──────────────────────────────────────────────────────────────────────────────────────┘  │
│                                   │                                                       │
│   ┌───────────────────────────────┴─────────────────────────────────────────────────────┐  │
│   │                           On-Device Services (Free)                                  │  │
│   │     • ML Kit (background removal)  • SpeechRecognizer (voice-to-text)               │  │
│   │     • ARCore (planes, anchors)     • Filament (wall materials)                       │  │
│   └──────────────────────────────────────────────────────────────────────────────────────┘  │
│                                   │                                                       │
└───────────────────────────────────┼───────────────────────────────────────────────────────┘
                                    │ HTTPS (REST)
                    ┌───────────────┴───────────────────┐
                    │   AWS Cloud Backend                │
                    │                                    │
                    │  ┌──────────────────────────────┐  │
                    │  │     Amazon API Gateway        │  │
                    │  │  /intent  /label  /voice-cmd  │  │
                    │  └──────────┬───────────────────┘  │
                    │             │                       │
                    │  ┌──────────┴───────────────────┐  │
                    │  │       AWS Lambda              │  │
                    │  │  • IntentExtractor (Bedrock)  │  │
                    │  │  • VoiceCmdParser (Bedrock)   │  │
                    │  │  • ObjectLabeler (Rekognition)│  │
                    │  └──────────┬───────────────────┘  │
                    │             │                       │
                    │  ┌──────────┴───────────────────┐  │
                    │  │  Amazon Bedrock (Claude Haiku)│  │
                    │  │  Amazon Rekognition (Labels)  │  │
                    │  │  Amazon S3 (GLB models)       │  │
                    │  │  Amazon CloudFront (CDN)      │  │
                    │  │  Amazon DynamoDB (Catalog)    │  │
                    │  └──────────────────────────────┘  │
                    └────────────────────────────────────┘
```

---

## 2. Application State Machine

```
                    ┌──────────────┐
                    │   LAUNCHING   │
                    │  (Splash +    │
                    │  Permissions)  │
                    └──────┬───────┘
                           │ Camera + Mic granted
                           ▼
                    ┌──────────────┐
                    │   SCANNING    │
                    │  (ARCore      │
                    │   detecting   │
                    │   planes)     │
                    └──────┬───────┘
                           │ Sufficient planes detected
                           ▼
                    ┌──────────────┐
                    │    READY      │ ◄── User taps "Clear"
                    │  (Prompt bar  │
                    │   active)     │
                    └──────┬───────┘
                           │ User submits prompt or uploads image
                           ▼
                    ┌──────────────┐
                    │  PROCESSING   │
                    │  (AI pipeline  │
                    │   running)    │
                    └──────┬───────┘
                           │ Candidates generated / 3D model ready
                           ▼
                    ┌──────────────┐
                    │  DISPLAYING   │
                    │  (AR overlay   │
                    │   active)     │
                    └──────┬───────┘
                           │ User taps placed object
                           ▼
                    ┌──────────────┐
                    │   EDITING     │
                    │  (Move/rotate  │
                    │   objects)    │
                    └──────────────┘
```

---

## 3. Technology Decision Rationale

The architecture uses a three-tier approach: **on-device** for real-time AR (latency <16ms), **AWS** for backend intelligence and storage, and **best-free alternatives** where AWS has no advantage or is cost-prohibitive.

| Layer | On-Device (Free, Real-Time) | AWS Cloud (Async) | Why Not the Other |
|---|---|---|---|
| **AR Core** | ARCore, SceneView, Filament | — | No AWS AR SDK; cloud latency breaks 30 FPS |
| **Background Removal** | Google ML Kit | — | Better than Rekognition for segmentation; zero latency; unlimited |
| **Voice Capture** | Android SpeechRecognizer | — | Transcribe only 60 min/month free — too limited |
| **NLP / Intent** | — | Bedrock (Claude Haiku) | No on-device LLM can do structured JSON extraction |
| **Object Labeling** | — | Rekognition | No good free on-device furniture classifier |
| **Model Storage** | Bundled fallbacks | S3 + CloudFront | Need CDN for catalog; fallbacks for offline |
| **Catalog DB** | Fallback JSON | DynamoDB | Queryable by style/category; 25 GB free |
| **Compute** | — | Lambda + API Gateway | Serverless glue; 1M req/month free |

> **Rule: No AWS call ever touches the real-time rendering loop.** Cloud is only used during the "thinking" phase before objects appear. Once models are cached, everything runs 100% on-device.

---

## 4. Pipeline A — Prompt-Based Design

**User types:** `"Minimalist room with white walls and wooden furniture under ₹30,000"`

### Step 1: Intent Extraction (AWS Bedrock)
- Prompt sent to API Gateway → Lambda → Bedrock Claude Haiku
- Returns structured `DesignIntent` JSON: style, color palette, furniture list, wall treatment, wall decor, accent lighting, budget constraints
- Latency: ~2-3 seconds

### Step 2: Candidate Generation (On-Device)
- Takes `DesignIntent` + `RoomGeometry` (from ARCore plane scan) → generates 3 design candidates
- Each candidate varies furniture models, color variations, and placement positions
- Floor objects placed within detected horizontal planes, wall objects on vertical planes
- Paintings at 1.4-1.6m height, sconces at 1.7-1.9m, minimum 0.5m spacing between objects

### Step 3: Scoring & Best Take (On-Device)

Each candidate is scored on 6 dimensions (0–100), weighted to produce a composite:

| Dimension | Weight | What It Measures |
|---|---|---|
| Color Harmony | 20% | Wall + furniture + decor color distances in HSL space |
| Spatial Fit | 25% | Furniture footprint vs. room area; penalize crowding (>60%) |
| Wall-Furniture Cohesion | 15% | Wall color vs. furniture contrast ratio |
| Lighting Match | 15% | Material reflectance + accent warmth vs. AR ambient light |
| Style Consistency | 15% | All items share style tags |
| Constraint Satisfaction | 10% | Budget compliance |

Highest composite score = **Best Take** (shown with golden border).

### Step 4: AR Placement (On-Device)
- Floor furniture: `Anchor` on detected floor `Plane` → load GLB via SceneView `ModelNode` → apply `LightEstimate` shadows
- Wall painting: Custom Filament `.filamat` material with `blending: transparent` at ~60% opacity on vertical planes
- Wall art & sconces: `Anchor` on vertical `Plane` at specified height; sconces use Filament `emissive` material
- Accent wall: distinct color material on identified wall plane

---

## 5. Pipeline B — Screenshot to 3D

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  User picks   │     │  Background   │     │  Identify     │     │  Optimize     │     │  Place in    │
│  image from   │────▶│  removal via  │────▶│  or Generate  │────▶│  for mobile   │────▶│  AR scene    │
│  gallery      │     │  ML Kit       │     │  3D model     │     │  (reduce poly)│     │              │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

1. **Image Upload** — Native gallery picker via Android Intent
2. **Background Removal** — On-device ML Kit Subject Segmentation (free, no network)
3. **Object Identification** — Send isolated image to Rekognition (via Lambda) for labeling → match against DynamoDB catalog → fetch GLB from S3/CloudFront. If no match, download closest model.
4. **Model Optimization** — Reduce to <50K polygons, compress textures to 1024x1024, ensure Y-up orientation
5. **AR Placement** — Raycast to floor `Plane`, place with `Anchor`, apply `LightEstimate`

---

## 6. Voice Command Pipeline

1. **Capture** — Android `SpeechRecognizer` (on-device, free, unlimited)
2. **Parse** — Send transcribed text to Bedrock via API Gateway → returns structured command (target, action, parameters)
3. **Mutate** — Apply to AR scene: move, recolor wall, remove, resize, undo
4. **Feedback** — Brief glow highlight on affected node (500ms)

**Supported commands:** "move the sofa left", "change wall color to blue", "remove the lamp", "make it bigger", "undo that"

---

## 7. Auto-Snap Placement Rules

| Object Type | Snap Behavior |
|---|---|
| Sofas, large furniture | Snap to nearest wall with ~10cm gap |
| Coffee tables | Snap to center of seating arrangement |
| Rugs | Center under furniture grouping |
| Wall art / paintings | Center on wall at eye-level (1.4–1.6m) |
| Sconces / wall lights | Flank artwork or above sofa (1.7–1.9m) |
| Floor lamps | Snap to sofa corners / reading nook areas |

All snapping uses ARCore's detected plane geometry and runs entirely on-device.

---

## 8. UI Design

### PromptBar (Bottom Floating Bar)
```
┌───────────────────────────────────────────────────┐
│ ┌──┐                                        ┌──┐ │
│ │📷│  "Describe your dream room..."          │🎤│ │
│ └──┘                                        └──┘ │
└───────────────────────────────────────────────────┘
```
- 📷 triggers gallery picker (Pipeline B) | 🎤 triggers voice input | Text field for prompts (Pipeline A)

### Candidate Carousel (After Design Generated)
```
┌─────────┐  ┌─────────┐  ┌─────────┐
│★ Best   │  │ Clean & │  │ Warm &  │
│  Take   │  │ Minimal │  │ Cozy    │
│Score: 87│  │Score: 74│  │Score: 68│
└─────────┘  └─────────┘  └─────────┘
```
- Horizontal scrollable cards, tap to swap AR scene, Best Take has golden border + star badge

---

## 9. Error Handling & Edge Cases

| Scenario | Handling |
|---|---|
| No planes detected after 10s | Show "Move your phone slowly over a flat surface" hint |
| No vertical planes (walls) detected | Show "Point camera at a wall" hint; skip wall features |
| Bedrock / API Gateway timeout | Retry once, then show "AI unavailable, try again" Snackbar |
| Rekognition labeling fails | Fall back to catalog keyword search |
| S3 / CloudFront model download fails | Try bundled fallback models |
| ML Kit segmentation poor quality | Allow user to manually crop object |
| Model too large for mobile | Auto-decimate to <50K polygons |
| Room too small for furniture | Reduce set, warn "Some items won't fit" |
| Network offline | Disable AI features, use cached/bundled models |
| Voice command not understood | Show "Didn't catch that" chip with retry |
| Lambda cold start delay | Show subtle loading indicator |

---

## 10. Security Considerations

| Concern | Mitigation |
|---|---|
| AWS credentials in app | API Gateway with API key auth; never embed IAM credentials in APK |
| API key management | Store in `local.properties` (gitignored); inject via BuildConfig |
| Lambda / Bedrock access | IAM roles with least-privilege policies |
| User images sent to Rekognition | Privacy policy disclosure; images processed, not stored |
| 3D model downloads | CloudFront with signed URLs or OAI for S3 security |
| Camera & Microphone | Explicit permission requests with onboarding explanation |

---

## 11. Future Enhancements (Out of Scope for v1)

| Feature | Description |
|---|---|
| iOS port | ARKit + RealityKit rewrite |
| Multi-room persistence | Save/reload designs via DynamoDB |
| Social sharing | Export AR scene as video → S3 pre-signed URL |
| E-commerce linking | "Buy this sofa" with affiliate links |
| Collaborative design | Multiple users via AppSync/WebSocket |
| Ceiling & flooring | Crown molding, floor material preview |
| LiDAR integration | Device depth sensor for precise room mapping |
| AI-generated 3D | SageMaker endpoint for image-to-3D generation |
