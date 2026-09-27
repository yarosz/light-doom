package io.github.charlietap.chasm.decoder.decoder.type.tag

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.error.SectionDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.type.TagType

internal fun AttributeDecoder(
    context: ModuleDecoderContext,
): Result<TagType.Attribute, WasmDecodeError> = binding {
    when (val attribute = context.reader.ubyte()) {
        ATTRIBUTE_EXCEPTION -> TagType.Attribute.Exception
        else -> Err(SectionDecodeError.UnknownTagAttribute(attribute)).bind()
    }
}

internal const val ATTRIBUTE_EXCEPTION: UByte = 0x00u
