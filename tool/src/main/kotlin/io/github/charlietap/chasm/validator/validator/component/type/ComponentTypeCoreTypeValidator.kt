package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.type.component.CoreModuleType
import io.github.charlietap.chasm.type.component.CoreType
import io.github.charlietap.chasm.validator.ComponentValidator
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.core.type.CoreDefinedTypeValidator
import io.github.charlietap.chasm.validator.validator.component.core.type.CoreModuleTypeValidator

internal fun ComponentTypeCoreTypeValidator(
    context: ComponentValidationContext,
    definition: CoreTypeDefinition,
): Result<Unit, ComponentValidatorError> = ComponentTypeCoreTypeValidator(
    context = context,
    definition = definition,
    definedTypeValidator = ::CoreDefinedTypeValidator,
    moduleTypeValidator = ::CoreModuleTypeValidator,
)

internal inline fun ComponentTypeCoreTypeValidator(
    context: ComponentValidationContext,
    definition: CoreTypeDefinition,
    crossinline definedTypeValidator: ComponentValidator<CoreTypeDefinition.DefinedType>,
    crossinline moduleTypeValidator: ComponentTypeResolver<CoreTypeDefinition.ModuleType, CoreModuleType>,
): Result<Unit, ComponentValidatorError> = binding {
    when (definition) {
        is CoreTypeDefinition.DefinedType -> definedTypeValidator(context, definition).bind()
        is CoreTypeDefinition.ModuleType -> {
            val moduleType = moduleTypeValidator(context, definition).bind()
            context.frame.coreTypes += CoreType.Module(moduleType)
        }
    }
}
