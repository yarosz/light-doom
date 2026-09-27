package io.github.charlietap.chasm.validator.validator.component.linking.type

import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.ComponentInstanceType
import io.github.charlietap.chasm.type.component.ComponentType
import io.github.charlietap.chasm.type.component.ComponentValueType

internal val ComponentEntityType.sort: String
    get() = when (this) {
        is ComponentEntityType.CoreModule -> "core module"
        is ComponentEntityType.Function -> "function"
        is ComponentEntityType.Value -> "value"
        is ComponentEntityType.Type -> "type"
        is ComponentEntityType.Component -> "component"
        is ComponentEntityType.Instance -> "instance"
    }

internal fun <T> List<T>.componentIndex(index: UInt): T? {
    if (index > Int.MAX_VALUE.toUInt()) return null
    return getOrNull(index.toInt())
}

internal fun ComponentEntityType.containsResource(): Boolean = when (this) {
    is ComponentEntityType.CoreModule -> false
    is ComponentEntityType.Function -> type.containsResource()
    is ComponentEntityType.Value -> type.containsResource()
    is ComponentEntityType.Type -> referenced.type.containsResource()
    is ComponentEntityType.Component -> type.containsResource()
    is ComponentEntityType.Instance -> type.containsResource()
}

private fun ComponentDefinedType.containsResource(): Boolean = when (this) {
    is ComponentDefinedType.Value -> type.containsResource()
    is ComponentDefinedType.Function -> type.containsResource()
    is ComponentDefinedType.Component -> type.containsResource()
    is ComponentDefinedType.Instance -> type.containsResource()
    is ComponentDefinedType.Resource -> true
}

private fun ComponentFunctionType.containsResource(): Boolean =
    params.any { parameter -> parameter.type.containsResource() } || result?.containsResource() == true

private fun ComponentType.containsResource(): Boolean =
    importedResources.isNotEmpty() ||
        definedResources.isNotEmpty() ||
        imports.values.any(ComponentEntityType::containsResource) ||
        exports.values.any(ComponentEntityType::containsResource)

private fun ComponentInstanceType.containsResource(): Boolean =
    definedResources.isNotEmpty() || exports.values.any(ComponentEntityType::containsResource)

private fun ComponentValueType.containsResource(): Boolean = when (this) {
    is ComponentValueType.Primitive -> false
    is ComponentValueType.Defined -> definition.type.containsResource()
}

private fun ComponentDefinedValueType.containsResource(): Boolean = when (this) {
    is ComponentDefinedValueType.Primitive,
    is ComponentDefinedValueType.Flags,
    is ComponentDefinedValueType.Enum,
    -> false
    is ComponentDefinedValueType.Record -> fields.any { field -> field.type.containsResource() }
    is ComponentDefinedValueType.Variant -> cases.any { case -> case.type?.containsResource() == true }
    is ComponentDefinedValueType.ListValue -> element.containsResource()
    is ComponentDefinedValueType.FixedLengthList -> element.containsResource()
    is ComponentDefinedValueType.Map -> value.containsResource()
    is ComponentDefinedValueType.Tuple -> elements.any(ComponentValueType::containsResource)
    is ComponentDefinedValueType.Option -> value.containsResource()
    is ComponentDefinedValueType.Result -> ok?.containsResource() == true || error?.containsResource() == true
    is ComponentDefinedValueType.Own,
    is ComponentDefinedValueType.Borrow,
    -> true
    is ComponentDefinedValueType.Stream -> element?.containsResource() == true
    is ComponentDefinedValueType.Future -> value?.containsResource() == true
}
