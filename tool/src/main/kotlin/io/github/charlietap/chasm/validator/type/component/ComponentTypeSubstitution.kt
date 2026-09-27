package io.github.charlietap.chasm.validator.type.component

import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.ComponentInstanceType
import io.github.charlietap.chasm.type.component.ComponentResourceTypeId
import io.github.charlietap.chasm.type.component.ComponentType
import io.github.charlietap.chasm.type.component.ComponentTypeDefinition
import io.github.charlietap.chasm.type.component.ComponentTypeId
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext

internal data class ComponentTypeSubstitution<T>(
    val type: T,
    val resources: Map<ComponentResourceTypeId, ComponentResourceTypeId>,
)

internal fun ComponentEntityTypesInstantiator(
    types: Map<String, ComponentEntityType>,
    remapping: ComponentRemapping,
): LinkedHashMap<String, ComponentEntityType> = ComponentTypeTransformation(remapping).transform(types)

internal fun ComponentType.freshenResources(
    identities: ComponentIdentityAllocator,
): ComponentTypeSubstitution<ComponentType> {
    if (definedResources.isEmpty()) return ComponentTypeSubstitution(this, emptyMap())

    val substitutions = linkedMapOf<ComponentResourceTypeId, ComponentResourceTypeId>()
    definedResources.keys.forEach { resource -> substitutions[resource] = identities.resourceId() }
    val remapping = ComponentRemapping().also { mapping -> mapping.resources.putAll(substitutions) }
    return ComponentTypeSubstitution(
        type = ComponentTypeTransformation(remapping).transform(this),
        resources = substitutions,
    )
}

internal fun ComponentInstanceType.freshenResources(
    identities: ComponentIdentityAllocator,
): ComponentTypeSubstitution<ComponentInstanceType> {
    if (definedResources.isEmpty()) return ComponentTypeSubstitution(this, emptyMap())

    val substitutions = linkedMapOf<ComponentResourceTypeId, ComponentResourceTypeId>()
    definedResources.forEach { resource -> substitutions[resource] = identities.resourceId() }
    val remapping = ComponentRemapping().also { mapping -> mapping.resources.putAll(substitutions) }
    val transformed = ComponentTypeTransformation(remapping).transform(this)
    return ComponentTypeSubstitution(
        type = transformed.copy(definedResources = emptySet()),
        resources = substitutions,
    )
}

internal fun ComponentTypeFreshener(
    context: ComponentValidationContext,
    type: ComponentType,
): ComponentTypeSubstitution<ComponentType> = type.freshenResources(context.identities)

internal fun ComponentInstanceTypeFreshener(
    context: ComponentValidationContext,
    type: ComponentInstanceType,
): ComponentTypeSubstitution<ComponentInstanceType> = type.freshenResources(context.identities)

private class ComponentTypeTransformation(
    private val remapping: ComponentRemapping,
) {

    fun transform(type: ComponentValueType): ComponentValueType = when (type) {
        is ComponentValueType.Primitive -> type
        is ComponentValueType.Defined -> {
            val definition = transform(type.definition)
            if (definition === type.definition) type else ComponentValueType.Defined(definition)
        }
    }

    private fun transform(type: ComponentDefinedValueType): ComponentDefinedValueType = when (type) {
        is ComponentDefinedValueType.Primitive -> type
        is ComponentDefinedValueType.Record -> type.copy(
            fields = type.fields.map { field -> field.copy(type = transform(field.type)) },
        )
        is ComponentDefinedValueType.Variant -> type.copy(
            cases = type.cases.map { case -> case.copy(type = case.type?.let(::transform)) },
        )
        is ComponentDefinedValueType.ListValue -> type.copy(element = transform(type.element))
        is ComponentDefinedValueType.FixedLengthList -> type.copy(element = transform(type.element))
        is ComponentDefinedValueType.Map -> type.copy(value = transform(type.value))
        is ComponentDefinedValueType.Tuple -> type.copy(elements = type.elements.map(::transform))
        is ComponentDefinedValueType.Flags -> type
        is ComponentDefinedValueType.Enum -> type
        is ComponentDefinedValueType.Option -> type.copy(value = transform(type.value))
        is ComponentDefinedValueType.Result -> type.copy(
            ok = type.ok?.let(::transform),
            error = type.error?.let(::transform),
        )
        is ComponentDefinedValueType.Own -> type.copy(
            id = typeId(type.id),
            resource = resourceId(type.resource),
        )
        is ComponentDefinedValueType.Borrow -> type.copy(
            id = typeId(type.id),
            resource = resourceId(type.resource),
        )
        is ComponentDefinedValueType.Stream -> type.copy(element = type.element?.let(::transform))
        is ComponentDefinedValueType.Future -> type.copy(value = type.value?.let(::transform))
    }

    private fun transform(type: ComponentFunctionType): ComponentFunctionType = type.copy(
        params = type.params.map { param -> param.copy(type = transform(param.type)) },
        result = type.result?.let(::transform),
    )

    private fun transform(type: ComponentDefinedType): ComponentDefinedType = when (type) {
        is ComponentDefinedType.Value -> type.copy(type = transform(type.type))
        is ComponentDefinedType.Function -> type.copy(type = transform(type.type))
        is ComponentDefinedType.Component -> type.copy(type = transform(type.type))
        is ComponentDefinedType.Instance -> type.copy(type = transform(type.type))
        is ComponentDefinedType.Resource -> type.copy(id = resourceId(type.id))
    }

    private fun transform(type: ComponentEntityType): ComponentEntityType = when (type) {
        is ComponentEntityType.CoreModule -> type
        is ComponentEntityType.Function -> type.copy(type = transform(type.type))
        is ComponentEntityType.Value -> type.copy(type = transform(type.type))
        is ComponentEntityType.Type -> type.copy(
            referenced = transform(type.referenced),
            createdId = typeId(type.createdId),
        )
        is ComponentEntityType.Component -> type.copy(type = transform(type.type))
        is ComponentEntityType.Instance -> type.copy(type = transform(type.type))
    }

    fun transform(type: ComponentType): ComponentType = type.copy(
        imports = transform(type.imports),
        exports = transform(type.exports),
        importedResources = transform(type.importedResources),
        definedResources = transform(type.definedResources),
        explicitResources = transform(type.explicitResources),
    )

    fun transform(type: ComponentInstanceType): ComponentInstanceType = type.copy(
        exports = transform(type.exports),
        definedResources = transform(type.definedResources),
        explicitResources = transform(type.explicitResources),
    )

    private fun transform(type: ComponentTypeDefinition): ComponentTypeDefinition {
        val id = typeId(type.id)
        val definition = transform(type.type)
        return if (id == type.id && definition === type.type) type else type.copy(id = id, type = definition)
    }

    fun transform(
        types: Map<String, ComponentEntityType>,
    ): LinkedHashMap<String, ComponentEntityType> = types.entries.associateTo(linkedMapOf()) { (name, type) ->
        name to transform(type)
    }

    private fun transform(resources: Set<ComponentResourceTypeId>): Set<ComponentResourceTypeId> {
        if (remapping.resources.isEmpty()) return resources
        return resources.mapTo(linkedSetOf(), ::resourceId)
    }

    private fun transform(resources: Map<ComponentResourceTypeId, List<String>>): Map<ComponentResourceTypeId, List<String>> {
        if (remapping.resources.isEmpty()) return resources
        return resources.entries.associateTo(linkedMapOf()) { (resource, path) ->
            resourceId(resource) to path
        }
    }

    private fun resourceId(id: ComponentResourceTypeId): ComponentResourceTypeId = remapping.resource(id)

    private fun typeId(id: ComponentTypeId): ComponentTypeId = remapping.types[id] ?: id
}
