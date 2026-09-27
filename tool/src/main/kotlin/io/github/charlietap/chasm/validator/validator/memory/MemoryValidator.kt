package io.github.charlietap.chasm.validator.validator.memory

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.Memory
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.validator.type.MemoryTypeValidator

internal fun MemoryValidator(
    context: ModuleValidationContext,
    memory: Memory,
): Result<Unit, ModuleValidatorError> =
    MemoryValidator(
        context = context,
        memory = memory,
        typeValidator = ::MemoryTypeValidator,
    )

internal inline fun MemoryValidator(
    context: ModuleValidationContext,
    memory: Memory,
    crossinline typeValidator: ModuleValidator<MemoryType>,
): Result<Unit, ModuleValidatorError> = binding {
    typeValidator(context, memory.type).bind()
}
