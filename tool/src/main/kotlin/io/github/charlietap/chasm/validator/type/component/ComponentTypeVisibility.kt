package io.github.charlietap.chasm.validator.type.component

import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.ComponentTypeDefinition
import io.github.charlietap.chasm.type.component.ComponentTypeId
import io.github.charlietap.chasm.type.component.ComponentValueType

internal class ComponentTypeVisibility {

    internal val importedTypes = linkedSetOf<ComponentTypeId>()
    internal val exportedTypes = linkedSetOf<ComponentTypeId>()

    constructor()

    constructor(entities: Iterable<ComponentEntityType>) {
        entities.forEach { entity -> entity.isExternallyVisible(this, validate = false) }
    }

    fun clear() {
        importedTypes.clear()
        exportedTypes.clear()
    }
}

internal data class ComponentTypeVisibilitySummary(
    val requiredTypeIds: Set<ComponentTypeId> = emptySet(),
    val exportedTypeIds: Set<ComponentTypeId> = emptySet(),
)

internal enum class ComponentTypeVisibilityMode {
    Import,
    Export,
}

internal fun componentTypeVisibilitySummary(
    entities: Iterable<ComponentEntityType>,
): ComponentTypeVisibilitySummary {
    val requiredTypeIds = linkedSetOf<ComponentTypeId>()
    val exportedTypeIds = linkedSetOf<ComponentTypeId>()
    entities.forEach { entity ->
        val visibility = entity.visibilitySummary()
        requiredTypeIds += visibility.requiredTypeIds - exportedTypeIds
        exportedTypeIds += visibility.exportedTypeIds
    }
    return ComponentTypeVisibilitySummary(requiredTypeIds, exportedTypeIds)
}

internal fun ComponentTypeVisibilitySummary.substituteTypes(
    substitutions: Map<ComponentTypeId, ComponentTypeId>,
): ComponentTypeVisibilitySummary {
    if (substitutions.isEmpty()) return this
    return copy(
        requiredTypeIds = requiredTypeIds.mapTo(linkedSetOf()) { id -> substitutions[id] ?: id },
        exportedTypeIds = exportedTypeIds.mapTo(linkedSetOf()) { id -> substitutions[id] ?: id },
    )
}

internal fun ComponentEntityType.isExternallyVisible(
    visibility: ComponentTypeVisibility,
    mode: ComponentTypeVisibilityMode = ComponentTypeVisibilityMode.Export,
    validate: Boolean = true,
    typeInfo: ComponentTypeInfoLookup = EmptyComponentTypeInfoLookup,
): Boolean {
    if (validate && this is ComponentEntityType.Value && type.containsBorrow(typeInfo)) return false

    val summary = visibilitySummary()
    val visibleTypes = when (mode) {
        ComponentTypeVisibilityMode.Import -> visibility.importedTypes
        ComponentTypeVisibilityMode.Export -> visibility.exportedTypes
    }
    if (validate && !visibleTypes.containsAll(summary.requiredTypeIds)) return false

    visibleTypes += summary.exportedTypeIds
    if (mode == ComponentTypeVisibilityMode.Import) visibility.exportedTypes += summary.exportedTypeIds
    return true
}

private fun ComponentEntityType.visibilitySummary(): ComponentTypeVisibilitySummary = when (this) {
    is ComponentEntityType.CoreModule -> ComponentTypeVisibilitySummary()
    is ComponentEntityType.Function -> type.visibilitySummary()
    is ComponentEntityType.Value -> type.visibilitySummary()
    is ComponentEntityType.Type -> referenced.type.referenceVisibilitySummary().copy(
        exportedTypeIds = setOf(createdId),
    )
    is ComponentEntityType.Component -> ComponentTypeVisibilitySummary()
    is ComponentEntityType.Instance -> componentTypeVisibilitySummary(type.exports.values)
}

private fun ComponentDefinedType.referenceVisibilitySummary(): ComponentTypeVisibilitySummary = when (this) {
    is ComponentDefinedType.Value -> type.referenceVisibilitySummary()
    is ComponentDefinedType.Function -> type.visibilitySummary()
    is ComponentDefinedType.Component -> ComponentTypeVisibilitySummary()
    is ComponentDefinedType.Instance -> componentTypeVisibilitySummary(type.exports.values)
    is ComponentDefinedType.Resource -> ComponentTypeVisibilitySummary()
}

private fun ComponentFunctionType.visibilitySummary(): ComponentTypeVisibilitySummary {
    val required = linkedSetOf<ComponentTypeId>()
    params.forEach { parameter -> required += parameter.type.visibilitySummary().requiredTypeIds }
    result?.let { type -> required += type.visibilitySummary().requiredTypeIds }
    return ComponentTypeVisibilitySummary(requiredTypeIds = required)
}

private fun ComponentValueType.visibilitySummary(): ComponentTypeVisibilitySummary = when (this) {
    is ComponentValueType.Primitive -> ComponentTypeVisibilitySummary()
    is ComponentValueType.Defined -> definition.namedVisibilitySummary()
}

private fun ComponentTypeDefinition.namedVisibilitySummary(): ComponentTypeVisibilitySummary {
    val value = (type as? ComponentDefinedType.Value)?.type ?: return ComponentTypeVisibilitySummary()
    return when (value) {
        is ComponentDefinedValueType.Record,
        is ComponentDefinedValueType.Variant,
        is ComponentDefinedValueType.Flags,
        is ComponentDefinedValueType.Enum,
        -> ComponentTypeVisibilitySummary(requiredTypeIds = setOf(id))
        else -> value.visibilitySummary()
    }
}

private fun ComponentDefinedValueType.referenceVisibilitySummary(): ComponentTypeVisibilitySummary = when (this) {
    is ComponentDefinedValueType.Record -> fields.valueVisibilitySummary { field -> field.type }
    is ComponentDefinedValueType.Variant -> cases.valueVisibilitySummary { case -> case.type }
    is ComponentDefinedValueType.Flags,
    is ComponentDefinedValueType.Enum,
    -> ComponentTypeVisibilitySummary()
    else -> visibilitySummary()
}

private fun ComponentDefinedValueType.visibilitySummary(): ComponentTypeVisibilitySummary = when (this) {
    is ComponentDefinedValueType.Primitive,
    is ComponentDefinedValueType.Flags,
    is ComponentDefinedValueType.Enum,
    -> ComponentTypeVisibilitySummary()
    is ComponentDefinedValueType.Record -> fields.valueVisibilitySummary { field -> field.type }
    is ComponentDefinedValueType.Variant -> cases.valueVisibilitySummary { case -> case.type }
    is ComponentDefinedValueType.ListValue -> element.visibilitySummary()
    is ComponentDefinedValueType.FixedLengthList -> element.visibilitySummary()
    is ComponentDefinedValueType.Map -> value.visibilitySummary()
    is ComponentDefinedValueType.Tuple -> elements.valueVisibilitySummary { type -> type }
    is ComponentDefinedValueType.Option -> value.visibilitySummary()
    is ComponentDefinedValueType.Result -> {
        val required = linkedSetOf<ComponentTypeId>()
        ok?.let { type -> required += type.visibilitySummary().requiredTypeIds }
        error?.let { type -> required += type.visibilitySummary().requiredTypeIds }
        ComponentTypeVisibilitySummary(requiredTypeIds = required)
    }
    is ComponentDefinedValueType.Own -> ComponentTypeVisibilitySummary(requiredTypeIds = setOf(id))
    is ComponentDefinedValueType.Borrow -> ComponentTypeVisibilitySummary(requiredTypeIds = setOf(id))
    is ComponentDefinedValueType.Stream -> element?.visibilitySummary() ?: ComponentTypeVisibilitySummary()
    is ComponentDefinedValueType.Future -> value?.visibilitySummary() ?: ComponentTypeVisibilitySummary()
}

private inline fun <T> Iterable<T>.valueVisibilitySummary(
    type: (T) -> ComponentValueType?,
): ComponentTypeVisibilitySummary {
    val required = linkedSetOf<ComponentTypeId>()
    forEach { value -> type(value)?.let { componentType -> required += componentType.visibilitySummary().requiredTypeIds } }
    return ComponentTypeVisibilitySummary(requiredTypeIds = required)
}

private val EmptyComponentTypeInfoLookup: ComponentTypeInfoLookup = { null }
