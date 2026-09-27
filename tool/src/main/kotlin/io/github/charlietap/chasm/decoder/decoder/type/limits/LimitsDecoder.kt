package io.github.charlietap.chasm.decoder.decoder.type.limits

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.error.TypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.decoder.reader.WasmBinaryReader
import io.github.charlietap.chasm.type.AddressType
import io.github.charlietap.chasm.type.Limits
import io.github.charlietap.chasm.type.SharedStatus

internal fun LimitsDecoder(
    context: ModuleDecoderContext,
): Result<Triple<Limits, SharedStatus, AddressType>, WasmDecodeError> = binding {

    val (hasMaximum, sharedStatus, addressType) = when (val byte = context.reader.ubyte()) {
        LIMIT_NO_MAX_UNSHARED_I32 -> Triple(false, SharedStatus.Unshared, AddressType.I32)
        LIMIT_MAX_UNSHARED_I32 -> Triple(true, SharedStatus.Unshared, AddressType.I32)
        LIMIT_NO_MAX_SHARED_I32 -> Err(TypeDecodeError.UnboundedSharedLimits).bind()
        LIMIT_MAX_SHARED_I32 -> Triple(true, SharedStatus.Shared, AddressType.I32)
        LIMIT_NO_MAX_UNSHARED_I64 -> Triple(false, SharedStatus.Unshared, AddressType.I64)
        LIMIT_MAX_UNSHARED_I64 -> Triple(true, SharedStatus.Unshared, AddressType.I64)
        LIMIT_NO_MAX_SHARED_I64 -> Err(TypeDecodeError.UnboundedSharedLimits).bind()
        LIMIT_MAX_SHARED_I64 -> Triple(true, SharedStatus.Shared, AddressType.I64)
        else -> Err(TypeDecodeError.UnknownLimitsFlag(byte)).bind()
    }

    val scalarReader: (WasmBinaryReader) -> ULong = when (addressType) {
        AddressType.I32 -> { reader ->
            reader.uint().toULong()
        }
        AddressType.I64 -> { reader ->
            reader.ulong()
        }
    }

    val minimum = scalarReader(context.reader)
    if (hasMaximum) {
        val limits = Limits(minimum, scalarReader(context.reader))
        Triple(limits, sharedStatus, addressType)
    } else {
        Triple(Limits(minimum), sharedStatus, addressType)
    }
}

internal const val LIMIT_NO_MAX_UNSHARED_I32: UByte = 0u
internal const val LIMIT_MAX_UNSHARED_I32: UByte = 1u
internal const val LIMIT_NO_MAX_SHARED_I32: UByte = 2u // Shared memory with no max is not permitted
internal const val LIMIT_MAX_SHARED_I32: UByte = 3u
internal const val LIMIT_NO_MAX_UNSHARED_I64: UByte = 4u
internal const val LIMIT_MAX_UNSHARED_I64: UByte = 5u
internal const val LIMIT_NO_MAX_SHARED_I64: UByte = 6u // Shared memory with no max is not permitted
internal const val LIMIT_MAX_SHARED_I64: UByte = 7u
