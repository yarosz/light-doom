package io.github.charlietap.chasm.validator.validator.component.core.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreType
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.type.component.CoreModuleType
import io.github.charlietap.chasm.validator.ComponentValidator
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.type.component.CoreType as SemanticCoreType

internal fun CoreTypeValidator(
    context: ComponentValidationContext,
    type: CoreType,
): Result<Unit, ComponentValidatorError> = CoreTypeValidator(
    context = context,
    type = type,
    definedTypeValidator = ::CoreDefinedTypeValidator,
    moduleTypeValidator = ::CoreModuleTypeValidator,
)

internal inline fun CoreTypeValidator(
    context: ComponentValidationContext,
    type: CoreType,
    crossinline definedTypeValidator: ComponentValidator<CoreTypeDefinition.DefinedType>,
    crossinline moduleTypeValidator: (
        ComponentValidationContext,
        CoreTypeDefinition.ModuleType,
    ) -> Result<CoreModuleType, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    when (val definition = type.type) {
        is CoreTypeDefinition.DefinedType -> definedTypeValidator(context, definition).bind()
        is CoreTypeDefinition.ModuleType -> {
            val moduleType = moduleTypeValidator(context, definition).bind()
            context.frame.coreTypes += SemanticCoreType.Module(moduleType)
        }
    }
}
