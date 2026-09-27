package io.github.charlietap.chasm.validator.validator.instruction.memory

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.instruction.MemoryInstruction
import io.github.charlietap.chasm.ast.module.Index
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.ext.popI32
import io.github.charlietap.chasm.validator.ext.popMemoryAddress
import io.github.charlietap.chasm.validator.validator.index.DataIndexValidator
import io.github.charlietap.chasm.validator.validator.index.MemoryIndexValidator

internal fun MemoryInitInstructionValidator(
    context: ModuleValidationContext,
    instruction: MemoryInstruction.MemoryInit,
): Result<Unit, ModuleValidatorError> =
    MemoryInitInstructionValidator(
        context = context,
        instruction = instruction,
        dataIndexValidator = ::DataIndexValidator,
        memoryIndexValidator = ::MemoryIndexValidator,
    )

internal inline fun MemoryInitInstructionValidator(
    context: ModuleValidationContext,
    instruction: MemoryInstruction.MemoryInit,
    crossinline dataIndexValidator: ModuleValidator<Index.DataIndex>,
    crossinline memoryIndexValidator: ModuleValidator<Index.MemoryIndex>,
): Result<Unit, ModuleValidatorError> = binding {

    dataIndexValidator(context, instruction.dataIndex).bind()
    memoryIndexValidator(context, instruction.memoryIndex).bind()

    context.popI32().bind()
    context.popI32().bind()
    context.popMemoryAddress(instruction.memoryIndex).bind()
}
