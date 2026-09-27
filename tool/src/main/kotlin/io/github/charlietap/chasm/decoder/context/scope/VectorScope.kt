package io.github.charlietap.chasm.decoder.context.scope

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal inline fun <T> VectorScope(
    context: ModuleDecoderContext,
    index: Int,
    crossinline decoder: Decoder<T>,
): Result<T, WasmDecodeError> {
    val previousIndex = context.index
    context.index = index

    return try {
        decoder(context)
    } finally {
        context.index = previousIndex
    }
}
