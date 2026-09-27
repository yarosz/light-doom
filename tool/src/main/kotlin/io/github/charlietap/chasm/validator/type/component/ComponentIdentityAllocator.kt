package io.github.charlietap.chasm.validator.type.component

import io.github.charlietap.chasm.type.component.ComponentResourceTypeId
import io.github.charlietap.chasm.type.component.ComponentTypeId

internal class ComponentIdentityAllocator {

    private var nextResourceId = 0u
    private var nextTypeId = 0u

    fun resourceId(): ComponentResourceTypeId = ComponentResourceTypeId(nextResourceId++)

    fun typeId(): ComponentTypeId = ComponentTypeId(nextTypeId++)

    fun clear() {
        nextResourceId = 0u
        nextTypeId = 0u
    }
}

internal fun ComponentTypeEntry.alias(
    identities: ComponentIdentityAllocator,
): ComponentTypeEntry = copy(definition = definition.copy(id = identities.typeId()))
