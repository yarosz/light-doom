package io.github.charlietap.chasm.decoder.decoder.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.error.TypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.type.Mutability

internal fun MutabilityDecoder(
    context: ModuleDecoderContext,
): Result<Mutability, WasmDecodeError> = binding {
    when (val byte = context.reader.ubyte()) {
        CONST_MUTABILITY -> Ok(Mutability.Const)
        VAR_MUTABILITY -> Ok(Mutability.Var)
        else -> Err(TypeDecodeError.UnknownMutabilityFlag(byte))
    }.bind()
}

internal const val CONST_MUTABILITY: UByte = 0x00u
internal const val VAR_MUTABILITY: UByte = 0x01u
