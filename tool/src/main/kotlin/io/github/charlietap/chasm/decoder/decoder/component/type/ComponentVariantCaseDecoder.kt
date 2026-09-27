package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.ast.component.VariantCase
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.ComponentOptionalDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentVariantCaseDecoder(
    context: ComponentDecoderContext,
): Result<VariantCase, WasmDecodeError> = ComponentVariantCaseDecoder(
    context = context,
    labelDecoder = ::ComponentLabelDecoder,
    optionalValueTypeDecoder = { scopedContext ->
        ComponentOptionalDecoder(scopedContext, ::ComponentValueTypeDecoder)
    },
)

internal inline fun ComponentVariantCaseDecoder(
    context: ComponentDecoderContext,
    crossinline labelDecoder: ComponentDecoder<NameValue>,
    crossinline optionalValueTypeDecoder: ComponentDecoder<ValueType?>,
): Result<VariantCase, WasmDecodeError> = binding {
    val label = labelDecoder(context).bind()
    val type = optionalValueTypeDecoder(context).bind()
    val reserved = context.reader.ubyte()
    if (reserved != VARIANT_CASE_RESERVED) {
        Err(
            ComponentTypeDecodeError.InvalidReservedByte(
                expected = VARIANT_CASE_RESERVED,
                actual = reserved,
            ),
        ).bind<Unit>()
    }

    VariantCase(label, type)
}

private const val VARIANT_CASE_RESERVED: UByte = 0x00u
