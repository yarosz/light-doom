package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.StorageType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.validator.CoreTypeValidator
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun StorageTypeValidator(
    context: CoreTypeValidationContext,
    type: StorageType,
): Result<Unit, ModuleValidatorError> =
    StorageTypeValidator(
        context = context,
        type = type,
        valueTypeValidator = ::ValueTypeValidator,
    )

internal inline fun StorageTypeValidator(
    context: CoreTypeValidationContext,
    type: StorageType,
    crossinline valueTypeValidator: CoreTypeValidator<ValueType>,
): Result<Unit, ModuleValidatorError> = binding {
    when (type) {
        is StorageType.Packed -> Unit
        is StorageType.Value -> valueTypeValidator(context, type.type).bind()
    }
}
