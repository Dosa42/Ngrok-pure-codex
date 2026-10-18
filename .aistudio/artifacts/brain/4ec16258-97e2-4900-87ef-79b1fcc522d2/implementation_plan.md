# GitHub Actions Workflow for Release APK Build & Publishing

Automated CI/CD pipeline (`.github/workflows/build-release-apk.yml`) to compile, native-link, self-sign, and publish release APKs with NDK/CMake support, GitHub workflow artifact storage, and automated GitHub Releases.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following choices confirmed in Phase 1 guide this workflow:

- **Confirmed Decision 1 (Self-Contained CI Signing)**: The workflow automatically provisions a self-contained release keystore using `keytool` during the CI run (with fallback to optional repository secrets if provided), guaranteeing that `assembleRelease` produces a fully signed, installable APK without manual keystore upload.
- **Confirmed Decision 2 (Workflow Triggers)**: Triggers automatically on:
  - Push to `main` branch
  - Tag creation matching `v*` (e.g. `v1.0.0`)
  - Manual execution via `workflow_dispatch` with optional build inputs
- **Confirmed Decision 3 (Artifact Publishing & Releases)**:
  - Always uploads the compiled release APK to GitHub Workflow Run Artifacts (`actions/upload-artifact@v4`) retained for 30 days.
  - When triggered by a version tag (`v*`), automatically creates a GitHub Release with changelog and attaches the APK binary (`softprops/action-gh-release@v2`).
- **Confirmed Decision 4 (Native C++ Toolchain in CI)**: Configures Java 21, Android SDK, Gradle cache, CMake 3.22.1, and Android NDK 27.2.12479018 to ensure the native JNI C++ engine (`libngrokforwarder.so`) compiles seamlessly.

---

### 1. Overview & Core Concept

- **What It Does**: Automates the end-to-end build process for the Ngrok Forwarder Android application on Ubuntu GitHub runners. It prepares `.env` configuration, installs required NDK/CMake native toolchains, generates a release keystore, executes `gradle :app:assembleRelease`, signs and zipaligns the APK, and publishes the deliverables.
- **Target Audience**: Maintainers, developers, and users needing immediate access to production-ready, signed APK binaries on every commit or version release.
- **Key Value**: 100% reproducible, zero-configuration cloud builds producing installable APKs directly downloadable from GitHub Actions and Releases.

---

### 2. Workflow Architecture & Steps

#### Build Pipeline Sequence

```
[ Git Push / Tag / Dispatch ]
             │
             ▼
┌────────────────────────────────────────────────────────┐
│  GitHub Actions Runner (ubuntu-latest)                 │
│                                                        │
│  1. Checkout Code (actions/checkout@v4)                │
│  2. Setup JDK 21 (actions/setup-java@v4)               │
│  3. Setup Android SDK, NDK 27.2 & CMake 3.22.1         │
│  4. Gradle Dependency & Build Cache                    │
│  5. Environment Setup (.env pre-seeding)               │
│  6. Self-Contained Release Keystore Generation         │
│  7. Build Release APK (gradle :app:assembleRelease)    │
│  8. Upload APK Artifact (actions/upload-artifact@v4)   │
│  9. Publish GitHub Release (on tag v*)                 │
└────────────────────────────────────────────────────────┘
```

#### Key Steps Breakdown:
1. **Checkout**: `actions/checkout@v4` with full depth for release notes generation.
2. **JDK Setup**: `actions/setup-java@v4` with distribution `temurin` and version `21`.
3. **Android NDK & CMake Setup**: Uses `android-actions/setup-android@v3` or sdkmanager to ensure NDK `27.2.12479018` and CMake `3.22.1` are present for C++ JNI compilation.
4. **Environment Initialization**: Creates `.env` from `.env.example` so the Secrets Gradle Plugin generates `BuildConfig` fields without errors.
5. **Keystore Generation**: Generates `my-upload-key.jks` with alias `upload` and password `android`, passing `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` as environment variables matching `app/build.gradle.kts`.
6. **Compilation**: Runs `gradle :app:assembleRelease --no-daemon --stacktrace`.
7. **Artifact Archive**: Collects `app/build/outputs/apk/release/*.apk` and uploads as `ngrok-forwarder-release-apk`.
8. **Release Publishing**: For version tags, runs `softprops/action-gh-release@v2` with `generate_release_notes: true`.

---

### 3. File Changes

- Create `.github/workflows/build-release-apk.yml`: Complete GitHub Actions workflow YAML.
- Re-create minimal `/.env` if required locally to keep workspace cleanly synced.
