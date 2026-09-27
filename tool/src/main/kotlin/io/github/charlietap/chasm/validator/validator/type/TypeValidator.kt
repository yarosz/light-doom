package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.Type
import io.github.charlietap.chasm.type.RecursiveType
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun TypeValidator(
    context: ModuleValidationContext,
    type: Type,
): Result<Unit, ModuleValidatorError> =
    TypeValidator(
        context = context,
        type = type,
        recursiveTypeValidator = ::RecursiveTypeValidator,
    )

internal inline fun TypeValidator(
    context: ModuleValidationContext,
    type: Type,
    crossinline recursiveTypeValidator: ModuleValidator<RecursiveType>,
): Result<Unit, ModuleValidatorError> = binding {
    context.definedTypesValidated += type.recursiveType.subTypes.size
    recursiveTypeValidator(context, type.recursiveType).bind()
}
