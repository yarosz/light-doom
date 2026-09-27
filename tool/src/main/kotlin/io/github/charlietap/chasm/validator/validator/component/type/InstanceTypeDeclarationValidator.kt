package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.ast.component.InstanceDeclaration
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.validator.ComponentValidator
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun InstanceDeclarationValidator(
    context: ComponentValidationContext,
    declaration: InstanceDeclaration,
): Result<Unit, ComponentValidatorError> = InstanceDeclarationValidator(
    context = context,
    declaration = declaration,
    coreTypeValidator = ::ComponentTypeCoreTypeValidator,
    typeDefinitionValidator = ::ComponentTypeDefinitionValidator,
    aliasValidator = ::ComponentTypeAliasValidator,
    exportValidator = ::InstanceTypeExportDeclarationValidator,
)

internal inline fun InstanceDeclarationValidator(
    context: ComponentValidationContext,
    declaration: InstanceDeclaration,
    crossinline coreTypeValidator: ComponentTypeResolver<CoreTypeDefinition, Unit>,
    crossinline typeDefinitionValidator: ComponentValidator<TypeDefinition>,
    crossinline aliasValidator: ComponentTypeResolver<io.github.charlietap.chasm.ast.component.AliasDefinition, Unit>,
    crossinline exportValidator: ComponentTypeResolver<InstanceDeclaration.Export, Unit>,
): Result<Unit, ComponentValidatorError> = when (declaration) {
    is InstanceDeclaration.CoreType -> coreTypeValidator(context, declaration.type)
    is InstanceDeclaration.Type -> typeDefinitionValidator(context, declaration.type)
    is InstanceDeclaration.Alias -> aliasValidator(context, declaration.alias)
    is InstanceDeclaration.Export -> exportValidator(context, declaration)
}

internal fun InstanceTypeExportDeclarationValidator(
    context: ComponentValidationContext,
    declaration: InstanceDeclaration.Export,
): Result<Unit, ComponentValidatorError> = ComponentTypeExportDeclarationValidator(
    context = context,
    name = declaration.name,
    type = declaration.type,
)
