package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.LabeledValueType
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentLabeledValueTypeDecoder(
    context: ComponentDecoderContext,
): Result<LabeledValueType, WasmDecodeError> = ComponentLabeledValueTypeDecoder(
    context = context,
    labelDecoder = ::ComponentLabelDecoder,
    valueTypeDecoder = ::ComponentValueTypeDecoder,
)

internal inline fun ComponentLabeledValueTypeDecoder(
    context: ComponentDecoderContext,
    crossinline labelDecoder: ComponentDecoder<NameValue>,
    crossinline valueTypeDecoder: ComponentDecoder<ValueType>,
): Result<LabeledValueType, WasmDecodeError> = binding {
    LabeledValueType(
        label = labelDecoder(context).bind(),
        type = valueTypeDecoder(context).bind(),
    )
}
