package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.FieldType
import io.github.charlietap.chasm.type.StructType
import io.github.charlietap.chasm.validator.CoreTypeValidator
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun StructTypeValidator(
    context: CoreTypeValidationContext,
    type: StructType,
): Result<Unit, ModuleValidatorError> =
    StructTypeValidator(
        context = context,
        type = type,
        fieldTypeValidator = ::FieldTypeValidator,
    )

internal inline fun StructTypeValidator(
    context: CoreTypeValidationContext,
    type: StructType,
    crossinline fieldTypeValidator: CoreTypeValidator<FieldType>,
): Result<Unit, ModuleValidatorError> = binding {
    type.fields.forEach { field ->
        fieldTypeValidator(context, field).bind()
    }
}
