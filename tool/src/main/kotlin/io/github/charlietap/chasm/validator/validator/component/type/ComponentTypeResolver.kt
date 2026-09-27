package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal typealias ComponentTypeResolver<I, O> =
    (ComponentValidationContext, I) -> Result<O, ComponentValidatorError>
