package io.github.charlietap.chasm.decoder.decoder.component

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentLayerValidator(
    context: ComponentDecoderContext,
): Result<Unit, WasmDecodeError> {
    val layer = context.reader.ubytes(2u)
    return if (layer.contentEquals(COMPONENT_LAYER)) {
        Ok(Unit)
    } else {
        Err(ComponentDecodeError.InvalidLayer(layer))
    }
}

private val COMPONENT_LAYER = ubyteArrayOf(0x01u, 0x00u)
