package io.github.charlietap.chasm.validator

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal typealias ModuleValidator<T> = (ModuleValidationContext, T) -> Result<Unit, ModuleValidatorError>
internal typealias CoreTypeValidator<T> = (CoreTypeValidationContext, T) -> Result<Unit, ModuleValidatorError>
