# Native JNI In-Process Ngrok Forwarder for 127.0.0.1:8080/*

A native Android application featuring an in-process C++ JNI engine to forward actual local services running on `127.0.0.1:8080/*` to the public internet via ngrok, with zero mocks, zero fake servers, and real socket-level traffic monitoring.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Revisions Incorporated**:
> - **Zero Mock Policy**: Completely eliminated all mock servers, fake responses, and demo HTTP servers.
> - **Real Local Service Forwarding Only**: The native C++ engine targets the actual local service listening on `127.0.0.1:8080/*` via raw POSIX sockets (`connect()` to `127.0.0.1:8080`). If no local service is active on 8080, it reports real `ECONNREFUSED` connection states.
> - **Native C++ JNI Socket Core**: Direct C++ implementation (`tunnel_engine.cpp`) with JNI export methods to manage tunnel sessions, bi-directional socket multiplexing, and byte stream dispatching between the remote ngrok endpoint and `127.0.0.1:8080`.

- **Confirmed Decision 1**: Target endpoint strictly locked to real local service at `127.0.0.1:8080/*`.
- **Confirmed Decision 2**: Ngrok authentication token configured via secure UI input field or `.env` variable (`NGROK_AUTHTOKEN`), passed directly into the native C++ engine.
- **Confirmed Decision 3**: Real socket metrics, live tunnel URL generation, connection state indicator (Connected, Connection Refused on 8080, Tunneling Active), and live stream logs of actual HTTP/TCP traffic passing through the forwarder.

---

### 1. Overview & Core Concept

- **What It Does**: Establishes an in-process native JNI bridge in C++ that connects to the ngrok tunneling service and proxies incoming internet traffic directly into an existing local service running on `127.0.0.1:8080/*` on the Android device.
- **Target Audience / Persona**: Engineers, testers, and power users running local services, daemons, or microservices on Android at `127.0.0.1:8080` that need to be reached from the outside world.
- **Key Value**: Direct, low-latency in-process C++ forwarding without external binary dependencies, providing instant public HTTPS access to local 8080 services.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Target Verification & Auth Setup**:
   - The user opens the app. The UI shows the status of target `127.0.0.1:8080` (probes the port to show if the user's local service is currently listening).
   - The user enters their ngrok Authtoken (persisted securely on-device).
2. **Start Native Forwarder**:
   - Tapping **"Start In-Process Forwarder"** calls the native JNI method `NativeTunnel.startForwarder("127.0.0.1", 8080, authToken)`.
   - The native C++ layer initiates the tunnel session, binds the event loop, and returns the assigned public HTTPS forwarding URL.
3. **Live Public Access & Metrics**:
   - Public URL is displayed with quick Copy and Share actions.
   - Live socket telemetry displays actual bytes transferred (TX/RX), active client socket connections, and real-time requests relayed to `127.0.0.1:8080/*`.
   - If the local service on 8080 is down or crashes, the UI immediately surfaces genuine socket error status (`ECONNREFUSED: No service listening at 127.0.0.1:8080`).

#### Visual Identity & Theme
- **Theme**: DevTools Dark Terminal (`#0A0E17` background, `#111827` surface).
- **Accents**: High-contrast Neon Cyan (`#00F0FF`) for active tunnel links, Emerald Green (`#10B981`) for live 8080 socket connectivity, and Coral Red (`#EF4444`) for local socket refused states.
- **Typography**: Monospace typography for ports, IP addresses, URLs, and real-time request paths.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct POSIX Socket Forwarding in Native C++**:
  - *Chosen Approach*: The native C++ layer handles the TCP socket connection to `127.0.0.1:8080` using standard POSIX socket APIs (`socket`, `connect`, `select`/`poll`, `send`, `recv`).
  - *Why*: Ensures maximum throughput, direct OS-level connection to local loopback, and eliminates JVM socket overhead for payload transmission.
- **Decision 2: Strict Error Reporting for 8080 Service State**:
  - *Chosen Approach*: If the target service on 8080 is not running, the forwarder reports genuine connection errors rather than swallowing errors or fabricating responses.
  - *Why*: Transparent feedback so the developer knows immediately whether their target service on 8080 is responding.
- **Decision 3: In-Memory Real Traffic Relay Inspector**:
  - *Chosen Approach*: The native engine notifies the Kotlin layer of actual relayed HTTP request lines, headers, response status codes, and latencies via JNI callbacks into a StateFlow buffer.
  - *Why*: Full real-time visibility into what internet clients are sending to `127.0.0.1:8080/*`.

---

### 4. Technical Architecture & Data Strategy

#### System Architecture & Flow Diagram

```
[ External Internet Client ]
             │
             │ HTTPS Request (e.g. POST /api/webhook)
             ▼
    [ Ngrok Cloud Edge ]
             │
             │ Encrypted Tunnel Connection (TLS)
             ▼
┌─────────────────────────────────────────────────────────────┐
│                 Android Application Process                 │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │           Native C++ JNI Engine (native-lib)          │  │
│  │  - JNI Export: startForwarder(host, port, token)       │  │
│  │  - JNI Export: stopForwarder()                        │  │
│  │  - JNI Export: pollTrafficEvent()                     │  │
│  │  - Native Thread: In-Process Socket Forwarding Loop   │  │
│  │  - POSIX Sockets: connect("127.0.0.1", 8080)          │  │
│  └───────────────────────────┬───────────────────────────┘  │
│                              │                              │
│                              ▼ Raw TCP Stream Proxy         │
│  ┌───────────────────────────────────────────────────────┐  │
│  │             Actual Target Local Service               │  │
│  │                  127.0.0.1:8080/*                     │  │
│  │            (User's Real Local Web Server)             │  │
│  └───────────────────────────────────────────────────────┘  │
│                              ▲                              │
│                              │ Real Socket Events           │
│  ┌───────────────────────────┴───────────────────────────┐  │
│  │      Kotlin UI & State Management (Jetpack Compose)   │  │
│  │  - NativeTunnelBridge (JNI Wrapper)                   │  │
│  │  - ForwarderViewModel (StateFlow, metrics, logs)      │  │
│  │  - ForwarderScreen (Controls, Real URL, Live Traffic) │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

#### Native JNI Interface Specification (`NativeTunnelBridge`)
- `external fun nativeStart(host: String, port: Int, authtoken: String): Int`
- `external fun nativeStop(): Int`
- `external fun nativeGetPublicUrl(): String?`
- `external fun nativeGetStatus(): Int` (0 = STOPPED, 1 = CONNECTING, 2 = ONLINE, -1 = ERROR)
- `external fun nativeGetStats(): LongArray` (bytesIn, bytesOut, activeConns, totalRequests)
- `external fun nativeProbeLocalPort(host: String, port: Int): Boolean`

#### State & Data Handling
- Real-time port health checker (`probeLocalPort("127.0.0.1", 8080)`) run on interval to inform user if their local server is active.
- Encrypted or SharedPreferences storage for the user's ngrok Authtoken.
- Circular buffer for real socket transaction logs.
