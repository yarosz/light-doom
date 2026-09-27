package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.DefinedValueType
import io.github.charlietap.chasm.ast.component.FunctionType
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.ComponentInstanceType
import io.github.charlietap.chasm.type.component.ComponentType
import io.github.charlietap.chasm.type.component.ComponentTypeDefinition
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentTypeEntry
import io.github.charlietap.chasm.validator.type.component.MAX_COMPONENT_TYPE_NESTING_DEPTH
import io.github.charlietap.chasm.validator.type.component.MAX_COMPONENT_TYPE_SIZE

internal fun ComponentTypeDefinitionValidator(
    context: ComponentValidationContext,
    definition: TypeDefinition,
): Result<Unit, ComponentValidatorError> = ComponentTypeDefinitionValidator(
    context = context,
    definition = definition,
    typeDefinitionResolver = ::ComponentTypeDefinitionResolver,
)

internal inline fun ComponentTypeDefinitionValidator(
    context: ComponentValidationContext,
    definition: TypeDefinition,
    crossinline typeDefinitionResolver: ComponentTypeResolver<TypeDefinition, ComponentDefinedType>,
): Result<Unit, ComponentValidatorError> = binding {
    val type = typeDefinitionResolver(context, definition).bind()
    val entry = context.frame.typeEntry(ComponentTypeDefinition(context.identities.typeId(), type))
    if (entry.info.effectiveSize >= MAX_COMPONENT_TYPE_SIZE) {
        com.github.michaelbull.result.Err(
            ComponentValidatorError.InvalidType(EFFECTIVE_TYPE_SIZE_EXCEEDS_LIMIT),
        ).bind<Unit>()
    }
    if (entry.info.nestingDepth > MAX_COMPONENT_TYPE_NESTING_DEPTH) {
        com.github.michaelbull.result.Err(
            ComponentValidatorError.InvalidType(TYPE_NESTING_TOO_DEEP),
        ).bind<Unit>()
    }
    context.frame.addType(entry)
    if (type is ComponentDefinedType.Resource) {
        context.frame.definedResources += type.id
    }
}

private const val EFFECTIVE_TYPE_SIZE_EXCEEDS_LIMIT = "effective type size exceeds the limit"
private const val TYPE_NESTING_TOO_DEEP = "component type nesting is too deep"

internal fun ComponentTypeDefinitionResolver(
    context: ComponentValidationContext,
    definition: TypeDefinition,
): Result<ComponentDefinedType, ComponentValidatorError> = ComponentTypeDefinitionResolver(
    context = context,
    definition = definition,
    definedValueTypeResolver = ::ComponentDefinedValueTypeResolver,
    functionTypeResolver = ::ComponentFunctionTypeResolver,
    componentTypeResolver = ::ComponentDefinitionTypeResolver,
    instanceTypeResolver = ::InstanceDefinitionTypeResolver,
    resourceTypeResolver = ::ComponentResourceTypeResolver,
)

internal inline fun ComponentTypeDefinitionResolver(
    context: ComponentValidationContext,
    definition: TypeDefinition,
    crossinline definedValueTypeResolver: ComponentTypeResolver<DefinedValueType, ComponentDefinedValueType>,
    crossinline functionTypeResolver: ComponentTypeResolver<FunctionType, ComponentFunctionType>,
    crossinline componentTypeResolver: ComponentTypeResolver<TypeDefinition.Component, ComponentType>,
    crossinline instanceTypeResolver: ComponentTypeResolver<TypeDefinition.Instance, ComponentInstanceType>,
    crossinline resourceTypeResolver: ComponentTypeResolver<TypeDefinition.Resource, ComponentDefinedType.Resource>,
): Result<ComponentDefinedType, ComponentValidatorError> = binding {
    when (definition) {
        is TypeDefinition.Value -> ComponentDefinedType.Value(
            definedValueTypeResolver(context, definition.type).bind(),
        )
        is TypeDefinition.Function -> ComponentDefinedType.Function(
            functionTypeResolver(context, definition.type).bind(),
        )
        is TypeDefinition.Component -> ComponentDefinedType.Component(
            componentTypeResolver(context, definition).bind(),
        )
        is TypeDefinition.Instance -> ComponentDefinedType.Instance(
            instanceTypeResolver(context, definition).bind(),
        )
        is TypeDefinition.Resource -> resourceTypeResolver(context, definition).bind()
    }
}
