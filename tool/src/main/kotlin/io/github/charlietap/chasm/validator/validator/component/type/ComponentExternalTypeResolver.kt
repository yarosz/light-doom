package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExternalType
import io.github.charlietap.chasm.ast.component.TypeBound
import io.github.charlietap.chasm.ast.component.ValueBound
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentTypeDefinition
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.type.component.CoreType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentTypeEntry
import io.github.charlietap.chasm.validator.type.component.sortName
import io.github.charlietap.chasm.validator.validator.component.linking.type.componentIndex

internal fun ComponentExternalTypeResolver(
    context: ComponentValidationContext,
    type: ExternalType,
): Result<ComponentEntityType, ComponentValidatorError> = ComponentExternalTypeResolver(
    context = context,
    type = type,
    valueTypeResolver = ::ComponentValueTypeResolver,
)

internal inline fun ComponentExternalTypeResolver(
    context: ComponentValidationContext,
    type: ExternalType,
    crossinline valueTypeResolver: ComponentTypeResolver<ValueType, ComponentValueType>,
): Result<ComponentEntityType, ComponentValidatorError> = binding {
    when (type) {
        is ExternalType.CoreModule -> {
            val coreType = context.frame.coreTypes.componentIndex(type.typeIndex.idx)
                ?: Err(ComponentValidatorError.UnknownIndex(CORE_TYPE_SORT, type.typeIndex.idx)).bind()
            when (coreType) {
                is CoreType.Module -> ComponentEntityType.CoreModule(coreType.type)
                is CoreType.Defined -> Err(
                    ComponentValidatorError.SortMismatch(CORE_MODULE_TYPE_SORT, CORE_DEFINED_TYPE_SORT),
                ).bind()
            }
        }
        is ExternalType.Function -> {
            val definedType = componentTypeEntry(context, type.typeIndex.idx).bind().type
            when (definedType) {
                is ComponentDefinedType.Function -> ComponentEntityType.Function(definedType.type)
                else -> Err(
                    ComponentValidatorError.SortMismatch(COMPONENT_FUNCTION_TYPE_SORT, definedType.sortName()),
                ).bind()
            }
        }
        is ExternalType.Value -> {
            if (ComponentFeature.Values !in context.config.features) {
                Err(ComponentValidatorError.FeatureDisabled(ComponentFeature.Values)).bind<Unit>()
            }
            val valueType = when (val bound = type.bound) {
                is ValueBound.Equals -> {
                    context.frame.values.componentIndex(bound.valueIndex.idx)?.type
                        ?: Err(
                            ComponentValidatorError.UnknownIndex(COMPONENT_VALUE_SORT, bound.valueIndex.idx),
                        ).bind()
                }
                is ValueBound.Type -> valueTypeResolver(context, bound.type).bind()
            }
            ComponentEntityType.Value(valueType)
        }
        is ExternalType.Type -> {
            val referenced = when (val bound = type.bound) {
                is TypeBound.Equals -> componentTypeEntry(context, bound.typeIndex.idx).bind()
                TypeBound.SubResource -> context.frame.typeEntry(
                    ComponentTypeDefinition(
                        id = context.identities.typeId(),
                        type = ComponentDefinedType.Resource(
                            id = context.identities.resourceId(),
                        ),
                    ),
                )
            }
            val createdId = when (type.bound) {
                is TypeBound.Equals -> context.identities.typeId()
                TypeBound.SubResource -> referenced.id
            }
            ComponentEntityType.Type(referenced.definition, createdId)
        }
        is ExternalType.Component -> {
            val definedType = componentTypeEntry(context, type.typeIndex.idx).bind().type
            when (definedType) {
                is ComponentDefinedType.Component -> ComponentEntityType.Component(definedType.type)
                else -> Err(
                    ComponentValidatorError.SortMismatch(COMPONENT_TYPE_SORT, definedType.sortName()),
                ).bind()
            }
        }
        is ExternalType.Instance -> {
            val definedType = componentTypeEntry(context, type.typeIndex.idx).bind().type
            when (definedType) {
                is ComponentDefinedType.Instance -> ComponentEntityType.Instance(definedType.type)
                else -> Err(
                    ComponentValidatorError.SortMismatch(COMPONENT_INSTANCE_TYPE_SORT, definedType.sortName()),
                ).bind()
            }
        }
    }
}

private fun componentTypeEntry(
    context: ComponentValidationContext,
    index: UInt,
): Result<ComponentTypeEntry, ComponentValidatorError> {
    val type = context.frame.types.componentIndex(index)
        ?: return Err(ComponentValidatorError.UnknownIndex(COMPONENT_TYPE_SORT, index))
    return Ok(type)
}

private const val CORE_TYPE_SORT = "core type"
private const val CORE_MODULE_TYPE_SORT = "core module type"
private const val CORE_DEFINED_TYPE_SORT = "core defined type"
private const val COMPONENT_TYPE_SORT = "component type"
private const val COMPONENT_VALUE_SORT = "component value"
private const val COMPONENT_FUNCTION_TYPE_SORT = "component function type"
private const val COMPONENT_INSTANCE_TYPE_SORT = "component instance type"
