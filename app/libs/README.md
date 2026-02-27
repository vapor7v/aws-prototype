# Unity AAR Placeholder

Place `unity-classes.aar` in this directory after exporting from Unity.

## How to generate the AAR

1. Open `unity_project/` in Unity Editor (2022.3 LTS recommended)
2. Go to **File → Build Settings**
3. Select **Android** platform
4. Check **"Export Project"** and enable **"Export as Google Android Library"**
5. Click **Export** and choose an output folder
6. Copy the generated `.aar` file into this `libs/` directory
7. Rename it to `unity-classes.aar`

The `build.gradle.kts` will automatically pick it up via:
```kotlin
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
```
