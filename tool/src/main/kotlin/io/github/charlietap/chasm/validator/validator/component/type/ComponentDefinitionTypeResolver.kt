package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import io.github.charlietap.chasm.ast.component.ComponentDeclaration
import io.github.charlietap.chasm.ast.component.InstanceDeclaration
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.type.component.ComponentInstanceType
import io.github.charlietap.chasm.type.component.ComponentType
import io.github.charlietap.chasm.validator.ComponentValidator
import io.github.charlietap.chasm.validator.context.component.ComponentScopeKind
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.resourceIds

internal fun ComponentDefinitionTypeResolver(
    context: ComponentValidationContext,
    definition: TypeDefinition.Component,
): Result<ComponentType, ComponentValidatorError> = ComponentDefinitionTypeResolver(
    context = context,
    definition = definition,
    declarationValidator = ::ComponentDeclarationValidator,
)

internal inline fun ComponentDefinitionTypeResolver(
    context: ComponentValidationContext,
    definition: TypeDefinition.Component,
    crossinline declarationValidator: ComponentValidator<ComponentDeclaration>,
): Result<ComponentType, ComponentValidatorError> {
    if (!context.canPush()) {
        return Err(ComponentValidatorError.InvalidType(TYPE_NESTING_TOO_DEEP))
    }
    context.push(ComponentScopeKind.ComponentType)

    val error = validateDeclarations(context, definition.declarations, declarationValidator)
        ?: validateComponentType(context)
    val type = if (error == null) context.frame.componentType() else null

    context.pop()
    return if (error == null) Ok(type!!) else Err(error)
}

internal fun InstanceDefinitionTypeResolver(
    context: ComponentValidationContext,
    definition: TypeDefinition.Instance,
): Result<ComponentInstanceType, ComponentValidatorError> = InstanceDefinitionTypeResolver(
    context = context,
    definition = definition,
    declarationValidator = ::InstanceDeclarationValidator,
)

internal inline fun InstanceDefinitionTypeResolver(
    context: ComponentValidationContext,
    definition: TypeDefinition.Instance,
    crossinline declarationValidator: ComponentValidator<InstanceDeclaration>,
): Result<ComponentInstanceType, ComponentValidatorError> {
    if (!context.canPush()) {
        return Err(ComponentValidatorError.InvalidType(TYPE_NESTING_TOO_DEEP))
    }
    context.push(ComponentScopeKind.InstanceType)

    val error = validateDeclarations(context, definition.declarations, declarationValidator)
        ?: validateInstanceType(context)
    val type = if (error == null) context.frame.instanceType() else null

    context.pop()
    return if (error == null) Ok(type!!) else Err(error)
}

private const val TYPE_NESTING_TOO_DEEP = "component type nesting is too deep"

private inline fun <T> validateDeclarations(
    context: ComponentValidationContext,
    declarations: List<T>,
    declarationValidator: ComponentValidator<T>,
): ComponentValidatorError? {
    declarations.forEach { declaration ->
        val error = declarationValidator(context, declaration).fold(
            success = { null },
            failure = { failure -> failure },
        )
        if (error != null) return error
    }
    return null
}

private fun validateComponentType(
    context: ComponentValidationContext,
): ComponentValidatorError? {
    val frame = context.frame
    val importedResourceReferences = frame.imports.values
        .flatMapTo(linkedSetOf()) { type -> type.resourceIds() }
    val localImport = importedResourceReferences.firstOrNull(frame.definedResources::contains)
    if (localImport != null) {
        return ComponentValidatorError.InvalidType("component type import refers to a locally defined resource")
    }

    return validateExportedResources(context)
}

private fun validateInstanceType(
    context: ComponentValidationContext,
): ComponentValidatorError? = validateExportedResources(context)

private fun validateExportedResources(
    context: ComponentValidationContext,
): ComponentValidatorError? {
    val frame = context.frame
    val exportedResourceReferences = frame.exports.values
        .flatMapTo(linkedSetOf()) { type -> type.resourceIds() }
    val hiddenResource = exportedResourceReferences.firstOrNull { resource ->
        resource in frame.definedResources && resource !in frame.explicitResources
    }
    return hiddenResource?.let { resource ->
        ComponentValidatorError.InvalidType(
            "resource ${resource.value} is used by an export but is not explicitly exported",
        )
    }
}
