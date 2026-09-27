package com.yarosz.doom

import android.provider.Settings
import android.view.View

/** Reads and writes LightOS's grayscale filter (1 = grayscale, 0 = color) on behalf of [DisplayColor]. */
interface FilterAccess {
    fun read(): Int

    /** False when this phone refuses the write (the permission isn't granted). */
    fun write(value: Int): Boolean
}

/**
 * Color builds only (`scripts/color-build.sh` copies this over tool/.../ColorBackend.kt): writes the filter
 * setting directly, as LightOS's Photos does. Needs WRITE_SECURE_SETTINGS, which only the plugin patch in
 * color/light-sdk-color.patch lets a Tool declare, granted once per phone:
 *
 *   adb shell pm grant com.yarosz.doom android.permission.WRITE_SECURE_SETTINGS
 */
object ColorBackend {
    private const val FILTER = "accessibility_display_daltonizer_enabled"

    fun attach(view: View): FilterAccess {
        val resolver = view.context.contentResolver
        return object : FilterAccess {
            override fun read() = Settings.Secure.getInt(resolver, FILTER, 1)

            override fun write(value: Int) = try {
                Settings.Secure.putInt(resolver, FILTER, value)
            } catch (_: SecurityException) {
                false
            }
        }
    }
}
