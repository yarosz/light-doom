package io.github.charlietap.chasm.decoder.context.scope

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.decoder.component.section.ComponentSectionType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.decoder.section.SectionSize

internal inline fun <T> ComponentSectionScope(
    context: ComponentDecoderContext,
    sectionDetails: Pair<SectionSize, ComponentSectionType>,
    crossinline decoder: ComponentDecoder<T>,
): Result<T, WasmDecodeError> {
    val (sectionSize, sectionType) = sectionDetails
    val previousSectionSize = context.sectionSize
    val previousSectionType = context.sectionType

    context.sectionSize = sectionSize
    context.sectionType = sectionType

    return try {
        ReaderByteScope(context, sectionSize.size, decoder)
    } finally {
        context.sectionSize = previousSectionSize
        context.sectionType = previousSectionType
    }
}
