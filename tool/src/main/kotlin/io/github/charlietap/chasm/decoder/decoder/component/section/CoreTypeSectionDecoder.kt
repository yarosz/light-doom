package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreType
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.type.CoreTypeDefinitionDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CoreTypeSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = CoreTypeSectionDecoder(
    context = context,
    typeDecoder = ::CoreTypeDefinitionDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun CoreTypeSectionDecoder(
    context: ComponentDecoderContext,
    noinline typeDecoder: ComponentDecoder<CoreTypeDefinition>,
    crossinline vectorDecoder: ComponentVectorDecoder<CoreTypeDefinition>,
): Result<ComponentSection, WasmDecodeError> = binding {
    val definitions = vectorDecoder(context, typeDecoder).bind().vector.map(::CoreType)
    ComponentSection.Definitions(definitions)
}
