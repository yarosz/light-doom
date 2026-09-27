package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.HeapType
import io.github.charlietap.chasm.type.ReferenceType
import io.github.charlietap.chasm.validator.CoreTypeValidator
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun ReferenceTypeValidator(
    context: CoreTypeValidationContext,
    type: ReferenceType,
): Result<Unit, ModuleValidatorError> =
    ReferenceTypeValidator(
        context = context,
        type = type,
        heapTypeValidator = ::HeapTypeValidator,
    )

internal inline fun ReferenceTypeValidator(
    context: CoreTypeValidationContext,
    type: ReferenceType,
    crossinline heapTypeValidator: CoreTypeValidator<HeapType>,
): Result<Unit, ModuleValidatorError> = binding {
    heapTypeValidator(context, type.heapType).bind()
}
