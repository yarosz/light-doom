package io.github.charlietap.chasm.validator

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal typealias ComponentValidator<T> =
    (ComponentValidationContext, T) -> Result<Unit, ComponentValidatorError>
