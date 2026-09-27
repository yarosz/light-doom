package com.yarosz.doom

import android.util.Log
import com.tap.mood.doom.runtime.input.Key
import com.tap.mood.doom.runtime.instance.Instance
import kotlin.math.abs

/**
 * Held Doom keys, as a set: every input surface asks for the keys it wants and [Keys] presses and releases the
 * difference, so two surfaces holding the same key (the shutter and a touch both firing) never release it early.
 */
class Keys(private val instance: Instance) {
    private val holders = mutableMapOf<Key, MutableSet<String>>()

    /** Uptime of the last key the player caused; keep-awake lapses when this goes stale. */
    @Volatile var lastInputMillis = android.os.SystemClock.uptimeMillis()
        private set

    @Synchronized
    fun hold(owner: String, wanted: Set<Key>) {
        for ((key, owners) in holders) {
            if (key !in wanted && owners.remove(owner) && owners.isEmpty()) change(key, false)
        }
        for (key in wanted) {
            val owners = holders.getOrPut(key) { mutableSetOf() }
            if (owners.add(owner) && owners.size == 1) change(key, true)
        }
    }

    @Synchronized
    fun releaseAll() {
        holders.clear()
        instance.releaseAllKeys()
    }

    fun tap(key: Key) {
        Log.i(TAG, "input tap ${key.code}")
        lastInputMillis = android.os.SystemClock.uptimeMillis()
        instance.tapKey(key)
    }

    private fun change(key: Key, pressed: Boolean) {
        Log.i(TAG, "input ${if (pressed) "down" else "up"} ${key.code}")
        lastInputMillis = android.os.SystemClock.uptimeMillis()
        instance.setKeyPressed(key, pressed)
    }
}

/** Movement stick: displacement (x right, y down, each -1..1) to walk and strafe keys, Mood's dead-zone rule. */
fun movementKeys(x: Float, y: Float): Set<Key> = buildSet {
    if (x * x + y * y < STICK_DEAD_ZONE * STICK_DEAD_ZONE) return@buildSet
    if (y < -AXIS) add(Key.UP)
    if (y > AXIS) add(Key.DOWN)
    if (x < -AXIS) add(Key.STRAFE_LEFT)
    if (x > AXIS) add(Key.STRAFE_RIGHT)
}

/**
 * Proportional turning on a key-only engine. Doom turns slowly for its first six tics of a held turn key, then at
 * walking speed, and twice that while running. A small displacement pulses the key (slow turns, duty cycle
 * proportional to the drag); past [TURN_HOLD] the key is held; past [TURN_RUN] run is held too.
 */
class Turning {
    private var credit = 0f

    /** Called once per Doom tic with the turn surface's horizontal displacement (-1..1). */
    fun tick(x: Float): Set<Key> {
        val a = abs(x)
        if (a < TURN_DEAD_ZONE) {
            credit = 0f
            return emptySet()
        }
        val key = if (x < 0) Key.LEFT else Key.RIGHT
        if (a >= TURN_RUN) return setOf(key, Key.SHIFT)
        if (a >= TURN_HOLD) return setOf(key)
        credit += (a - TURN_DEAD_ZONE) / (TURN_HOLD - TURN_DEAD_ZONE)
        if (credit < 1f) return emptySet()
        credit -= 1f
        return setOf(key)
    }
}

const val TIC_MILLIS = 28L
private const val STICK_DEAD_ZONE = 0.3f
private const val AXIS = 0.35f
private const val TURN_DEAD_ZONE = 0.06f
private const val TURN_HOLD = 0.55f
private const val TURN_RUN = 0.9f
