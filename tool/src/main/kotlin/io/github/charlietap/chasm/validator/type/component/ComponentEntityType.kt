package io.github.charlietap.chasm.validator.type.component

import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentTypeDefinition
import io.github.charlietap.chasm.type.component.ComponentTypeId
import io.github.charlietap.chasm.type.component.ComponentValueType

internal typealias ComponentTypeInfoLookup = (ComponentTypeId) -> ComponentTypeInfo?

internal data class ComponentTypeInfo(
    val effectiveSize: Int,
    val containsBorrow: Boolean,
    val nestingDepth: Int,
)

internal data class ComponentTypeEntry(
    val definition: ComponentTypeDefinition,
    val info: ComponentTypeInfo,
) {
    val id get() = definition.id
    val type get() = definition.type
}

internal data class ComponentValueEntry(
    val type: ComponentValueType,
    var consumed: Boolean = false,
)
