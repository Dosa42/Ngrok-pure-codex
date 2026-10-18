# Samsung DeX Mode & Samsung SDK Integration

Enabling full Samsung DeX desktop mode compatibility, bundling the Samsung MultiWindow SDK library, providing an expanded dual-pane desktop layout with navigation rails and mouse hover interaction, and integrating DeX build validation into the GitHub Actions CI workflow.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following choices confirmed in Phase 1 guide this implementation:

- **Confirmed Decision 1 (Samsung DeX & MultiWindow Manifest Declarations)**: Add Samsung DeX and MultiWindow metadata tags, `resizeableActivity="true"`, and complete `configChanges` handling (`orientation|screenSize|screenLayout|smallestScreenSize|uiMode|density`) to prevent activity reloads when docking/undocking from DeX mode.
- **Confirmed Decision 2 (Bundled Samsung SDK Library)**: Bundle the Samsung MultiDisplay / DeX SDK library directly within `app/libs/` and wire it via Gradle `fileTree("libs")`, ensuring self-contained offline builds without external repository dependencies.
- **Confirmed Decision 3 (Desktop & DeX Adaptive Layout)**: Implement an adaptive dual-pane layout using `BoxWithConstraints` (or WindowSizeClass):
  - On wide / desktop screens (> 600dp, such as external monitors in DeX mode): renders a spacious two-column layout with Target Controls and Codex status on the left, and the Live JSON Forwarding Terminal on the right.
  - On compact / mobile screens: maintains the streamlined vertical scroll flow.
  - Adds pointer input hover cues and mouse cursor indicators for desktop navigation.
- **Confirmed Decision 4 (GitHub Actions Workflow Integration)**: Update `.github/workflows/build-release-apk.yml` to verify and bundle the Samsung SDK library, ensure `app/libs/` is preserved, and test building the DeX-compatible release APK.

---

### 1. Overview & Core Concept

- **What It Does**: Optimizes the Ngrok Forwarder for professional workstation use on Samsung DeX (connected to monitors, keyboards, and mice). The app runs seamlessly in resizable free-form desktop windows, responds smoothly to display scaling and docking transitions without restarting, and takes advantage of the larger screen with a dedicated dual-pane DevTools console.
- **Target Audience / Persona**: Engineers and power users who use Samsung DeX as their primary desktop development environment to run local services (such as Codex Proxy on Termux) and tunnel them to the internet.
- **Key Value**: First-class desktop ergonomics with zero window-stretching, full multi-window support, and instant keyboard/mouse productivity.

---

### 2. User Experience & Visual Design

#### Adaptive Layout Specifications
- **Mobile Mode (< 600dp)**: Vertical single-column feed with collapsible cards.
- **Samsung DeX Desktop Mode (>= 600dp)**:
  - **Left Pane (40% width, min 320dp)**:
    - Target Service Configuration (Host, Port, Quick Presets)
    - Codex Proxy Auto-Discovery Status & Model Badges
    - Pre-Configured Test Suite (1-tap Manifest, Models, Chat, Catalog)
    - Ngrok Credentials & In-Process Tunnel Controls
    - Public URL Banner & Share Actions
    - Socket Pool & Telemetry Metrics
  - **Right Pane (60% width)**:
    - Full-height Live JSON Forwarding Terminal (STDOUT)
    - Live search/filter bar
    - Dedicated JSON Payload Inspector with syntax highlighting and copy actions
  - **Desktop Mouse Interaction**: Hover states with highlight borders (`DarkOutline` -> `NeonCyan`) on cards and buttons when hovered by a mouse or trackpad.

#### Visual Theme
- High-tech dark terminal aesthetic with desktop density, crisp monospace fonts, and DeX window controls.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Manifest Window Rules for DeX**:
  - *Chosen Approach*: Declare `android:resizeableActivity="true"`, `com.samsung.android.keepalive.density`, `com.sec.android.multiwindow.activity.INTERACTION_MODE = 2`, and `configChanges` in `AndroidManifest.xml`.
  - *Why*: Required by Samsung DeX to allow freeform window resizing, maximizing, and multi-display dragging without crashing or restarting the activity.
- **Decision 2: Local Bundled SDK in `app/libs`**:
  - *Chosen Approach*: Place the Samsung SDK jar inside `app/libs/` and reference `implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))` in `app/build.gradle.kts`.
  - *Why*: Eliminates remote Maven downtime and guarantees deterministic CI compilation on GitHub Actions runners.
- **Decision 3: Container-Based Adaptive Layout**:
  - *Chosen Approach*: Use `BoxWithConstraints` to dynamically switch between single-column and dual-pane desktop layouts based on the current window bounds rather than physical screen hardware.
  - *Why*: Seamlessly transitions when the user resizes the DeX window or snaps it to half-screen.

---

### 4. Technical Architecture & Data Strategy

#### Architectural Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                 Samsung DeX Desktop Window                  │
│                                                             │
│ ┌───────────────────────────┐ ┌───────────────────────────┐ │
│ │ Left Pane: Controls       │ │ Right Pane: Live Terminal │ │
│ │ - Dynamic Target Config   │ │ - Full-Height Terminal    │ │
│ │ - Codex Proxy Banner      │ │ - Live Stdout Stream      │ │
│ │ - Pre-Configured Tests    │ │ - JSON Search & Filter    │ │
│ │ - Tunnel Start/Stop       │ │ - Single Payload Viewer   │ │
│ │ - Socket Pool Stats       │ │ - Export & Copy           │ │
│ └─────────────┬─────────────┘ └─────────────┬─────────────┘ │
└───────────────┼─────────────────────────────┼───────────────┘
                │                             │
┌───────────────▼─────────────────────────────▼───────────────┐
│     ForwarderViewModel & Samsung DeX Manifest Handlers      │
│     - OnConfigurationChanged keep-alive                     │
│     - Responsive Window Constraints                         │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│        Native C++ JNI Engine (libngrokforwarder.so)         │
│        - High-throughput keep-alive socket pool             │
│        - Full stdout JSON streaming                         │
└─────────────────────────────────────────────────────────────┘
```

#### Files to Create & Update:
1. `app/libs/samsung-dex-sdk.jar`: Bundled Samsung DeX MultiWindow SDK library.
2. `app/build.gradle.kts`: Add `fileTree("libs")` dependency.
3. `app/src/main/AndroidManifest.xml`: Add Samsung DeX and MultiWindow metadata tags and activity configuration.
4. `app/src/main/java/com/example/ngrokforwarder/ForwarderScreen.kt`: Implement adaptive dual-pane desktop layout for DeX with hover interaction.
5. `.github/workflows/build-release-apk.yml`: Verify `app/libs` bundling in release APK build.
