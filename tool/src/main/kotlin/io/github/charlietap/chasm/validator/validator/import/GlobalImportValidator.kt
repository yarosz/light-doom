package io.github.charlietap.chasm.validator.validator.import

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.Import
import io.github.charlietap.chasm.type.GlobalType
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.validator.type.GlobalTypeValidator

internal fun GlobalImportValidator(
    context: ModuleValidationContext,
    descriptor: Import.Descriptor.Global,
): Result<Unit, ModuleValidatorError> =
    GlobalImportValidator(
        context = context,
        descriptor = descriptor,
        typeValidator = ::GlobalTypeValidator,
    )

internal inline fun GlobalImportValidator(
    context: ModuleValidationContext,
    descriptor: Import.Descriptor.Global,
    crossinline typeValidator: ModuleValidator<GlobalType>,
): Result<Unit, ModuleValidatorError> = binding {
    typeValidator(context, descriptor.type).bind()
}
