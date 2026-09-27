package io.github.charlietap.chasm.decoder.decoder.component.name

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExternalAttribute
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ExternalAttributeDecoder(
    context: ComponentDecoderContext,
): Result<ExternalAttribute, WasmDecodeError> = ExternalAttributeDecoder(
    context = context,
    nameDecoder = ::NameValueDecoder,
)

internal inline fun ExternalAttributeDecoder(
    context: ComponentDecoderContext,
    crossinline nameDecoder: ReaderDecoder<NameValue>,
): Result<ExternalAttribute, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        ATTRIBUTE_IMPLEMENTS -> ExternalAttribute.Implements(nameDecoder(context).bind().name)
        ATTRIBUTE_VERSION_SUFFIX -> ExternalAttribute.VersionSuffix(nameDecoder(context).bind().name)
        ATTRIBUTE_EXTERNAL_ID -> ExternalAttribute.ExternalId(nameDecoder(context).bind())
        else -> Err(ComponentDecodeError.UnknownExternalAttribute(opcode)).bind<ExternalAttribute>()
    }
}

private const val ATTRIBUTE_IMPLEMENTS: UByte = 0x00u
private const val ATTRIBUTE_VERSION_SUFFIX: UByte = 0x01u
private const val ATTRIBUTE_EXTERNAL_ID: UByte = 0x02u
