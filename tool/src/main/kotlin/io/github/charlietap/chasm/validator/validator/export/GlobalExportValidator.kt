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
import io.github.charlietap.chasm.validator.validator.index.GlobalIndexValidator

internal fun GlobalExportValidator(
    context: ModuleValidationContext,
    descriptor: Export.Descriptor.Global,
): Result<Unit, ModuleValidatorError> =
    GlobalExportValidator(
        context = context,
        descriptor = descriptor,
        globalIndexValidator = ::GlobalIndexValidator,
    )

internal inline fun GlobalExportValidator(
    context: ModuleValidationContext,
    descriptor: Export.Descriptor.Global,
    crossinline globalIndexValidator: ModuleValidator<Index.GlobalIndex>,
): Result<Unit, ModuleValidatorError> = binding {
    globalIndexValidator(context, descriptor.globalIndex)
        .mapError {
            ExportValidatorError.UnknownGlobal
        }.bind()
}
