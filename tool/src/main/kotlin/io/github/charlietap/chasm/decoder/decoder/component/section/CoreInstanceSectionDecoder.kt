package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreInstance
import io.github.charlietap.chasm.ast.component.CoreInstanceDefinition
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.instance.CoreInstanceDefinitionDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CoreInstanceSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = CoreInstanceSectionDecoder(
    context = context,
    instanceDecoder = ::CoreInstanceDefinitionDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun CoreInstanceSectionDecoder(
    context: ComponentDecoderContext,
    noinline instanceDecoder: ComponentDecoder<CoreInstanceDefinition>,
    crossinline vectorDecoder: ComponentVectorDecoder<CoreInstanceDefinition>,
): Result<ComponentSection, WasmDecodeError> = binding {
    val definitions = vectorDecoder(context, instanceDecoder).bind().vector.map(::CoreInstance)
    ComponentSection.Definitions(definitions)
}
