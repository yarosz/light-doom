package io.github.charlietap.chasm.validator.validator.component

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.AliasDefinition
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.ast.component.Component
import io.github.charlietap.chasm.ast.component.ComponentValue
import io.github.charlietap.chasm.ast.component.CoreInstance
import io.github.charlietap.chasm.ast.component.CoreModule
import io.github.charlietap.chasm.ast.component.CoreType
import io.github.charlietap.chasm.ast.component.Definition
import io.github.charlietap.chasm.ast.component.Export
import io.github.charlietap.chasm.ast.component.Import
import io.github.charlietap.chasm.ast.component.InstanceDefinition
import io.github.charlietap.chasm.ast.component.StartDefinition
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.validator.ComponentValidator
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.canonical.CanonicalDefinitionValidator
import io.github.charlietap.chasm.validator.validator.component.core.instance.CoreInstanceValidator
import io.github.charlietap.chasm.validator.validator.component.core.module.CoreModuleValidator
import io.github.charlietap.chasm.validator.validator.component.core.type.CoreTypeValidator
import io.github.charlietap.chasm.validator.validator.component.linking.alias.ComponentAliasValidator
import io.github.charlietap.chasm.validator.validator.component.linking.component.NestedComponentValidator
import io.github.charlietap.chasm.validator.validator.component.linking.export.ComponentExportValidator
import io.github.charlietap.chasm.validator.validator.component.linking.import.ComponentImportValidator
import io.github.charlietap.chasm.validator.validator.component.linking.instance.ComponentInstanceValidator
import io.github.charlietap.chasm.validator.validator.component.linking.start.ComponentStartValidator
import io.github.charlietap.chasm.validator.validator.component.linking.value.ComponentValueValidator
import io.github.charlietap.chasm.validator.validator.component.type.ComponentTypeDefinitionValidator

internal fun ComponentDefinitionValidator(
    context: ComponentValidationContext,
    definition: Definition,
): Result<Unit, ComponentValidatorError> = ComponentDefinitionValidator(
    context = context,
    definition = definition,
    coreModuleValidator = ::CoreModuleValidator,
    coreInstanceValidator = ::CoreInstanceValidator,
    coreTypeValidator = ::CoreTypeValidator,
    nestedComponentValidator = ::NestedComponentValidator,
    instanceValidator = ::ComponentInstanceValidator,
    aliasValidator = ::ComponentAliasValidator,
    typeValidator = ::ComponentTypeDefinitionValidator,
    canonicalValidator = ::CanonicalDefinitionValidator,
    startValidator = ::ComponentStartValidator,
    importValidator = ::ComponentImportValidator,
    exportValidator = ::ComponentExportValidator,
    valueValidator = ::ComponentValueValidator,
)

internal inline fun ComponentDefinitionValidator(
    context: ComponentValidationContext,
    definition: Definition,
    crossinline coreModuleValidator: ComponentValidator<CoreModule>,
    crossinline coreInstanceValidator: ComponentValidator<CoreInstance>,
    crossinline coreTypeValidator: ComponentValidator<CoreType>,
    crossinline nestedComponentValidator: ComponentValidator<Component>,
    crossinline instanceValidator: ComponentValidator<InstanceDefinition>,
    crossinline aliasValidator: ComponentValidator<AliasDefinition>,
    crossinline typeValidator: ComponentValidator<TypeDefinition>,
    crossinline canonicalValidator: ComponentValidator<CanonicalDefinition>,
    crossinline startValidator: ComponentValidator<StartDefinition>,
    crossinline importValidator: ComponentValidator<Import>,
    crossinline exportValidator: ComponentValidator<Export>,
    crossinline valueValidator: ComponentValidator<ComponentValue>,
): Result<Unit, ComponentValidatorError> = when (definition) {
    is CoreModule -> coreModuleValidator(context, definition)
    is CoreInstance -> coreInstanceValidator(context, definition)
    is CoreType -> coreTypeValidator(context, definition)
    is io.github.charlietap.chasm.ast.component.NestedComponent -> {
        nestedComponentValidator(context, definition.component)
    }
    is io.github.charlietap.chasm.ast.component.Instance -> instanceValidator(context, definition.instance)
    is io.github.charlietap.chasm.ast.component.Alias -> aliasValidator(context, definition.alias)
    is io.github.charlietap.chasm.ast.component.Type -> typeValidator(context, definition.type)
    is io.github.charlietap.chasm.ast.component.Canon -> canonicalValidator(context, definition.canon)
    is io.github.charlietap.chasm.ast.component.Start -> startValidator(context, definition.start)
    is Import -> importValidator(context, definition)
    is Export -> exportValidator(context, definition)
    is io.github.charlietap.chasm.ast.component.Value -> valueValidator(context, definition.value)
}
