package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Start
import io.github.charlietap.chasm.ast.component.StartDefinition
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.ComponentStartDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun StartSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = StartSectionDecoder(
    context = context,
    startDecoder = ::ComponentStartDecoder,
)

internal inline fun StartSectionDecoder(
    context: ComponentDecoderContext,
    crossinline startDecoder: ComponentDecoder<StartDefinition>,
): Result<ComponentSection, WasmDecodeError> = binding {
    val start = startDecoder(context).bind()
    ComponentSection.Definitions(listOf(Start(start)))
}
