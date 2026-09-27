package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.ComponentInstanceType
import io.github.charlietap.chasm.type.component.ComponentType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentTypeEntry
import io.github.charlietap.chasm.validator.type.component.sortName

internal fun ComponentTypeEntryResolver(
    context: ComponentValidationContext,
    index: ComponentTypeIndex,
): Result<ComponentTypeEntry, ComponentValidatorError> {
    val type = context.frame.types.getOrNull(index.idx.toInt())
        ?: return Err(ComponentValidatorError.UnknownIndex(TYPE_SORT, index.idx))
    return Ok(type)
}

internal fun ComponentDefinedTypeResolver(
    context: ComponentValidationContext,
    index: ComponentTypeIndex,
): Result<ComponentDefinedType, ComponentValidatorError> = ComponentTypeEntryResolver(context, index).fold(
    success = { entry -> Ok(entry.type) },
    failure = ::Err,
)

internal fun ComponentValueTypeIndexResolver(
    context: ComponentValidationContext,
    index: ComponentTypeIndex,
): Result<ComponentValueType, ComponentValidatorError> = ComponentTypeEntryResolver(context, index).fold(
    success = { entry ->
        when (val type = entry.type) {
            is ComponentDefinedType.Value -> Ok(
                ComponentValueType.Defined(entry.definition),
            )
            else -> Err(ComponentValidatorError.SortMismatch(VALUE_TYPE_SORT, type.sortName()))
        }
    },
    failure = ::Err,
)

internal fun ComponentResourceTypeIndexResolver(
    context: ComponentValidationContext,
    index: ComponentTypeIndex,
): Result<ComponentTypeEntry, ComponentValidatorError> = ComponentTypeEntryResolver(context, index).fold(
    success = { entry ->
        when (val type = entry.type) {
            is ComponentDefinedType.Resource -> Ok(entry)
            else -> Err(ComponentValidatorError.SortMismatch(RESOURCE_TYPE_SORT, type.sortName()))
        }
    },
    failure = ::Err,
)

internal fun ComponentFunctionTypeIndexResolver(
    context: ComponentValidationContext,
    index: ComponentTypeIndex,
): Result<ComponentFunctionType, ComponentValidatorError> =
    ComponentDefinedTypeResolver(context, index).fold(
        success = { type ->
            when (type) {
                is ComponentDefinedType.Function -> Ok(type.type)
                else -> Err(ComponentValidatorError.SortMismatch(FUNCTION_TYPE_SORT, type.sortName()))
            }
        },
        failure = ::Err,
    )

internal fun ComponentTypeIndexResolver(
    context: ComponentValidationContext,
    index: ComponentTypeIndex,
): Result<ComponentType, ComponentValidatorError> =
    ComponentDefinedTypeResolver(context, index).fold(
        success = { type ->
            when (type) {
                is ComponentDefinedType.Component -> Ok(type.type)
                else -> Err(ComponentValidatorError.SortMismatch(COMPONENT_TYPE_SORT, type.sortName()))
            }
        },
        failure = ::Err,
    )

internal fun ComponentInstanceTypeIndexResolver(
    context: ComponentValidationContext,
    index: ComponentTypeIndex,
): Result<ComponentInstanceType, ComponentValidatorError> =
    ComponentDefinedTypeResolver(context, index).fold(
        success = { type ->
            when (type) {
                is ComponentDefinedType.Instance -> Ok(type.type)
                else -> Err(ComponentValidatorError.SortMismatch(INSTANCE_TYPE_SORT, type.sortName()))
            }
        },
        failure = ::Err,
    )

private const val TYPE_SORT = "component type"
private const val VALUE_TYPE_SORT = "component value type"
private const val FUNCTION_TYPE_SORT = "component function type"
private const val COMPONENT_TYPE_SORT = "component type definition"
private const val INSTANCE_TYPE_SORT = "component instance type"
private const val RESOURCE_TYPE_SORT = "component resource type"
