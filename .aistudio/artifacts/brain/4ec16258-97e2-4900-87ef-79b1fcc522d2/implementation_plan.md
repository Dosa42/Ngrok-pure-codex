# Implementation Plan - Native Samsung DeX & Platform Multi-Window Integration

## 1. Problem & Context
The project previously generated a placeholder/mock `samsung-dex-sdk.jar` inside the CI workflow with dummy methods (`return false;`), which was misleading and did not provide actual Samsung functionality.

On real Samsung Galaxy devices running One UI, Samsung DeX does not require any proprietary SDK JAR. Samsung officially supports DeX through:
1. **Samsung Configuration API**: `Configuration.semDesktopModeEnabled == 1` (queried via platform reflection).
2. **Android Desktop UI Mode**: `(Configuration.uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_DESK`.
3. **Samsung DeX System Broadcasts**:
   - `com.samsung.android.desktopmode.action.ENTER_DESKTOP_MODE`
   - `com.samsung.android.desktopmode.action.EXIT_DESKTOP_MODE`
4. **Android Multi-Window & Adaptive Insets**: `Activity.isInMultiWindowMode`, `WindowMetrics`, and desktop mouse/pointer input callbacks.

The user has explicitly requested to:
- Implement real Samsung DeX support using native Android and Samsung platform APIs.
- Remove the stub JAR and its generation script completely.

---

## 2. Proposed Changes

### A. Remove Stub JAR and CI Fallback
- **Workflow (`.github/workflows/build-release-apk.yml`)**:
  - Remove the "Bootstrap Samsung DeX SDK Jar" step that compiled stub Java files into `samsung-dex-sdk.jar`.
  - Ensure CI builds compile cleanly using only official Android and Samsung platform APIs.
- **Repository Cleanup**:
  - Remove any generated `app/libs/samsung-dex-sdk.jar` from the filesystem.
  - Verify `app/build.gradle.kts` does not require external stub libraries.

### B. Implement Real `SamsungDexManager`
- Create `app/src/main/java/com/example/ngrokforwarder/SamsungDexManager.kt`:
  - **Dynamic Detection**:
    - Detects DeX mode through `Configuration.semDesktopModeEnabled` via reflection.
    - Detects Android Desktop UI Mode (`UI_MODE_TYPE_DESK`).
    - Detects Samsung DeX Standalone mode vs. Dual Display mode via `SemDesktopModeManager` if available.
  - **Lifecycle & Broadcasts**:
    - Registers a `BroadcastReceiver` for `com.samsung.android.desktopmode.action.ENTER_DESKTOP_MODE` and `EXIT_DESKTOP_MODE`.
    - Exposes a reactive `StateFlow<Boolean>` (`isDexModeActive`) that updates in real time when docking/undocking.
  - **Pointer & Hover Support**:
    - Exposes desktop window and cursor capabilities for enhanced mouse hover interactions.

### C. Update `AndroidManifest.xml` for Samsung DeX Compatibility
- Add Samsung DeX and multi-window metadata tags inside `<application>` and `<activity>`:
  - `<meta-data android:name="com.samsung.android.keepalive.density" android:value="true"/>` (prevents app restart on DeX resolution change).
  - `<meta-data android:name="supports_dex_dual_mode" android:value="true"/>` (enables DeX dual monitor mode).
  - Ensure `android:resizeableActivity="true"`.
  - Ensure `android:configChanges` handles `uiMode|screenSize|smallestScreenSize|orientation|keyboardHidden` so the Compose UI responds dynamically to docking without tearing down the Activity.

### D. Connect Real DeX State to UI in `ForwarderViewModel` & `ForwarderScreen`
- Inject or observe `SamsungDexManager.isDexModeActive` in `ForwarderViewModel`.
- When in DeX desktop mode, the UI dynamically displays:
  - DeX Status Banner and desktop-optimized dual-pane layout.
  - Pointer hover indicators on buttons and controls.
  - Multi-window navigation rail.

---

## 3. Verification Plan
- **Unit & Robolectric Tests**:
  - Add test cases in `app/src/test/java/com/example/ExampleRobolectricTest.kt` verifying `SamsungDexManager` reflection handling, fallback to standard Android `UI_MODE_TYPE_DESK`, and broadcast receiver transitions.
- **Gradle Tasks**:
  - Run `gradle :app:assembleDebug` and `gradle :app:assembleRelease` to guarantee the build succeeds without the stub JAR.
  - Run `gradle :app:testDebugUnitTest` to verify test suite passes.
  - Run `compile_applet` for full platform verification.
