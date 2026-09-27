package com.yarosz.doom

import android.view.View

/** Reads and writes LightOS's grayscale filter (1 = grayscale, 0 = color) on behalf of [DisplayColor]. */
interface FilterAccess {
    fun read(): Int

    /** False when this phone refuses the write (the permission isn't granted). */
    fun write(value: Int): Boolean
}

/**
 * Where lifting the filter comes from. This committed version, the one Light's builder sees, has no way to: COLOR
 * is only Doom's palette under LightOS's filter. When light-sdk#190 ships, this asks LightOS through the SDK.
 * `scripts/color-build.sh` swaps in `color/ColorBackend.kt` for adb-granted color builds.
 */
object ColorBackend {
    fun attach(view: View): FilterAccess? = null
}
