# GitHub Actions CI Workflow Fix & Dual APK Packaging

A robust, resilient GitHub Actions CI workflow that resolves step execution failures (`exit code 2`), adds step-by-step diagnostic shell tracing, relies on Gradle auto-download for Android SDK/NDK/CMake components with pre-seeded licenses, and builds and uploads both Release and Debug APKs.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during the interactive interview:
> - **Build Scope & Diagnostics**: Add step-by-step shell tracing (`set -x`) and build both **Release** and **Debug** APK variants in CI.
> - **SDK & NDK Management**: Rely primarily on Gradle's native auto-download engine (`android.builder.sdkDownload=true`) with pre-seeded SDK licenses rather than executing fragile command-line `sdkmanager` flags.

---

## 1. Overview & Core Concept

- **The Problem**: GitHub Actions runs failed with `Process completed with exit code 2` and `No Gradle build results detected`. The error occurred prior to Gradle execution due to:
  1. A missing classpath file dependency (`$ANDROID_HOME/platforms/android-36/android.jar`) in the fallback Samsung SDK stub compilation step.
  2. Permission and licensing hurdles when calling `sdkmanager` in the Ubuntu runner environment.
- **The Solution**: 
  1. Modernize to `actions/setup-java@v5` (Node.js 24 runtime).
  2. Pre-seed the official Android SDK license hashes into `$ANDROID_SDK_PATH/licenses/` with runner ownership permissions (`sudo chown`).
  3. Make the Samsung DeX MultiWindow SDK stub generator completely self-contained with mock `Context` and `Configuration` stubs, removing all external Android platform jar requirements.
  4. Build both `:app:assembleDebug` and `:app:assembleRelease` using the self-generated CI keystore.
  5. Package and upload both `ngrok-forwarder-debug.apk` and `ngrok-forwarder-release.apk` to GitHub Actions run artifacts.

---

## 2. CI Workflow Architecture & Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│                 GitHub Actions Runner (Ubuntu)             │
└──────────────────────────────┬──────────────────────────────┘
                               │
            ┌──────────────────▼──────────────────┐
            │  1. Checkout & Java 21 (setup-java@v5)│
            └──────────────────┬──────────────────┘
                               │
            ┌──────────────────▼──────────────────┐
            │  2. License Pre-Seeding (Non-Fatal) │
            │     - android-sdk-license           │
            │     - android-sdk-preview-license   │
            │     - android.builder.sdkDownload=1 │
            └──────────────────┬──────────────────┘
                               │
            ┌──────────────────▼──────────────────┐
            │  3. Verify Samsung DeX MultiWindow  │
            │     - Zero external classpath       │
            │     - Self-contained stub compile   │
            │     - Output: samsung-dex-sdk.jar   │
            └──────────────────┬──────────────────┘
                               │
            ┌──────────────────▼──────────────────┐
            │  4. Keystore Generation & Verify    │
            │     - my-upload-key.jks at workspace│
            └──────────────────┬──────────────────┘
                               │
            ┌──────────────────▼──────────────────┐
            │  5. Gradle Assemble (Release+Debug) │
            │     - Shell tracing (set -x)        │
            │     - ./gradlew :app:assembleDebug  │
            │     - ./gradlew :app:assembleRelease│
            └──────────────────┬──────────────────┘
                               │
            ┌──────────────────▼──────────────────┐
            │  6. Artifact Packaging & Upload     │
            │     - ngrok-forwarder-debug.apk     │
            │     - ngrok-forwarder-release.apk   │
            │     - GitHub Release (on v* tag)    │
            └─────────────────────────────────────┘
```

---

## 3. Key Product & Technical Decisions

- **Decision 1: Standalone Samsung DeX SDK Stub Generation**
  - *Chosen Approach*: Write pure-Java stubs for `Context`, `Configuration`, `SMultiWindow`, and `DexManager` inside `/tmp/samsung_sdk` and compile with `javac -source 11 -target 11` without specifying any external `-cp`.
  - *Why*: Fresh GitHub Actions runners do not have `platforms/android-36/android.jar` installed prior to Gradle invocation. Compiling standalone stubs guarantees `exit code 0` on every runner.
  - *Alternative Considered*: Running `sdkmanager "platforms;android-36"` before Gradle, which is slow (adds 1–2 minutes) and prone to runner network timeouts.

- **Decision 2: Gradle Native Auto-Download Strategy**
  - *Chosen Approach*: Rely on `android.builder.sdkDownload=true` in `gradle.properties` and pre-seed the license agreement hashes.
  - *Why*: Gradle automatically downloads exact CMake and NDK matching versions on demand during build task compilation.
  - *Alternative Considered*: Manual `sdkmanager` installation scripts with complex flag variants.

- **Decision 3: Dual APK Build (Debug & Release)**
  - *Chosen Approach*: Build both variants in sequence; package debug and signed release APKs into the workflow artifacts.
  - *Why*: Gives the user immediate access to both a lightweight, unminified debug APK for quick testing and an optimized signed release APK for deployment.

---

## 4. Implementation Steps & Status

- [x] **1. Update `.github/workflows/build-release-apk.yml`**:
  - [x] Added `set -x` shell tracing to all critical steps for clear diagnostic logs.
  - [x] Pre-seeded Android SDK and NDK licenses into `$ANDROID_SDK_PATH/licenses/` with write permissions.
  - [x] Configured self-contained Samsung DeX MultiWindow SDK stub generation without external classpath dependencies.
  - [x] Setup both `my-upload-key.jks` and `debug.keystore` to satisfy signing requirements for both release and debug variants.
  - [x] Added dual build execution: `./gradlew :app:assembleDebug :app:assembleRelease --no-daemon --stacktrace`.
  - [x] Packaged and uploaded both `ngrok-forwarder-debug.apk` and `ngrok-forwarder-release.apk` under `ngrok-forwarder-apks`.
  - [x] Attached both APKs to GitHub releases on `v*` tags.

- [x] **2. Local Build & Test Verification**:
  - [x] `gradle :app:assembleRelease`: Verified BUILD SUCCESSFUL.
  - [x] `gradle :app:assembleDebug`: Verified BUILD SUCCESSFUL.
  - [x] `gradle :app:testDebugUnitTest`: Verified all unit tests pass (BUILD SUCCESSFUL).
  - [x] `compile_applet`: Verified applet builds cleanly.
  - [x] Adaptive app launcher icon with "NF" logo verified across all density mipmaps.

