package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.FieldType
import io.github.charlietap.chasm.type.StorageType
import io.github.charlietap.chasm.validator.CoreTypeValidator
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun FieldTypeValidator(
    context: CoreTypeValidationContext,
    type: FieldType,
): Result<Unit, ModuleValidatorError> =
    FieldTypeValidator(
        context = context,
        type = type,
        storageTypeValidator = ::StorageTypeValidator,
    )

internal inline fun FieldTypeValidator(
    context: CoreTypeValidationContext,
    type: FieldType,
    crossinline storageTypeValidator: CoreTypeValidator<StorageType>,
): Result<Unit, ModuleValidatorError> = binding {
    storageTypeValidator(context, type.storageType).bind()
}
