package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Custom
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.component.section.ComponentSectionType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.custom.ComponentCustomSectionDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = ComponentSectionDecoder(
    context = context,
    customSectionDecoder = ::ComponentCustomSectionDecoder,
    coreModuleSectionDecoder = ::CoreModuleSectionDecoder,
    coreInstanceSectionDecoder = ::CoreInstanceSectionDecoder,
    coreTypeSectionDecoder = ::CoreTypeSectionDecoder,
    nestedComponentSectionDecoder = ::NestedComponentSectionDecoder,
    instanceSectionDecoder = ::InstanceSectionDecoder,
    aliasSectionDecoder = ::AliasSectionDecoder,
    typeSectionDecoder = ::TypeSectionDecoder,
    canonicalSectionDecoder = ::CanonicalSectionDecoder,
    startSectionDecoder = ::StartSectionDecoder,
    importSectionDecoder = ::ImportSectionDecoder,
    exportSectionDecoder = ::ExportSectionDecoder,
    valueSectionDecoder = ::ValueSectionDecoder,
)

@Suppress("LongParameterList")
internal inline fun ComponentSectionDecoder(
    context: ComponentDecoderContext,
    crossinline customSectionDecoder: ComponentDecoder<Custom>,
    crossinline coreModuleSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline coreInstanceSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline coreTypeSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline nestedComponentSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline instanceSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline aliasSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline typeSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline canonicalSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline startSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline importSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline exportSectionDecoder: ComponentDecoder<ComponentSection>,
    crossinline valueSectionDecoder: ComponentDecoder<ComponentSection>,
): Result<ComponentSection, WasmDecodeError> = binding {
    when (context.sectionType) {
        ComponentSectionType.Custom -> ComponentSection.Custom(customSectionDecoder(context).bind())
        ComponentSectionType.CoreModule -> coreModuleSectionDecoder(context).bind()
        ComponentSectionType.CoreInstance -> coreInstanceSectionDecoder(context).bind()
        ComponentSectionType.CoreType -> coreTypeSectionDecoder(context).bind()
        ComponentSectionType.Component -> nestedComponentSectionDecoder(context).bind()
        ComponentSectionType.Instance -> instanceSectionDecoder(context).bind()
        ComponentSectionType.Alias -> aliasSectionDecoder(context).bind()
        ComponentSectionType.Type -> typeSectionDecoder(context).bind()
        ComponentSectionType.Canonical -> canonicalSectionDecoder(context).bind()
        ComponentSectionType.Start -> startSectionDecoder(context).bind()
        ComponentSectionType.Import -> importSectionDecoder(context).bind()
        ComponentSectionType.Export -> exportSectionDecoder(context).bind()
        ComponentSectionType.Value -> valueSectionDecoder(context).bind()
    }
}
