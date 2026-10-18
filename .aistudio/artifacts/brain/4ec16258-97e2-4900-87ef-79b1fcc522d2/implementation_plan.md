# Implementation Plan - Unique Build Versioning & Enforced Desktop Layout

## 1. Problem & Requirements
The user requested two key enhancements:
1. **Unique Build & Artifact Naming**:
   - Every CI run must generate uniquely named APKs and artifact bundles with an incrementing `v1.0.<run_number>` tag appended to the name (e.g., `ngrok-forwarder-release-v1.0.42.apk`, `ngrok-forwarder-debug-v1.0.42.apk`, and artifact name `ngrok-forwarder-apks-v1.0.42`).
2. **Permanent Desktop Layout Selection in App Source**:
   - The app's source code must **always** select its desktop layout, including after device rotation or window resizing, rather than falling back to single-column mobile view on narrow viewports.

---

## 2. Proposed Changes & Status

### A. Permanent Desktop Layout in Source Code (`ForwarderScreen.kt`) [COMPLETED]
- [x] Set `isDesktopDeX = true` permanently so the app source unconditionally selects the expanded dual-pane DevTools layout.
- [x] Added responsive horizontal and vertical scroll containers so that upon device rotation, window resizing, or running on narrow screens, both panes remain fully navigable and accessible without clipping.
- [x] Connected dynamic top bar status badges for desktop mode.

### B. Dynamic Build Versioning in Gradle (`app/build.gradle.kts`) [COMPLETED]
- [x] Enabled dynamic property overrides:
  - `versionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull() ?: 1`
  - `versionName = project.findProperty("versionName")?.toString() ?: "1.0"`

### C. Unique Naming in CI Workflow (`.github/workflows/build-release-apk.yml`) [COMPLETED]
- [x] **Gradle Build**: Injected `-PversionCode=${{ github.run_number }} -PversionName=1.0.${{ github.run_number }}`.
- [x] **APK Packaging**: Packaged APKs with the unique version tag:
  - `app/build/outputs/apk/packaged/ngrok-forwarder-debug-v1.0.${{ github.run_number }}.apk`
  - `app/build/outputs/apk/packaged/ngrok-forwarder-release-v1.0.${{ github.run_number }}.apk`
- [x] **Artifact Upload**: Dynamic artifact bundle name:
  `name: ngrok-forwarder-apks-v1.0.${{ github.run_number }}`

---

## 4. KSP2 Migration & AWT-EventQueue Resolution [COMPLETED]
- **Root Cause**: KSP1 relied on legacy IntelliJ platform components whose `BinaryFileTypeDecompilers` posted unhandled background events to `AWT-EventQueue-0`, resulting in `Application.getService(...)` NullPointerExceptions on headless CI runners.
- **Fix**: Enabled `ksp.useKSP2=true` in `gradle.properties` and added `-Dksp.useKSP2=true` / `-Pksp.useKSP2=true` to the GitHub Actions workflow. KSP2 runs natively on the Kotlin Analysis API (AA), completely bypassing the legacy IntelliJ event dispatch thread.
- **Verification**: `:app:kspDebugKotlin` and `:app:kspReleaseKotlin` completed with 0 errors or warnings, and all 33 unit tests passed.
