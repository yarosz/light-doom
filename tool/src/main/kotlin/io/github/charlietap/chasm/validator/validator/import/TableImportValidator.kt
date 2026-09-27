package io.github.charlietap.chasm.validator.validator.import

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.Import
import io.github.charlietap.chasm.type.TableType
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.validator.type.TableTypeValidator

internal fun TableImportValidator(
    context: ModuleValidationContext,
    descriptor: Import.Descriptor.Table,
): Result<Unit, ModuleValidatorError> =
    TableImportValidator(
        context = context,
        descriptor = descriptor,
        typeValidator = ::TableTypeValidator,
    )

internal inline fun TableImportValidator(
    context: ModuleValidationContext,
    descriptor: Import.Descriptor.Table,
    crossinline typeValidator: ModuleValidator<TableType>,
): Result<Unit, ModuleValidatorError> = binding {
    typeValidator(context, descriptor.type).bind()
}
