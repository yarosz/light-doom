package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Export
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.ComponentExportDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ExportSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = ExportSectionDecoder(
    context = context,
    exportDecoder = ::ComponentExportDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun ExportSectionDecoder(
    context: ComponentDecoderContext,
    noinline exportDecoder: ComponentDecoder<Export>,
    crossinline vectorDecoder: ComponentVectorDecoder<Export>,
): Result<ComponentSection, WasmDecodeError> = binding {
    ComponentSection.Definitions(vectorDecoder(context, exportDecoder).bind().vector)
}
