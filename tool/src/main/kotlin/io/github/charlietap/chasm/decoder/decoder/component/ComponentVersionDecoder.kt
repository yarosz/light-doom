package io.github.charlietap.chasm.decoder.decoder.component

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.Version
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentVersionDecoder(
    context: ComponentDecoderContext,
): Result<Version, WasmDecodeError> {
    val version = context.reader.ubytes(2u)
    return if (version.contentEquals(PROTOTYPE_13)) {
        Ok(Version.Prototype13)
    } else {
        Err(WasmDecodeError.UnsupportedVersion(version))
    }
}

private val PROTOTYPE_13 = ubyteArrayOf(0x0Du, 0x00u)
