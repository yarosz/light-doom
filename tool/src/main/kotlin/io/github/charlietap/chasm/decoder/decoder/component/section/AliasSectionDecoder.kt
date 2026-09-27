package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Alias
import io.github.charlietap.chasm.ast.component.AliasDefinition
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.alias.AliasDefinitionDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun AliasSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = AliasSectionDecoder(
    context = context,
    aliasDecoder = ::AliasDefinitionDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun AliasSectionDecoder(
    context: ComponentDecoderContext,
    noinline aliasDecoder: ComponentDecoder<AliasDefinition>,
    crossinline vectorDecoder: ComponentVectorDecoder<AliasDefinition>,
): Result<ComponentSection, WasmDecodeError> = binding {
    val definitions = vectorDecoder(context, aliasDecoder).bind().vector.map(::Alias)
    ComponentSection.Definitions(definitions)
}
