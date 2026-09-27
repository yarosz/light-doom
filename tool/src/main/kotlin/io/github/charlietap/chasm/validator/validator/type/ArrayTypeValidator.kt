package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.ArrayType
import io.github.charlietap.chasm.type.FieldType
import io.github.charlietap.chasm.validator.CoreTypeValidator
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun ArrayTypeValidator(
    context: CoreTypeValidationContext,
    type: ArrayType,
): Result<Unit, ModuleValidatorError> =
    ArrayTypeValidator(
        context = context,
        type = type,
        fieldTypeValidator = ::FieldTypeValidator,
    )

internal inline fun ArrayTypeValidator(
    context: CoreTypeValidationContext,
    type: ArrayType,
    crossinline fieldTypeValidator: CoreTypeValidator<FieldType>,
): Result<Unit, ModuleValidatorError> = binding {
    fieldTypeValidator(context, type.fieldType).bind()
}
