// Adapted from Mood's android/src/main/kotlin/com/tap/mood/di/AndroidComponents.kt
// (github.com/CharlieTap/mood, Apache-2.0): the same audio, thread and save-slot hosts, without Metro DI or
// Android's Application.
package com.yarosz.doom

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.tap.mood.doom.runtime.engine.AudioFrame
import com.tap.mood.doom.runtime.host.AudioSink
import com.tap.mood.doom.runtime.host.Logger
import com.tap.mood.doom.runtime.host.SaveSlots
import com.tap.mood.doom.runtime.host.SaveStore
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

const val TAG = "Doom"

object DoomLogger : Logger {
    override fun info(message: String) {
        Log.i(TAG, message)
    }

    override fun warning(message: String, throwable: Throwable?) {
        Log.w(TAG, message, throwable)
    }

    override fun error(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }
}

fun engineDispatcher(): ExecutorCoroutineDispatcher =
    Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "Doom-Engine") }.asCoroutineDispatcher()

class FileSaveStore(filesDir: File) : SaveStore {
    private val directory = File(filesDir, "saves")

    override fun load(slot: Int): ByteArray? = file(slot).takeIf(File::isFile)?.readBytes()

    override fun size(slot: Int): Int = file(slot).takeIf(File::isFile)?.length()?.toInt() ?: 0

    override fun save(slot: Int, data: ByteArray): Boolean =
        runCatching {
            check(directory.exists() || directory.mkdirs()) { "Could not create the save directory" }
            val temporary = File(directory, "slot-$slot.tmp")
            temporary.writeBytes(data)
            check(temporary.renameTo(file(slot))) { "Could not move the save into place" }
        }.onFailure { DoomLogger.error("Failed to save slot $slot", it) }.isSuccess

    private fun file(slot: Int): File {
        SaveSlots.requireValid(slot)
        return File(directory, "slot-$slot.sav")
    }
}

class TrackAudioSink : AudioSink {
    private val buffers = ArrayBlockingQueue<Pair<Int, ByteArray>>(BUFFER_COUNT)
    private val lock = Any()

    @Volatile private var active = false
    @Volatile private var closed = false
    @Volatile private var track: AudioTrack? = null

    private val playback = thread(name = "Doom-Audio") {
        try {
            while (!closed) {
                val (rate, pcm) = buffers.take()
                if (!active) continue
                val t = trackFor(rate)
                if (t.playState != AudioTrack.PLAYSTATE_PLAYING) t.play()
                t.write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (t: Throwable) {
            if (!closed) DoomLogger.error("Audio playback failed", t)
        }
    }

    override fun setActive(active: Boolean) {
        this.active = active
        if (!active) {
            buffers.clear()
            synchronized(lock) { track?.run { pause(); flush() } }
        }
    }

    override fun write(frame: AudioFrame) {
        if (!active || closed) return
        val buffer = frame.sampleRate to frame.pcm.copyOf()
        if (!buffers.offer(buffer)) {
            buffers.poll()
            buffers.offer(buffer)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        active = false
        buffers.clear()
        playback.interrupt()
        synchronized(lock) {
            track?.run { runCatching(::stop); release() }
            track = null
        }
    }

    private fun trackFor(rate: Int): AudioTrack =
        track?.takeIf { it.sampleRate == rate } ?: synchronized(lock) {
            track?.takeIf { it.sampleRate == rate } ?: create(rate).also {
                track?.release()
                track = it
                DoomLogger.info("audio track open at $rate Hz stereo")
            }
        }

    private fun create(rate: Int): AudioTrack {
        val minimum = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "Could not determine the audio buffer size" }
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minimum, rate / TIC_RATE * 2 * Short.SIZE_BYTES * BUFFER_COUNT))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private companion object {
        const val TIC_RATE = 35
        const val BUFFER_COUNT = 4
    }
}
