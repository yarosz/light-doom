package io.github.charlietap.chasm.decoder.decoder.section.datacount

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.decoder.section.DataCountSection

internal fun DataCountSectionDecoder(
    context: ModuleDecoderContext,
): Result<DataCountSection, WasmDecodeError> = binding {
    DataCountSection(context.reader.uint())
}
