package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.GlobalType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.validator.CoreTypeValidator
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun GlobalTypeValidator(
    context: CoreTypeValidationContext,
    type: GlobalType,
): Result<Unit, ModuleValidatorError> =
    GlobalTypeValidator(
        context = context,
        type = type,
        valueTypeValidator = ::ValueTypeValidator,
    )

internal inline fun GlobalTypeValidator(
    context: CoreTypeValidationContext,
    type: GlobalType,
    crossinline valueTypeValidator: CoreTypeValidator<ValueType>,
): Result<Unit, ModuleValidatorError> = binding {
    valueTypeValidator(context, type.valueType).bind()
}
