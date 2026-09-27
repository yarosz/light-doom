package io.github.charlietap.chasm.validator.context.scope

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal typealias Scope<T> = (
    ModuleValidationContext,
    T,
    (ModuleValidationContext) -> Result<Unit, ModuleValidatorError>,
) -> Result<Unit, ModuleValidatorError>

internal typealias CoreTypeScope<T> = (
    CoreTypeValidationContext,
    T,
    (CoreTypeValidationContext) -> Result<Unit, ModuleValidatorError>,
) -> Result<Unit, ModuleValidatorError>

internal typealias NewScope<T> = Scope<T>
