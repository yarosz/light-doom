package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Import
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.ComponentImportDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ImportSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = ImportSectionDecoder(
    context = context,
    importDecoder = ::ComponentImportDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun ImportSectionDecoder(
    context: ComponentDecoderContext,
    noinline importDecoder: ComponentDecoder<Import>,
    crossinline vectorDecoder: ComponentVectorDecoder<Import>,
): Result<ComponentSection, WasmDecodeError> = binding {
    ComponentSection.Definitions(vectorDecoder(context, importDecoder).bind().vector)
}
