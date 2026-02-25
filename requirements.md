# Requirements: AR Interior Engine

## Overview

An AI-powered mobile AR application where users either describe what they want (text/voice prompt) or upload a screenshot (from Instagram, Flipkart, Pinterest, etc.), and the system automatically understands the room, generates or retrieves 3D objects, selects the optimal layout, paints walls, places accent lighting and artwork, and overlays the complete interior design in real time in their actual physical space.

This is not just a furniture placement app. It designs the entire room - walls, lighting, art, and furniture - as one cohesive experience.

## Problem Statement

Current interior design solutions have three major gaps:

1. Catalog-first AR apps: Users browse, search, and manually place furniture. This is slow, unintuitive, and requires users to know exactly what they want.
2. AI room generators: Generate pretty images of designed rooms, but not real rooms - you can't walk around in them.
3. Inspiration platforms: Users see furniture they love on Instagram/Flipkart/Pinterest but cannot try it in their own space.

This product collapses all three problems into one solution: a prompt-first, vision-aware, spatial AI engine that outputs complete room reality instead of images.

## Target Platform

- Platform: Android (ARCore-supported devices)
- Language: Kotlin
- AR SDK: ARCore
- 3D Engine: SceneView (Filament-based, Kotlin successor to Sceneform)
- UI Framework: Jetpack Compose
- Backend: AWS (Lambda + API Gateway + S3 + DynamoDB + Bedrock)
- Minimum Android: API 24 (Android 7.0) with ARCore support

## Framework Selection

Kotlin + ARCore + SceneView was selected over Unity, React Native + ViroReact, and Flutter for the following reasons:

1. Native Android integration — Retrofit, SpeechRecognizer, Jetpack Compose, and all Android SDKs work natively without bridging or plugins
2. SceneView + Google Filament — High-quality PBR rendering engine with direct glTF/GLB model loading and custom Filament materials for wall overlays
3. ARCore direct access — Full access to plane detection, Scene Semantics API (ML-based wall classification), Depth API, light estimation, and anchoring
4. Modern UI — Jetpack Compose provides declarative, beautiful, responsive UI with full Material 3 design system
5. Lightweight — App size ~30-40 MB vs ~120 MB for Unity-based apps
6. AWS-native backend — Serverless Lambda functions + API Gateway for AI pipeline; S3 + CloudFront for 3D model delivery; DynamoDB for catalog
7. Standard Android tooling — Gradle, Android Studio, GitHub Actions CI/CD, JUnit testing — all industry-standard

## User Stories

### 1. Prompt-Based Room Design
As a user, I want to point my phone camera at my room and type "modern living room with a white sofa, warm lighting, and sage green walls" so that the app automatically designs the entire room - walls, furniture, and lighting - in AR.

Acceptance Criteria:
- 1.1: System detects horizontal planes (floors) within 5 seconds of camera activation
- 1.2: System detects vertical planes (walls) within 5 seconds of camera activation
- 1.3: Text input is accepted through floating prompt bar at bottom of screen
- 1.4: Structured intent is extracted from prompt including style, palette, objects, wall design, and lighting
- 1.5: Three design candidates are generated based on intent and room geometry
- 1.6: Candidates are scored on color harmony, spatial fit, lighting match, and wall-furniture cohesion
- 1.7: Best scoring candidate is automatically selected and displayed in AR
- 1.8: Wall colors are applied to detected vertical planes in AR
- 1.9: Furniture models are auto-snapped to optimal positions (against walls, centered, cornered) without manual placement
- 1.10: Accent lighting is placed on walls with emissive glow effects
- 1.11: No finger-based dragging required for initial placement
- 1.12: Total latency from prompt submission to AR display is under 8 seconds

### 2. Screenshot-to-3D Import
As a user, I want to screenshot a chair I liked on Instagram and upload it so that the app creates a 3D version and shows it in my actual room.

Acceptance Criteria:
- 2.1: Image upload is supported from device gallery via screenshot upload button
- 2.2: Furniture object is isolated from uploaded image (background removed)
- 2.3: System attempts to match object to existing 3D model in library
- 2.4: If no match found, system generates 3D model from image using AI service
- 2.5: Generated model is optimized (polygon reduction, texture compression)
- 2.6: Model is placed in AR scene on detected horizontal surface
- 2.7: Model is scaled realistically based on real-world dimensions
- 2.8: Total latency from upload to AR placement is under 30 seconds

### 3. Design Candidate Selection
As a user, I want to see 3 design options ranked by how well they fit my room so that I can pick the one I prefer.

Acceptance Criteria:
- 3.1: Three visually distinct design candidates are generated per prompt
- 3.2: Each candidate includes wall colors, furniture placement, and lighting
- 3.3: Candidates are displayed as horizontal carousel cards with scores
- 3.4: User can swipe between alternate candidates
- 3.5: Selected candidate is rendered in AR view
- 3.6: Switching between candidates updates AR view in real-time

### 4. Walk-Around Viewing
As a user, I want to walk around the designed room with my phone so that I can see furniture, wall colors, lighting, and artwork from all angles.

Acceptance Criteria:
- 4.1: All 3D furniture models remain anchored to floor surfaces during movement
- 4.2: All wall elements remain anchored to vertical surfaces during movement
- 4.3: AR frame rate maintains at least 30 FPS during walk-around
- 4.4: No visible drift or jitter in anchored elements
- 4.5: Virtual objects maintain correct scale and orientation from all viewing angles

### 5. Realistic Lighting
As a user, I want the virtual furniture to be lit like my real room so that it looks realistic and blends in naturally.

Acceptance Criteria:
- 5.1: Real-world light estimation is applied to virtual objects
- 5.2: Virtual object lighting matches real environment intensity
- 5.3: Virtual object lighting matches real environment color temperature
- 5.4: Lighting updates dynamically as user moves through space

### 6. Wall Design and Accent Lighting
As a user, I want to say "paint the walls terracotta with small accent lights" and see my walls change color with sconce lights placed automatically.

Acceptance Criteria:
- 6.1: Wall color intent is extracted from prompt
- 6.2: Accent lighting intent is extracted from prompt
- 6.3: Wall color is applied to all detected vertical planes
- 6.4: Accent lights are placed on walls at realistic heights
- 6.5: Wall decor suggestions are generated based on style intent
- 6.6: Wall color changes preview in real-time

### 7. Wall Art Placement
As a user, I want to upload a painting I saw online and have it placed on my wall in AR so I can see how it looks before buying.

Acceptance Criteria:
- 7.1: Screenshot upload is supported for wall art/decor
- 7.2: Wall art is isolated from uploaded image
- 7.3: Wall art is placed on detected vertical surface
- 7.4: Wall art is anchored at realistic height (eye level)
- 7.5: Wall art maintains correct scale and aspect ratio

### 8. Cohesive Design Suggestions
As a user, I want the app to suggest matching wall art and lighting based on the furniture style it picked, so everything feels cohesive.

Acceptance Criteria:
- 8.1: Wall decor suggestions are generated based on furniture style
- 8.2: Lighting suggestions are generated based on furniture style
- 8.3: Suggested elements match color palette of furniture
- 8.4: Suggested elements match design style (modern, traditional, etc.)

### 9. Auto-Snapping and Smart Placement
As a user, I want furniture and decor to automatically snap to logical positions in my room so that I don't need to manually drag and position every item.

Acceptance Criteria:
- 9.1: Sofas and large furniture auto-snap to detected walls with correct spacing (~10cm gap)
- 9.2: Coffee tables auto-snap to center of seating arrangement
- 9.3: Rugs auto-snap centered under furniture grouping
- 9.4: Wall art auto-snaps to center of wall at eye-level height (1.4-1.6m)
- 9.5: Sconces auto-snap flanking artwork or above sofa at mounting height (1.7-1.9m)
- 9.6: Objects respect minimum spacing rules (~0.5m between items)
- 9.7: Objects avoid overlapping with each other and with detected room boundaries
- 9.8: User can override auto-placement with manual drag if needed

### 10. Voice-Based Real-Time Editing
As a user, I want to speak commands like "move the sofa to the left" or "change the light color to warm yellow" and see changes happen instantly in AR, so that I can refine my design hands-free.

Acceptance Criteria:
- 10.1: Voice input is activated via microphone button or always-listening mode
- 10.2: On-device speech-to-text converts voice to text in real-time
- 10.3: AI parses voice command to identify target object and action
- 10.4: Movement commands are supported ("move sofa left", "push table closer to wall")
- 10.5: Color change commands are supported ("change wall to blue", "make lights warmer")
- 10.6: Property change commands are supported ("make the rug bigger", "rotate the painting")
- 10.7: Removal commands are supported ("remove the lamp", "take off the painting")
- 10.8: Changes are applied to AR scene within 1 second of command recognition
- 10.9: Visual feedback confirms which object was affected (brief highlight/glow)
- 10.10: Undo command is supported ("undo that", "put it back")

## Non-Functional Requirements

### Performance
- AR frame rate: Minimum 30 FPS during rendering
- Prompt-to-display latency: Under 8 seconds (intent extraction + candidate generation)
- Screenshot-to-3D latency: Under 30 seconds (image to 3D model to placed)
- App size (APK): Under 50 MB
- Minimum RAM: 3 GB

### Compatibility
- Supported 3D formats: glTF, GLB, OBJ
- Offline capability: Room scanning works offline; AI features require network

## External Service Dependencies

### Technology Decision Matrix

Each component uses the best option based on three criteria: latency requirements, cost, and capability. Real-time AR functions stay on-device for zero latency. AWS handles backend intelligence and storage. Best-free alternatives are used where AWS has no advantage.

| Function | Technology | Tier | Why This Choice |
|---|---|---|---|
| **AR tracking & planes** | ARCore | On-device (free) | Real-time (<16ms), no AWS AR SDK exists |
| **3D rendering & materials** | SceneView + Filament | On-device (free) | Real-time PBR rendering, wall painting overlays |
| **Object placement & snapping** | Custom Kotlin logic | On-device (free) | Real-time spatial math, cloud latency unacceptable |
| **Background removal** | Google ML Kit | On-device (free) | Better than Rekognition for segmentation, zero latency, no usage limits |
| **Voice capture (STT)** | Android SpeechRecognizer | On-device (free) | Transcribe only 60 min/month free — too limited |
| **Intent extraction (NLP)** | Amazon Bedrock (Claude Haiku) | AWS (credits) | No on-device LLM can do structured JSON extraction at this quality |
| **Voice command parsing** | Amazon Bedrock (Claude Haiku) | AWS (credits) | Same LLM call, parses "move sofa left" → structured command |
| **Object labeling** | Amazon Rekognition | AWS (12-mo free) | No good free on-device furniture classifier |
| **3D model storage** | Amazon S3 + CloudFront | AWS (Always Free) | Need cloud storage + CDN for catalog delivery |
| **Furniture catalog DB** | Amazon DynamoDB | AWS (Always Free) | Queryable by style, category, color; 25 GB free |
| **Backend compute** | AWS Lambda | AWS (Always Free) | Serverless glue for Bedrock + Rekognition calls |
| **API routing** | Amazon API Gateway | AWS (12-mo free) | Single REST entry point for app → cloud |

### AWS Free Tier Summary

| Service | Free Tier | Type |
|---|---|---|
| Lambda | 1M requests + 400K GB-seconds/month | Always Free |
| API Gateway | 1M REST calls/month | 12 months |
| S3 | 5 GB storage + 20K GET/2K PUT | Always Free |
| DynamoDB | 25 GB + 25 RCU/WCU | Always Free |
| CloudFront | 1 TB transfer/month | Always Free |
| Rekognition | 1,000 images/month | 12 months |
| Bedrock | No free tier — uses hackathon credits | Pay-per-use |

## Assumptions and Constraints

### Assumptions
- User has an ARCore-compatible Android device (API 24+)
- User has internet access for AI features (Bedrock, model downloads)
- Room has sufficient lighting for plane detection
- Prototype uses a curated set of ~15-20 furniture models plus ~10 wall decor items (art, lights, sconces) for the catalog
- AWS hackathon credits are available for Bedrock usage

### Constraints
- Android-only — no iOS support in v1
- AI-generated 3D models may not match original quality perfectly
- Real-time voice input requires on-device speech-to-text (Android SpeechRecognizer) — Transcribe's 60 min/month free tier is too limited
- Bedrock has no free tier — requires hackathon credits or pay-per-use (~$0.001/request with Haiku)
- S3 free tier limited to 5 GB — use compressed GLB models (<5 MB each)
