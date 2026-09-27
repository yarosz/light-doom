package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreModule
import io.github.charlietap.chasm.ast.module.Module
import io.github.charlietap.chasm.decoder.WasmModuleDecoder
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CoreModuleSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = CoreModuleSectionDecoder(
    context = context,
    moduleDecoder = ::WasmModuleDecoder,
)

internal inline fun CoreModuleSectionDecoder(
    context: ComponentDecoderContext,
    crossinline moduleDecoder: Decoder<Module>,
): Result<ComponentSection, WasmDecodeError> = binding {
    val module = moduleDecoder(context.moduleContext).bind()
    ComponentSection.Definitions(listOf(CoreModule(module)))
}
