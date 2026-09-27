package io.github.charlietap.chasm.decoder.context

import io.github.charlietap.chasm.config.ComponentConfig
import io.github.charlietap.chasm.decoder.component.section.ComponentSectionType
import io.github.charlietap.chasm.decoder.reader.WasmBinaryReader
import io.github.charlietap.chasm.decoder.section.SectionSize

internal class ComponentDecoderContext(
    val config: ComponentConfig,
    reader: WasmBinaryReader,
    val moduleContext: ModuleDecoderContext = ModuleDecoderContext(
        config = config.moduleConfig,
        reader = reader,
    ),
    var sectionSize: SectionSize = SectionSize(0u),
    var sectionType: ComponentSectionType = ComponentSectionType.Custom,
) : ReaderContext {

    override var reader: WasmBinaryReader
        get() = moduleContext.reader
        set(value) {
            moduleContext.reader = value
        }
}
