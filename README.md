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
| `colorblindness` | AGSL source for the colour-blindness shader, exposed as a Kotlin string (`ColorBlindnessShader`). | Yes |
| `shaders-bom` | Bill of materials for the published libraries. | Yes |

## Ported effects

| Effect | Upstream source | Status |
|---|---|---|
| Colour blindness simulation (protanomaly, deuteranomaly, tritanomaly) | `AcerolaFX_ColorBlindness.fx` in AcerolaFX | Ported |
| CRT screen (barrel warp, scanline colour fringing, vignette) | CRT-Shader, and `AcerolaFX_CRT.fx` in AcerolaFX | Ported |

The CRT effect lives in the `app` module only. It is not published as a library.

## Use the colour-blindness shader

Add the BOM, then the library, to a module that uses Jetpack Compose. No stable release exists yet. The current version is `1.0.0-SNAPSHOT`.

```kotlin
dependencies {
    implementation(platform("app.soulcramer.shaders:shaders-bom:<version>"))
    implementation("app.soulcramer.shaders:colorblindness")
}
```

Build a `RuntimeShader` from the exposed AGSL source and set its uniforms:

```kotlin
val runtimeShader = RuntimeShader(ColorBlindnessShader)
runtimeShader.setFloatUniform("severity", severity)
runtimeShader.setIntUniform("colorblindType", type.ordinal) // 0 = Protanomaly, 1 = Deuteranomaly, 2 = Tritanomaly
```

Attach the shader as a `RenderEffect` through `graphicsLayer`, with `composable` as the uniform name for the layer content. See `ShaderImage` in the `app` module for a full example.

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

The shaders are ports of effects by Garrett Gunnell (Acerola):

- [AcerolaFX](https://github.com/GarrettGunnell/AcerolaFX): the colour-blindness simulation.
- [CRT-Shader](https://github.com/GarrettGunnell/CRT-Shader): the CRT screen effect.

Both upstream projects use the MIT licence.

## Licence

MIT, as declared in the publishing configuration. No `LICENSE` file exists in this repository yet.
