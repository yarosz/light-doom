package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Index.ComponentValueIndex
import io.github.charlietap.chasm.ast.component.ValueBound
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentValueIndexDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentValueBoundDecoder(
    context: ComponentDecoderContext,
): Result<ValueBound, WasmDecodeError> = ComponentValueBoundDecoder(
    context = context,
    valueIndexDecoder = ::ComponentValueIndexDecoder,
    valueTypeDecoder = ::ComponentValueTypeDecoder,
)

internal inline fun ComponentValueBoundDecoder(
    context: ComponentDecoderContext,
    crossinline valueIndexDecoder: ComponentDecoder<ComponentValueIndex>,
    crossinline valueTypeDecoder: ComponentDecoder<ValueType>,
): Result<ValueBound, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        VALUE_BOUND_EQUALS -> ValueBound.Equals(valueIndexDecoder(context).bind())
        VALUE_BOUND_TYPE -> ValueBound.Type(valueTypeDecoder(context).bind())
        else -> Err(ComponentTypeDecodeError.UnknownValueBound(opcode)).bind<ValueBound>()
    }
}

private const val VALUE_BOUND_EQUALS: UByte = 0x00u
private const val VALUE_BOUND_TYPE: UByte = 0x01u
