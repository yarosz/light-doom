package com.tap.mood.doom.runtime.generated

import kotlin.ByteArray
import kotlin.Int

public interface DoomWasmModule {
  public val memory: Memory

  public fun tickGame(): Int

  public fun setRenderEnabled(p0: Int)

  public fun configureView(p0: Int, p1: Int)

  public fun setGammaLevel(p0: Int)

  public fun getInputMode(): Int

  public fun reportKeyDown(p0: Int)

  public fun reportKeyUp(p0: Int)

  public fun initGame()

  public interface Memory {
    public fun read(
      buffer: ByteArray,
      memoryPointer: Int,
      bufferPointer: Int = 0,
      bytesToRead: Int = buffer.size - bufferPointer,
    ): ByteArray

    public fun write(
      pointer: Int,
      buffer: ByteArray,
      bufferPointer: Int = 0,
      bytesToWrite: Int = buffer.size - bufferPointer,
    )
  }
}
