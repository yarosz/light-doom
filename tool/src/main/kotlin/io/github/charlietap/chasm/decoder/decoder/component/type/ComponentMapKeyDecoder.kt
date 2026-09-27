package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.KeyType
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentMapKeyDecoder(
    context: ComponentDecoderContext,
): Result<KeyType, WasmDecodeError> = ComponentMapKeyDecoder(
    context = context,
    valueTypeDecoder = ::ComponentValueTypeDecoder,
)

internal inline fun ComponentMapKeyDecoder(
    context: ComponentDecoderContext,
    crossinline valueTypeDecoder: ComponentDecoder<ValueType>,
): Result<KeyType, WasmDecodeError> = binding {
    when (val type = valueTypeDecoder(context).bind()) {
        ValueType.Bool -> KeyType.Bool
        ValueType.S8 -> KeyType.S8
        ValueType.U8 -> KeyType.U8
        ValueType.S16 -> KeyType.S16
        ValueType.U16 -> KeyType.U16
        ValueType.S32 -> KeyType.S32
        ValueType.U32 -> KeyType.U32
        ValueType.S64 -> KeyType.S64
        ValueType.U64 -> KeyType.U64
        ValueType.Char -> KeyType.Char
        ValueType.String -> KeyType.String
        else -> Err(ComponentTypeDecodeError.InvalidMapKey(type.toString())).bind<KeyType>()
    }
}
