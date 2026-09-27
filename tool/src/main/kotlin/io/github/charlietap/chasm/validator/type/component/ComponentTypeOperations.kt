package io.github.charlietap.chasm.validator.type.component

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.ComponentInstanceType
import io.github.charlietap.chasm.type.component.ComponentResourceTypeId
import io.github.charlietap.chasm.type.component.ComponentType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.type.component.CoreModuleType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.core.type.CoreModuleTypeSubtypeValidator

internal fun ComponentEntityTypeMatcher(
    context: ComponentValidationContext,
    actual: ComponentEntityType,
    expected: ComponentEntityType,
): Result<Unit, ComponentValidatorError> = ComponentEntityTypeMatcher(
    context = context,
    actual = actual,
    expected = expected,
    coreModuleTypeMatcher = ::CoreModuleTypeSubtypeValidator,
)

internal inline fun ComponentEntityTypeMatcher(
    context: ComponentValidationContext,
    actual: ComponentEntityType,
    expected: ComponentEntityType,
    crossinline coreModuleTypeMatcher: (
        ComponentValidationContext,
        CoreModuleType,
        CoreModuleType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = ComponentSubtypeValidator(
    context = context,
    actual = actual,
    expected = expected,
    coreModuleTypeMatcher = { matcherContext, actualType, expectedType ->
        coreModuleTypeMatcher(matcherContext, actualType, expectedType)
    },
)

internal fun ComponentImportsMatcher(
    context: ComponentValidationContext,
    arguments: Map<String, ComponentEntityType>,
    component: ComponentType,
    remapping: ComponentRemapping,
): Result<Unit, ComponentValidatorError> = ComponentImportsMatcher(
    context = context,
    arguments = arguments,
    component = component,
    remapping = remapping,
    coreModuleTypeMatcher = ::CoreModuleTypeSubtypeValidator,
)

internal inline fun ComponentImportsMatcher(
    context: ComponentValidationContext,
    arguments: Map<String, ComponentEntityType>,
    component: ComponentType,
    remapping: ComponentRemapping,
    crossinline coreModuleTypeMatcher: (
        ComponentValidationContext,
        CoreModuleType,
        CoreModuleType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = ComponentImportsSubtypeValidator(
    context = context,
    arguments = arguments,
    component = component,
    remapping = remapping,
    coreModuleTypeMatcher = { matcherContext, actualType, expectedType ->
        coreModuleTypeMatcher(matcherContext, actualType, expectedType)
    },
)

internal fun ComponentValueType.containsBorrow(typeInfo: ComponentTypeInfoLookup): Boolean = when (this) {
    is ComponentValueType.Primitive -> false
    is ComponentValueType.Defined ->
        typeInfo(definition.id)?.containsBorrow ?: definition.type.containsBorrow(typeInfo)
}

internal fun ComponentDefinedValueType.containsBorrow(typeInfo: ComponentTypeInfoLookup): Boolean = when (this) {
    is ComponentDefinedValueType.Borrow -> true
    is ComponentDefinedValueType.Record -> fields.any { field -> field.type.containsBorrow(typeInfo) }
    is ComponentDefinedValueType.Variant -> cases.any { case -> case.type?.containsBorrow(typeInfo) == true }
    is ComponentDefinedValueType.ListValue -> element.containsBorrow(typeInfo)
    is ComponentDefinedValueType.FixedLengthList -> element.containsBorrow(typeInfo)
    is ComponentDefinedValueType.Map -> value.containsBorrow(typeInfo)
    is ComponentDefinedValueType.Tuple -> elements.any { type -> type.containsBorrow(typeInfo) }
    is ComponentDefinedValueType.Option -> value.containsBorrow(typeInfo)
    is ComponentDefinedValueType.Result ->
        ok?.containsBorrow(typeInfo) == true || error?.containsBorrow(typeInfo) == true
    is ComponentDefinedValueType.Stream -> element?.containsBorrow(typeInfo) == true
    is ComponentDefinedValueType.Future -> value?.containsBorrow(typeInfo) == true
    is ComponentDefinedValueType.Primitive,
    is ComponentDefinedValueType.Flags,
    is ComponentDefinedValueType.Enum,
    is ComponentDefinedValueType.Own,
    -> false
}

internal fun ComponentFunctionType.containsBorrow(typeInfo: ComponentTypeInfoLookup): Boolean =
    params.any { param -> param.type.containsBorrow(typeInfo) } || result?.containsBorrow(typeInfo) == true

internal fun ComponentDefinedType.containsBorrow(typeInfo: ComponentTypeInfoLookup): Boolean = when (this) {
    is ComponentDefinedType.Value -> type.containsBorrow(typeInfo)
    is ComponentDefinedType.Function -> type.containsBorrow(typeInfo)
    is ComponentDefinedType.Component,
    is ComponentDefinedType.Instance,
    is ComponentDefinedType.Resource,
    -> false
}

internal fun ComponentValueType.resourceIds(): Set<ComponentResourceTypeId> = buildSet {
    collectResourceIds(this@resourceIds, this)
}

internal fun ComponentFunctionType.resourceIds(): Set<ComponentResourceTypeId> = buildSet {
    params.forEach { param -> collectResourceIds(param.type, this) }
    result?.let { type -> collectResourceIds(type, this) }
}

internal fun ComponentDefinedType.resourceIds(): Set<ComponentResourceTypeId> = buildSet {
    collectResourceIds(this@resourceIds, this)
}

internal fun ComponentEntityType.resourceIds(): Set<ComponentResourceTypeId> = buildSet {
    collectResourceIds(this@resourceIds, this)
}

internal fun ComponentType.freeResourceIds(): Set<ComponentResourceTypeId> = buildSet {
    imports.values.forEach { type -> collectResourceIds(type, this) }
    exports.values.forEach { type -> collectResourceIds(type, this) }
    removeAll(importedResources.keys)
    removeAll(definedResources.keys)
}

internal fun ComponentInstanceType.freeResourceIds(): Set<ComponentResourceTypeId> = buildSet {
    exports.values.forEach { type -> collectResourceIds(type, this) }
    removeAll(definedResources)
}

internal fun ComponentEntityType.sortName(): String = when (this) {
    is ComponentEntityType.CoreModule -> "core module"
    is ComponentEntityType.Function -> "function"
    is ComponentEntityType.Value -> "value"
    is ComponentEntityType.Type -> "type"
    is ComponentEntityType.Component -> "component"
    is ComponentEntityType.Instance -> "instance"
}

internal fun ComponentDefinedType.sortName(): String = when (this) {
    is ComponentDefinedType.Value -> "value type"
    is ComponentDefinedType.Function -> "function type"
    is ComponentDefinedType.Component -> "component type"
    is ComponentDefinedType.Instance -> "instance type"
    is ComponentDefinedType.Resource -> "resource type"
}

internal fun ComponentDefinedValueType.sortName(): String = when (this) {
    is ComponentDefinedValueType.Stream -> "stream type"
    is ComponentDefinedValueType.Future -> "future type"
    else -> "value type"
}

internal fun CoreEntityType.sortName(): String = when (this) {
    is CoreEntityType.Function -> "core function"
    is CoreEntityType.Table -> "core table"
    is CoreEntityType.Memory -> "core memory"
    is CoreEntityType.Global -> "core global"
    is CoreEntityType.Tag -> "core tag"
    is CoreEntityType.Type -> "core type"
    is CoreEntityType.Module -> "core module"
    is CoreEntityType.Instance -> "core instance"
}

private fun collectResourceIds(
    type: ComponentValueType,
    resources: MutableSet<ComponentResourceTypeId>,
) {
    if (type is ComponentValueType.Defined) collectResourceIds(type.definition.type, resources)
}

private fun collectResourceIds(
    type: ComponentDefinedValueType,
    resources: MutableSet<ComponentResourceTypeId>,
) {
    when (type) {
        is ComponentDefinedValueType.Own -> resources += type.resource
        is ComponentDefinedValueType.Borrow -> resources += type.resource
        is ComponentDefinedValueType.Record -> type.fields.forEach { field ->
            collectResourceIds(field.type, resources)
        }
        is ComponentDefinedValueType.Variant -> type.cases.forEach { case ->
            case.type?.let { caseType -> collectResourceIds(caseType, resources) }
        }
        is ComponentDefinedValueType.ListValue -> collectResourceIds(type.element, resources)
        is ComponentDefinedValueType.FixedLengthList -> collectResourceIds(type.element, resources)
        is ComponentDefinedValueType.Map -> collectResourceIds(type.value, resources)
        is ComponentDefinedValueType.Tuple -> type.elements.forEach { element ->
            collectResourceIds(element, resources)
        }
        is ComponentDefinedValueType.Option -> collectResourceIds(type.value, resources)
        is ComponentDefinedValueType.Result -> {
            type.ok?.let { ok -> collectResourceIds(ok, resources) }
            type.error?.let { error -> collectResourceIds(error, resources) }
        }
        is ComponentDefinedValueType.Stream -> type.element?.let { element ->
            collectResourceIds(element, resources)
        }
        is ComponentDefinedValueType.Future -> type.value?.let { value ->
            collectResourceIds(value, resources)
        }
        is ComponentDefinedValueType.Primitive,
        is ComponentDefinedValueType.Flags,
        is ComponentDefinedValueType.Enum,
        -> Unit
    }
}

private fun collectResourceIds(
    type: ComponentDefinedType,
    resources: MutableSet<ComponentResourceTypeId>,
) {
    when (type) {
        is ComponentDefinedType.Value -> collectResourceIds(type.type, resources)
        is ComponentDefinedType.Function -> {
            type.type.params.forEach { param -> collectResourceIds(param.type, resources) }
            type.type.result?.let { result -> collectResourceIds(result, resources) }
        }
        is ComponentDefinedType.Component -> resources += type.type.freeResourceIds()
        is ComponentDefinedType.Instance -> resources += type.type.freeResourceIds()
        is ComponentDefinedType.Resource -> resources += type.id
    }
}

private fun collectResourceIds(
    type: ComponentEntityType,
    resources: MutableSet<ComponentResourceTypeId>,
) {
    when (type) {
        is ComponentEntityType.CoreModule -> Unit
        is ComponentEntityType.Function -> {
            type.type.params.forEach { param -> collectResourceIds(param.type, resources) }
            type.type.result?.let { result -> collectResourceIds(result, resources) }
        }
        is ComponentEntityType.Value -> collectResourceIds(type.type, resources)
        is ComponentEntityType.Type -> collectResourceIds(type.referenced.type, resources)
        is ComponentEntityType.Component -> resources += type.type.freeResourceIds()
        is ComponentEntityType.Instance -> resources += type.type.freeResourceIds()
    }
}
