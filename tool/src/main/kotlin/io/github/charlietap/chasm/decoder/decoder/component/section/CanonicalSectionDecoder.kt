package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Canon
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.canonical.CanonicalDefinitionDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CanonicalSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = CanonicalSectionDecoder(
    context = context,
    canonicalDefinitionDecoder = ::CanonicalDefinitionDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun CanonicalSectionDecoder(
    context: ComponentDecoderContext,
    noinline canonicalDefinitionDecoder: ComponentDecoder<CanonicalDefinition>,
    crossinline vectorDecoder: ComponentVectorDecoder<CanonicalDefinition>,
): Result<ComponentSection, WasmDecodeError> = binding {
    val definitions = vectorDecoder(context, canonicalDefinitionDecoder).bind().vector.map(::Canon)
    ComponentSection.Definitions(definitions)
}
