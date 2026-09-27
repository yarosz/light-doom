package io.github.charlietap.chasm.decoder.builder

import io.github.charlietap.chasm.ast.component.Component
import io.github.charlietap.chasm.ast.component.Custom
import io.github.charlietap.chasm.ast.component.Definition
import io.github.charlietap.chasm.ast.component.Version
import io.github.charlietap.chasm.decoder.component.section.ComponentSection

internal class ComponentBuilder(private val version: Version) {

    private val definitions: MutableList<Definition> = mutableListOf()
    private val customs: MutableList<Custom> = mutableListOf()

    fun section(section: ComponentSection) = apply {
        when (section) {
            is ComponentSection.Definitions -> definitions += section.definitions
            is ComponentSection.Custom -> customs += section.custom
        }
    }

    fun build() = Component(
        version = version,
        definitions = definitions,
        customs = customs,
    )
}
