package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ComponentDeclaration
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.ast.component.ExternalType
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.validator.ComponentValidator
import io.github.charlietap.chasm.validator.context.component.ComponentScopeKind
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentExternalRegistrar
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentEntityOrigin
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentExportedEntityType
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentImportedEntityType
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentExternalRegistrar as RegisterComponentExternal

internal fun ComponentDeclarationValidator(
    context: ComponentValidationContext,
    declaration: ComponentDeclaration,
): Result<Unit, ComponentValidatorError> = ComponentDeclarationValidator(
    context = context,
    declaration = declaration,
    coreTypeValidator = ::ComponentTypeCoreTypeValidator,
    typeDefinitionValidator = ::ComponentTypeDefinitionValidator,
    aliasValidator = ::ComponentTypeAliasValidator,
    importValidator = ::ComponentTypeImportDeclarationValidator,
    exportValidator = ::ComponentTypeExportDeclarationValidator,
)

internal inline fun ComponentDeclarationValidator(
    context: ComponentValidationContext,
    declaration: ComponentDeclaration,
    crossinline coreTypeValidator: ComponentTypeResolver<CoreTypeDefinition, Unit>,
    crossinline typeDefinitionValidator: ComponentValidator<TypeDefinition>,
    crossinline aliasValidator: ComponentTypeResolver<io.github.charlietap.chasm.ast.component.AliasDefinition, Unit>,
    crossinline importValidator: ComponentTypeResolver<ComponentDeclaration.Import, Unit>,
    crossinline exportValidator: ComponentTypeResolver<ComponentDeclaration.Export, Unit>,
): Result<Unit, ComponentValidatorError> = when (declaration) {
    is ComponentDeclaration.CoreType -> coreTypeValidator(context, declaration.type)
    is ComponentDeclaration.Type -> typeDefinitionValidator(context, declaration.type)
    is ComponentDeclaration.Alias -> aliasValidator(context, declaration.alias)
    is ComponentDeclaration.Import -> importValidator(context, declaration)
    is ComponentDeclaration.Export -> exportValidator(context, declaration)
}

internal fun ComponentTypeImportDeclarationValidator(
    context: ComponentValidationContext,
    declaration: ComponentDeclaration.Import,
): Result<Unit, ComponentValidatorError> = ComponentTypeImportDeclarationValidator(
    context = context,
    declaration = declaration,
    externalTypeResolver = ::ComponentExternalTypeResolver,
    importedEntityType = ::ComponentImportedEntityType,
    externalRegistrar = ::RegisterComponentExternal,
)

internal inline fun ComponentTypeImportDeclarationValidator(
    context: ComponentValidationContext,
    declaration: ComponentDeclaration.Import,
    crossinline externalTypeResolver: ComponentTypeResolver<ExternalType, ComponentEntityType>,
    crossinline importedEntityType: (
        ComponentValidationContext,
        ComponentEntityType,
    ) -> ComponentEntityType,
    crossinline externalRegistrar: ComponentExternalRegistrar,
): Result<Unit, ComponentValidatorError> = binding {
    val resolvedType = externalTypeResolver(context, declaration.type).bind()
    val type = importedEntityType(context, resolvedType)
    externalRegistrar(context, declaration.name, type, ComponentEntityOrigin.Import, true).bind()
}

internal fun ComponentTypeExportDeclarationValidator(
    context: ComponentValidationContext,
    declaration: ComponentDeclaration.Export,
): Result<Unit, ComponentValidatorError> = ComponentTypeExportDeclarationValidator(
    context = context,
    name = declaration.name,
    type = declaration.type,
)

internal fun ComponentTypeExportDeclarationValidator(
    context: ComponentValidationContext,
    name: NameAttributes,
    type: ExternalType,
): Result<Unit, ComponentValidatorError> = ComponentTypeExportDeclarationValidator(
    context = context,
    name = name,
    type = type,
    externalTypeResolver = ::ComponentExternalTypeResolver,
    exportedEntityType = ::ComponentExportedEntityType,
    externalRegistrar = ::RegisterComponentExternal,
)

internal inline fun ComponentTypeExportDeclarationValidator(
    context: ComponentValidationContext,
    name: NameAttributes,
    type: ExternalType,
    crossinline externalTypeResolver: ComponentTypeResolver<ExternalType, ComponentEntityType>,
    crossinline exportedEntityType: (
        ComponentValidationContext,
        ComponentEntityType,
    ) -> ComponentEntityType,
    crossinline externalRegistrar: ComponentExternalRegistrar,
): Result<Unit, ComponentValidatorError> = binding {
    val externalType = externalTypeResolver(context, type).bind()
    val resolvedType = exportedEntityType(context, externalType)
    val validateVisibility = context.frame.kind != ComponentScopeKind.InstanceType
    externalRegistrar(context, name, resolvedType, ComponentEntityOrigin.Export, validateVisibility).bind()
}
