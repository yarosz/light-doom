package io.github.charlietap.chasm.decoder.reader

internal interface WasmBinaryReader {

    fun byte(): Byte

    fun ubyte(): UByte

    fun bytes(amount: Int): ByteArray

    fun ubytes(amount: UInt): UByteArray

    fun int(): Int

    fun uint(): UInt

    fun short(): Short

    fun ushort(): UShort

    fun s33(): Long

    fun long(): Long

    fun ulong(): ULong

    fun float(): Float

    fun double(): Double

    fun exhausted(): Boolean

    fun position(): UInt

    fun peekUByte(): UByte

    fun peekUInt(): UInt

    fun pushLimit(size: UInt): ULong = error("Reader does not support byte limits")

    fun restoreLimit(limit: ULong): Unit = error("Reader does not support byte limits")
}
