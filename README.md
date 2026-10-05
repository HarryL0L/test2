# Guard Gallery

A local Android gallery app that automatically **blurs NSFW photos and videos** — detection runs
100% on the device. The app does not request the `INTERNET` permission, so your media never leaves
your phone.

## Features

- Browses all photos and videos on the device (MediaStore), newest first.
- On-device NSFW detection with a bundled MobileNetV4 classifier (ONNX Runtime, ~10 MB).
  - Classes: `drawings`, `hentai`, `neutral`, `porn`, `sexy`.
  - Photos (incl. GIF first frame) are classified from a thumbnail.
  - Videos: 6 frames are sampled across the clip; the most explicit frame decides.
- Flagged items are blurred in the grid and in the viewer, with a **Show anyway** button.
  - Blurred thumbnails are decoded at 12×12 px, so detail never reaches the screen, on every
    Android version; Android 12+ additionally applies a real Gaussian blur.
- Optional **Blur until scanned** (on by default) so new items stay hidden until checked.
- Sensitivity: Strict / Balanced / Relaxed, and a toggle for suggestive (non-explicit) content.
- Results are cached per file (`id + date modified`) so each file is only scanned once.
- Material You (Material 3) UI with dynamic wallpaper colours on Android 12+, light/dark theme.

Requires Android 8.0 (API 26) or newer.

## Build

### GitHub Actions

Every push builds the APKs via `.github/workflows/android.yml`. Download them from the workflow
run's **Artifacts** (`guard-gallery-apks`). Pushing a `v*` tag also attaches them to a GitHub
release.

One APK is built per CPU architecture (ONNX Runtime's native code is large):

| APK | Devices |
| --- | --- |
| `app-arm64-v8a-release.apk` | Almost all modern phones — pick this one |
| `app-armeabi-v7a-release.apk` | Older 32-bit phones |
| `app-x86_64-release.apk` | Emulators / Chromebooks |

The release build is minified and signed with the standard debug key so it can be sideloaded.
Set up your own signing key before distributing it anywhere.

### Locally

```sh
./gradlew assembleRelease
# APKs: app/build/outputs/apk/release/
```

Needs JDK 17+ and the Android SDK (compile SDK 37). Built with Gradle 9.8, AGP 9.4, Kotlin 2.4,
Compose BOM 2026.09 (Material 3).

## Project layout

| Path | Purpose |
| --- | --- |
| `ml/NsfwClassifier.kt` | Loads the ONNX model, runs inference, softmax |
| `ml/ImagePreprocessor.kt` | Center-crop + resize to 224×224, CHW floats |
| `data/MediaScanner.kt` | Decodes thumbnails / samples video frames |
| `data/MediaRepository.kt` | Queries MediaStore for images and videos |
| `data/ScanCache.kt` | Persists scan results in app-private storage |
| `ui/` | Compose screens: grid, viewer, settings |

## Model

`app/src/main/assets/nsfw_mobilenetv4.onnx` is
[`taufiqdp/mobilenetv4_conv_small.e2400_r224_in1k_nsfw_classifier`](https://huggingface.co/taufiqdp/mobilenetv4_conv_small.e2400_r224_in1k_nsfw_classifier)
(Apache-2.0). Input: float `[3, 224, 224]` RGB in 0..1 (ImageNet normalisation is inside the graph).
Output: logits `[1, 5]`.

To swap in another model with the same input/output, replace the file and keep the name, or change
`NsfwClassifier.MODEL_ASSET`.

## Limitations

Automatic classification is not perfect: some content may be missed and some safe content may be
blurred. Use **Strict** sensitivity if you prefer over-blurring.
