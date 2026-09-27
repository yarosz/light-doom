package io.github.charlietap.chasm.decoder.decoder.section

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.decoder.ext.sectionType
import io.github.charlietap.chasm.decoder.section.SectionType

internal fun SectionTypeDecoder(
    context: ModuleDecoderContext,
): Result<SectionType, WasmDecodeError> = context.reader.ubyte().sectionType()
