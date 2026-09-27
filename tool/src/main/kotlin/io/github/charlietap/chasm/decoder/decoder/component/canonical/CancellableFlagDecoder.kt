package io.github.charlietap.chasm.decoder.decoder.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.error.ComponentCanonicalDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CancellableFlagDecoder(
    context: ComponentDecoderContext,
): Result<Boolean, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        CANCELLABLE_FALSE -> false
        CANCELLABLE_TRUE -> true
        else -> Err(ComponentCanonicalDecodeError.InvalidCancellableFlag(opcode)).bind<Boolean>()
    }
}

private const val CANCELLABLE_FALSE: UByte = 0x00u
private const val CANCELLABLE_TRUE: UByte = 0x01u
