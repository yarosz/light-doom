package io.github.charlietap.chasm.decoder.context.scope

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.decoder.section.SectionSize
import io.github.charlietap.chasm.decoder.section.SectionType

internal inline fun <T> SectionScope(
    context: ModuleDecoderContext,
    sectionDetails: Pair<SectionSize, SectionType>,
    crossinline decoder: Decoder<T>,
): Result<T, WasmDecodeError> {
    val (sectionSize, sectionType) = sectionDetails
    val previousSectionSize = context.sectionSize
    val previousSectionType = context.sectionType

    context.sectionSize = sectionSize
    context.sectionType = sectionType

    return try {
        decoder(context)
    } finally {
        context.sectionSize = previousSectionSize
        context.sectionType = previousSectionType
    }
}
