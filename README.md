# AcerolaShaders

Android ports of shader effects by Garrett Gunnell (Acerola), written in AGSL and run through `RuntimeShader`. The project follows [AcerolaFX](https://github.com/GarrettGunnell/AcerolaFX) and [CRT-Shader](https://github.com/GarrettGunnell/CRT-Shader).

## Quick start

Clone the repository and install the sample app on a device or emulator that runs Android 14 (API 34) or higher.

```bash
git clone https://github.com/soulcramer/AcerolaShaders.git
cd AcerolaShaders
./gradlew :app:installDebug
```

Gradle resolves the required JDK on first run through the Foojay toolchain plugin.

## Modules

| Module | Purpose | Published |
|---|---|---|
| `app` | Compose sample app. Shows each shader on a drawable or on a photo-picker image. | No |
| `colorblindness` | Compose modifier (`Modifier.colorBlindness`) for the colour-blindness shader, and its AGSL source as a Kotlin string (`ColorBlindnessShader`). | Yes |
| `shaders-bom` | Bill of materials for the published libraries. | Yes |

## Ported effects

| Effect | Upstream source | Status |
|---|---|---|
| Colour blindness simulation (protanomaly, deuteranomaly, tritanomaly) | `AcerolaFX_ColorBlindness.fx` in AcerolaFX | Ported |
| CRT screen (barrel warp, scanline colour fringing, vignette) | `AcerolaFX_CRT.fx` in AcerolaFX | Ported |

The CRT effect lives in the `app` module only. It is not published as a library.

## Use the colour-blindness shader

Add the BOM, then the library, to a module that uses Jetpack Compose. No stable release exists yet. The current version is `1.0.0-SNAPSHOT`.

```kotlin
dependencies {
    implementation(platform("app.soulcramer.shaders:shaders-bom:<version>"))
    implementation("app.soulcramer.shaders:colorblindness")
}
```

Apply the `colorBlindness` modifier to the content to filter. The severity runs from 0 (no deficiency) to 1 (full deficiency).

```kotlin
Image(
    painter = painter,
    contentDescription = null,
    modifier = Modifier.colorBlindness(type = ColorBlindnessType.Deuteranomaly, severity = 0.6f),
)
```

To use the shader outside this modifier, build a `RuntimeShader` from the exposed AGSL source and set its uniforms:

```kotlin
val runtimeShader = RuntimeShader(ColorBlindnessShader)
runtimeShader.setFloatUniform("severity", severity)
runtimeShader.setIntUniform("colorblindType", type.code) // 0 = Protanomaly, 1 = Deuteranomaly, 2 = Tritanomaly
```

The shader reads its input from the `composable` child shader. Pass `composable` as the uniform name to `RenderEffect.createRuntimeShaderEffect`, or set a child shader with `setInputShader`.

## Build and verify

```bash
./gradlew assembleDebug      # build the app and the colorblindness library
./gradlew spotlessCheck      # check formatting
./gradlew lintDebug          # run Android Lint on every module
./gradlew :dokkaGenerate     # generate API docs for the published libraries
```

## Requirements

- A device or emulator that runs Android 14 (API 34) or higher. The app and the library set minSdk 34. `RuntimeShader` needs API 33.
- The Gradle wrapper (9.8.0). The build turns on isolated projects.
- A JDK that Gradle can resolve through the Foojay toolchain plugin. The daemon pins JetBrains Runtime 25 and downloads it automatically.

## Credits

The shaders are ports of effects from [AcerolaFX](https://github.com/GarrettGunnell/AcerolaFX) by Garrett Gunnell (Acerola), which uses the MIT licence. [CRT-Shader](https://github.com/GarrettGunnell/CRT-Shader) shows the same CRT effect in Unity, but this project copies no code from it.

## Licence

MIT. See [the LICENSE file](LICENSE), which also contains the AcerolaFX copyright notice.
