package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Instance
import io.github.charlietap.chasm.ast.component.InstanceDefinition
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.instance.InstanceDefinitionDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun InstanceSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = InstanceSectionDecoder(
    context = context,
    instanceDecoder = ::InstanceDefinitionDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun InstanceSectionDecoder(
    context: ComponentDecoderContext,
    noinline instanceDecoder: ComponentDecoder<InstanceDefinition>,
    crossinline vectorDecoder: ComponentVectorDecoder<InstanceDefinition>,
): Result<ComponentSection, WasmDecodeError> = binding {
    val definitions = vectorDecoder(context, instanceDecoder).bind().vector.map(::Instance)
    ComponentSection.Definitions(definitions)
}
