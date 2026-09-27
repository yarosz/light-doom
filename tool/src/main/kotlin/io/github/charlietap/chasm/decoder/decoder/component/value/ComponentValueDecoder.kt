package io.github.charlietap.chasm.decoder.decoder.component.value

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ComponentValue
import io.github.charlietap.chasm.ast.component.ComponentValueLiteral
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.context.scope.ReaderByteScope
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.type.ComponentValueTypeDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentValueDecoder(
    context: ComponentDecoderContext,
): Result<ComponentValue, WasmDecodeError> = ComponentValueDecoder(
    context = context,
    valueTypeDecoder = ::ComponentValueTypeDecoder,
    valueLiteralDecoder = ::ComponentValueLiteralDecoder,
)

internal inline fun ComponentValueDecoder(
    context: ComponentDecoderContext,
    crossinline valueTypeDecoder: ComponentDecoder<ValueType>,
    crossinline valueLiteralDecoder: (
        ComponentDecoderContext,
        ValueType,
        UInt,
    ) -> Result<ComponentValueLiteral, WasmDecodeError>,
): Result<ComponentValue, WasmDecodeError> = binding {
    val type = valueTypeDecoder(context).bind()
    val payloadSize = context.reader.uint()
    val value = ReaderByteScope(context, payloadSize) { scopedContext ->
        valueLiteralDecoder(scopedContext, type, payloadSize)
    }.bind()

    ComponentValue(type, value)
}
