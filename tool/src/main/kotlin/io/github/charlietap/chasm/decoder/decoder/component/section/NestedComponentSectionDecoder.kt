package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Component
import io.github.charlietap.chasm.ast.component.NestedComponent
import io.github.charlietap.chasm.decoder.WasmComponentDecoder
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun NestedComponentSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = NestedComponentSectionDecoder(
    context = context,
    componentDecoder = ::WasmComponentDecoder,
)

internal inline fun NestedComponentSectionDecoder(
    context: ComponentDecoderContext,
    crossinline componentDecoder: ComponentDecoder<Component>,
): Result<ComponentSection, WasmDecodeError> = binding {
    val component = componentDecoder(context).bind()
    ComponentSection.Definitions(listOf(NestedComponent(component)))
}
