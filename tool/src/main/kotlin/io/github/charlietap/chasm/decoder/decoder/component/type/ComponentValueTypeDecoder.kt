package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentValueTypeDecoder(
    context: ComponentDecoderContext,
): Result<ValueType, WasmDecodeError> = ComponentValueTypeDecoder(
    context = context,
    primitiveValueTypeDecoder = ::ComponentPrimitiveValueTypeDecoder,
)

internal inline fun ComponentValueTypeDecoder(
    context: ComponentDecoderContext,
    crossinline primitiveValueTypeDecoder: ComponentDecoder<ValueType>,
): Result<ValueType, WasmDecodeError> = binding {
    val firstByte = context.reader.peekUByte()
    when {
        firstByte == TYPE_ERROR_CONTEXT || firstByte in TYPE_STRING..TYPE_BOOL -> {
            primitiveValueTypeDecoder(context).bind()
        }
        firstByte in NEGATIVE_ONE_BYTE_MIN..NEGATIVE_ONE_BYTE_MAX -> {
            Err(ComponentTypeDecodeError.UnknownPrimitiveValueType(firstByte)).bind<ValueType>()
        }
        else -> {
            val index = context.reader.s33()
            if (index < 0 || index > UInt.MAX_VALUE.toLong()) {
                Err(ComponentTypeDecodeError.UnknownPrimitiveValueType(firstByte)).bind<Unit>()
            }
            ValueType.TypeIndex(ComponentTypeIndex(index.toUInt()))
        }
    }
}

private const val TYPE_ERROR_CONTEXT: UByte = 0x64u
private const val TYPE_STRING: UByte = 0x73u
private const val TYPE_BOOL: UByte = 0x7Fu
private const val NEGATIVE_ONE_BYTE_MIN: UByte = 0x40u
private const val NEGATIVE_ONE_BYTE_MAX: UByte = 0x7Fu
