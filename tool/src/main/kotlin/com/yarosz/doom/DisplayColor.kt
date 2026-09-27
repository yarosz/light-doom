package com.yarosz.doom

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Lifts LightOS's global grayscale filter while Doom is on screen in COLOR, the way LightOS's Photos does
 * (light-sdk#190), and always puts back the value it found: on pause, screen-off, home, screen hide, engine stop
 * and crash. How the filter is written comes from [ColorBackend]; in a build without one, COLOR is only the palette.
 */
object DisplayColor {
    enum class Support { UNKNOWN, NONE, NOT_GRANTED, GRANTED }

    val support = MutableStateFlow(Support.UNKNOWN)

    private var access: FilterAccess? = null
    private var attached = false
    private var wanted = false

    /** The filter's value before Doom lifted it; null while Doom has not changed it. */
    private var original: Int? = null

    @Synchronized
    fun attach(access: FilterAccess?) {
        if (attached) return
        attached = true
        this.access = access
        if (access == null) {
            support.value = Support.NONE
            return
        }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            restore()
            previous?.uncaughtException(thread, error)
        }
        apply()
    }

    /** Color on screen while [color] and the Tool is showing; grayscale as LightOS set it otherwise. */
    @Synchronized
    fun want(color: Boolean) {
        wanted = color
        apply()
    }

    /** Put the filter back to what it was. Safe to call any number of times, from any thread. */
    @Synchronized
    fun restore() {
        val value = original ?: return
        if (write(value)) {
            original = null
            Log.i(TAG, "color: filter restored to $value")
        }
    }

    @Synchronized
    private fun apply() {
        val a = access ?: return
        if (!wanted) return restore()
        if (original != null) return
        val current = a.read()
        if (current == 0) return
        if (write(0)) {
            original = current
            Log.i(TAG, "color: filter lifted (was $current)")
        }
    }

    private fun write(value: Int): Boolean {
        val ok = access?.write(value) ?: return false
        if (!ok && support.value != Support.NOT_GRANTED) {
            Log.i(TAG, "color: not granted; adb shell pm grant com.yarosz.doom android.permission.WRITE_SECURE_SETTINGS")
        }
        support.value = if (ok) Support.GRANTED else Support.NOT_GRANTED
        return ok
    }
}
