package io.github.charlietap.chasm.validator.validator.tag

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.Tag
import io.github.charlietap.chasm.type.TagType
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.validator.type.TagTypeValidator

internal fun TagValidator(
    context: ModuleValidationContext,
    tag: Tag,
): Result<Unit, ModuleValidatorError> =
    TagValidator(
        context = context,
        tag = tag,
        typeValidator = ::TagTypeValidator,
    )

internal inline fun TagValidator(
    context: ModuleValidationContext,
    tag: Tag,
    crossinline typeValidator: ModuleValidator<TagType>,
): Result<Unit, ModuleValidatorError> = binding {
    typeValidator(context, tag.type).bind()
}
