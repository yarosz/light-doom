package io.github.charlietap.chasm.validator.validator.import

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.Import
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.validator.type.MemoryTypeValidator

internal fun MemoryImportValidator(
    context: ModuleValidationContext,
    descriptor: Import.Descriptor.Memory,
): Result<Unit, ModuleValidatorError> =
    MemoryImportValidator(
        context = context,
        descriptor = descriptor,
        typeValidator = ::MemoryTypeValidator,
    )

internal inline fun MemoryImportValidator(
    context: ModuleValidationContext,
    descriptor: Import.Descriptor.Memory,
    crossinline typeValidator: ModuleValidator<MemoryType>,
): Result<Unit, ModuleValidatorError> = binding {
    typeValidator(context, descriptor.type).bind()
}
