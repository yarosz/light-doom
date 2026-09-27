package io.github.charlietap.chasm.validator.context.component

import io.github.charlietap.chasm.type.component.ComponentResourceTypeId
import io.github.charlietap.chasm.validator.type.component.ComponentLabel

internal class ComponentNameContext(
    val resourceNamesVisible: Boolean = true,
) {

    val strongNames = mutableSetOf<String>()
    val resourceNames = mutableMapOf<ComponentResourceTypeId, ComponentLabel>()
    val resourcesByName = mutableMapOf<ComponentLabel, ComponentResourceTypeId>()

    fun clear() {
        strongNames.clear()
        resourceNames.clear()
        resourcesByName.clear()
    }
}
