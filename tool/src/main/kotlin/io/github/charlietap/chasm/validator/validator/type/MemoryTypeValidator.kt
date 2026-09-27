package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.Limits
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.validator.CoreTypeValidator
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.context.scope.CoreTypeScope
import io.github.charlietap.chasm.validator.context.scope.MemoryTypeScope
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.validator.type.limits.LimitsValidator

internal fun MemoryTypeValidator(
    context: CoreTypeValidationContext,
    type: MemoryType,
): Result<Unit, ModuleValidatorError> =
    MemoryTypeValidator(
        context = context,
        type = type,
        scope = ::MemoryTypeScope,
        limitsValidator = ::LimitsValidator,
    )

internal inline fun MemoryTypeValidator(
    context: CoreTypeValidationContext,
    type: MemoryType,
    crossinline scope: CoreTypeScope<MemoryType>,
    crossinline limitsValidator: CoreTypeValidator<Limits>,
): Result<Unit, ModuleValidatorError> = binding {
    scope(context, type) { scopedContext ->
        limitsValidator(scopedContext, type.limits)
    }.bind()
}
