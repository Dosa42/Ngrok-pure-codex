package com.example.ngrokforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DexState(
    val isDexActive: Boolean = false,
    val isDesktopUiMode: Boolean = false,
    val isSamsungDesktopMode: Boolean = false,
    val modeDescription: String = "Standard Mobile"
)

/**
 * Real Samsung DeX and Android Multi-Window detection service.
 *
 * Utilizes genuine Android and Samsung platform APIs:
 * - Samsung One UI `Configuration.semDesktopModeEnabled` platform field/method reflection.
 * - Standard Android `Configuration.UI_MODE_TYPE_DESK`.
 * - Samsung `SemDesktopModeManager` system service if present on Samsung One UI.
 * - Broadcast receivers for `ENTER_DESKTOP_MODE` and `EXIT_DESKTOP_MODE` lifecycle events.
 */
object SamsungDexManager {
    private const val TAG = "SamsungDexManager"

    // Samsung official broadcast actions
    const val ACTION_ENTER_DEX = "com.samsung.android.desktopmode.action.ENTER_DESKTOP_MODE"
    const val ACTION_EXIT_DEX = "com.samsung.android.desktopmode.action.EXIT_DESKTOP_MODE"
    const val ACTION_ENTER_DESK = "android.app.action.ENTER_DESKTOP_MODE"
    const val ACTION_EXIT_DESK = "android.app.action.EXIT_DESKTOP_MODE"

    private val _dexState = MutableStateFlow(DexState())
    val dexState: StateFlow<DexState> = _dexState.asStateFlow()

    private var receiverRegistered = false

    private val dexReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            Log.d(TAG, "Samsung DeX state broadcast received: $action")
            context?.let { updateState(it) }
        }
    }

    /**
     * Initializes DeX detection and registers dynamic broadcast receivers for docking events.
     */
    fun initialize(context: Context) {
        val appContext = context.applicationContext ?: context
        updateState(appContext)

        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_ENTER_DEX)
                addAction(ACTION_EXIT_DEX)
                addAction(ACTION_ENTER_DESK)
                addAction(ACTION_EXIT_DESK)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appContext.registerReceiver(dexReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    appContext.registerReceiver(dexReceiver, filter)
                }
                receiverRegistered = true
                Log.i(TAG, "Registered real Samsung DeX broadcast listeners")
            } catch (e: Exception) {
                Log.w(TAG, "Could not register DeX broadcast receiver: ${e.message}")
            }
        }
    }

    /**
     * Evaluates current configuration against Samsung One UI and Android platform APIs.
     */
    fun updateState(context: Context): DexState {
        val config = context.resources.configuration

        // 1. Android standard desktop UI mode
        val uiMode = (config.uiMode and Configuration.UI_MODE_TYPE_MASK)
        val isDesktopUi = uiMode == Configuration.UI_MODE_TYPE_DESK

        // 2. Samsung One UI semDesktopModeEnabled (via reflection on real Samsung devices)
        var isSamsungSemDesktop = false
        try {
            val semDesktopField = config.javaClass.getField("semDesktopModeEnabled")
            val rawValue = semDesktopField.getInt(config)
            isSamsungSemDesktop = (rawValue == 1) // 1 == SEM_DESKTOP_MODE_ENABLED
        } catch (_: NoSuchFieldException) {
            try {
                val semDesktopMethod = config.javaClass.getMethod("semDesktopModeEnabled")
                val result = semDesktopMethod.invoke(config) as? Int ?: 0
                isSamsungSemDesktop = (result == 1)
            } catch (_: Throwable) {
                // Not a Samsung device or running on generic AOSP
            }
        } catch (_: Throwable) {
            // Ignore reflection errors on non-Samsung devices
        }

        // 3. Samsung Desktop Mode Service (SemDesktopModeManager)
        var isSamsungServiceActive = false
        try {
            val desktopService = context.getSystemService("desktopmode")
            if (desktopService != null) {
                val getStateMethod = desktopService.javaClass.getMethod("getDesktopModeState")
                val stateObj = getStateMethod.invoke(desktopService)
                if (stateObj != null) {
                    val getEnabledMethod = stateObj.javaClass.getMethod("getEnabled")
                    val enabledValue = getEnabledMethod.invoke(stateObj) as? Int
                    // SemDesktopModeState.ENABLED == 4 in Samsung framework
                    isSamsungServiceActive = (enabledValue == 4)
                }
            }
        } catch (_: Throwable) {
            // Not supported on non-Samsung systems
        }

        val isDexActive = isDesktopUi || isSamsungSemDesktop || isSamsungServiceActive
        val modeDesc = when {
            isSamsungSemDesktop || isSamsungServiceActive -> "Samsung DeX (Desktop Mode)"
            isDesktopUi -> "Desktop Mode (UI_MODE_TYPE_DESK)"
            else -> "Standard Mobile / Tablet"
        }

        val state = DexState(
            isDexActive = isDexActive,
            isDesktopUiMode = isDesktopUi,
            isSamsungDesktopMode = isSamsungSemDesktop || isSamsungServiceActive,
            modeDescription = modeDesc
        )
        _dexState.value = state
        return state
    }
}
