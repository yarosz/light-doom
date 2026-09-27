package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.TagType
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.error.TagValidatorError

internal fun TagTypeValidator(
    context: CoreTypeValidationContext,
    type: TagType,
): Result<Unit, ModuleValidatorError> = binding {
    if (type.functionType.results.types
            .isNotEmpty()
    ) {
        Err(TagValidatorError.InvalidTagType).bind()
    }
}
