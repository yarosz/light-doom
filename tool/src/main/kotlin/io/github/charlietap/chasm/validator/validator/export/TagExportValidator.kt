package io.github.charlietap.chasm.validator.validator.export

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import io.github.charlietap.chasm.ast.module.Export
import io.github.charlietap.chasm.ast.module.Index
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ExportValidatorError
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.validator.index.TagIndexValidator

internal fun TagExportValidator(
    context: ModuleValidationContext,
    descriptor: Export.Descriptor.Tag,
): Result<Unit, ModuleValidatorError> =
    TagExportValidator(
        context = context,
        descriptor = descriptor,
        tagIndexValidator = ::TagIndexValidator,
    )

internal inline fun TagExportValidator(
    context: ModuleValidationContext,
    descriptor: Export.Descriptor.Tag,
    crossinline tagIndexValidator: ModuleValidator<Index.TagIndex>,
): Result<Unit, ModuleValidatorError> = binding {
    tagIndexValidator(context, descriptor.tagIndex)
        .mapError {
            ExportValidatorError.UnknownTag
        }.bind()
}
