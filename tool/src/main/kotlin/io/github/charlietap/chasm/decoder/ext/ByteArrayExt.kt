package io.github.charlietap.chasm.decoder.ext

import io.github.charlietap.chasm.decoder.error.ValueDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeException

internal fun ByteArray.toFloatLe(): Float {
    if (size != 4) {
        throw WasmDecodeException(ValueDecodeError.InvalidFloat(this))
    }

    val intValue = foldIndexed(0) { index, acc, byte ->
        acc or (byte.toInt() and 0xFF shl (8 * index))
    }

    return Float.fromBits(intValue)
}

internal fun ByteArray.toDoubleLe(): Double {
    if (size != 8) {
        throw WasmDecodeException(ValueDecodeError.InvalidDouble(this))
    }

    val longValue = foldIndexed(0L) { index, acc, byte ->
        acc or (byte.toLong() and 0xFF shl (8 * index))
    }

    return Double.fromBits(longValue)
}
