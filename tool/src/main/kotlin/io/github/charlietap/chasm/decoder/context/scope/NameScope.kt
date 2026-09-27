package io.github.charlietap.chasm.decoder.context.scope

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal inline fun <T> NameScope(
    context: ModuleDecoderContext,
    size: UInt,
    crossinline decoder: Decoder<T>,
): Result<T, WasmDecodeError> {
    val previousNameSectionSize = context.nameSectionSize
    context.nameSectionSize = size

    return try {
        decoder(context)
    } finally {
        context.nameSectionSize = previousNameSectionSize
    }
}
