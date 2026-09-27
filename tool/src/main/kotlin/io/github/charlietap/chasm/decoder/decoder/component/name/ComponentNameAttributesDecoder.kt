package io.github.charlietap.chasm.decoder.decoder.component.name

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExternalAttribute
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentNameAttributesDecoder(
    context: ComponentDecoderContext,
): Result<NameAttributes, WasmDecodeError> = ComponentNameAttributesDecoder(
    context = context,
    nameDecoder = ::NameValueDecoder,
    attributeDecoder = ::ExternalAttributeDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun ComponentNameAttributesDecoder(
    context: ComponentDecoderContext,
    crossinline nameDecoder: ReaderDecoder<NameValue>,
    noinline attributeDecoder: ComponentDecoder<ExternalAttribute>,
    crossinline vectorDecoder: ComponentVectorDecoder<ExternalAttribute>,
): Result<NameAttributes, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        NAME_ATTRIBUTE_LEGACY,
        NAME_ATTRIBUTE_CURRENT,
        -> NameAttributes(nameDecoder(context).bind())
        NAME_ATTRIBUTE_WITH_ATTRIBUTES -> NameAttributes(
            name = nameDecoder(context).bind(),
            attributes = vectorDecoder(context, attributeDecoder).bind().vector,
        )
        else -> Err(ComponentDecodeError.UnknownNameAttribute(opcode)).bind<NameAttributes>()
    }
}

private const val NAME_ATTRIBUTE_LEGACY: UByte = 0x00u
private const val NAME_ATTRIBUTE_CURRENT: UByte = 0x01u
private const val NAME_ATTRIBUTE_WITH_ATTRIBUTES: UByte = 0x02u
