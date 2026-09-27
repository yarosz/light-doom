package io.github.charlietap.chasm.decoder.decoder.component

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal inline fun <T> ComponentOptionalDecoder(
    context: ComponentDecoderContext,
    crossinline valueDecoder: ComponentDecoder<T>,
): Result<T?, WasmDecodeError> = binding {
    when (val tag = context.reader.ubyte()) {
        OPTIONAL_NONE -> null
        OPTIONAL_SOME -> valueDecoder(context).bind()
        else -> Err(ComponentDecodeError.InvalidOptionalTag(tag)).bind<T?>()
    }
}

private const val OPTIONAL_NONE: UByte = 0x00u
private const val OPTIONAL_SOME: UByte = 0x01u
