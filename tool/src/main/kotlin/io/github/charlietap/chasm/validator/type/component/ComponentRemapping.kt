package io.github.charlietap.chasm.validator.type.component

import io.github.charlietap.chasm.type.component.ComponentResourceTypeId
import io.github.charlietap.chasm.type.component.ComponentTypeId

internal class ComponentRemapping {

    val resources = linkedMapOf<ComponentResourceTypeId, ComponentResourceTypeId>()
    val types = linkedMapOf<ComponentTypeId, ComponentTypeId>()

    fun addResource(
        expected: ComponentResourceTypeId,
        actual: ComponentResourceTypeId,
    ): Boolean {
        val previous = resources[expected]
        if (previous != null) return previous == actual
        resources[expected] = actual
        return true
    }

    fun addType(
        expected: ComponentTypeId,
        actual: ComponentTypeId,
    ): Boolean {
        val previous = types[expected]
        if (previous != null) return previous == actual
        types[expected] = actual
        return true
    }

    fun resource(id: ComponentResourceTypeId): ComponentResourceTypeId {
        var current = id
        repeat(resources.size) {
            val next = resources[current] ?: return current
            if (next == current) return current
            current = next
        }
        return current
    }

    fun clear() {
        resources.clear()
        types.clear()
    }
}
