package io.github.charlietap.chasm.decoder.decoder.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.error.ComponentCanonicalDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun AsyncFlagDecoder(
    context: ComponentDecoderContext,
): Result<Boolean, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        ASYNC_FALSE -> false
        ASYNC_TRUE -> true
        else -> Err(ComponentCanonicalDecodeError.InvalidAsyncFlag(opcode)).bind<Boolean>()
    }
}

private const val ASYNC_FALSE: UByte = 0x00u
private const val ASYNC_TRUE: UByte = 0x01u
