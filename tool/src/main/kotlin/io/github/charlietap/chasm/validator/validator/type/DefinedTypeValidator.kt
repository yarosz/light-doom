package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun DefinedTypeValidator(
    context: CoreTypeValidationContext,
    type: DefinedType,
): Result<Unit, ModuleValidatorError> = binding {
}
