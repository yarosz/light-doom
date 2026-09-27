package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentResultListDecoder(
    context: ComponentDecoderContext,
): Result<ValueType?, WasmDecodeError> = ComponentResultListDecoder(
    context = context,
    valueTypeDecoder = ::ComponentValueTypeDecoder,
)

internal inline fun ComponentResultListDecoder(
    context: ComponentDecoderContext,
    crossinline valueTypeDecoder: ComponentDecoder<ValueType>,
): Result<ValueType?, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        RESULT_ONE -> valueTypeDecoder(context).bind()
        RESULT_NONE -> {
            val reserved = context.reader.ubyte()
            if (reserved != RESULT_NONE_RESERVED) {
                Err(ComponentTypeDecodeError.InvalidReservedByte(RESULT_NONE_RESERVED, reserved)).bind<Unit>()
            }
            null
        }
        else -> Err(ComponentTypeDecodeError.InvalidResultList(opcode)).bind<ValueType?>()
    }
}

private const val RESULT_ONE: UByte = 0x00u
private const val RESULT_NONE: UByte = 0x01u
private const val RESULT_NONE_RESERVED: UByte = 0x00u
