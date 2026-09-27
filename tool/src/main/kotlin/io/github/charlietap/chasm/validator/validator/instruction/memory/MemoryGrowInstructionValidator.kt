package io.github.charlietap.chasm.validator.validator.instruction.memory

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.instruction.MemoryInstruction
import io.github.charlietap.chasm.ast.module.Index
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.ext.popMemoryAddress
import io.github.charlietap.chasm.validator.ext.pushMemoryAddress
import io.github.charlietap.chasm.validator.validator.index.MemoryIndexValidator

internal fun MemoryGrowInstructionValidator(
    context: ModuleValidationContext,
    instruction: MemoryInstruction.MemoryGrow,
): Result<Unit, ModuleValidatorError> =
    MemoryGrowInstructionValidator(
        context = context,
        instruction = instruction,
        memoryIndexValidator = ::MemoryIndexValidator,
    )

internal inline fun MemoryGrowInstructionValidator(
    context: ModuleValidationContext,
    instruction: MemoryInstruction.MemoryGrow,
    crossinline memoryIndexValidator: ModuleValidator<Index.MemoryIndex>,
): Result<Unit, ModuleValidatorError> = binding {

    memoryIndexValidator(context, instruction.memoryIndex).bind()

    context.popMemoryAddress(instruction.memoryIndex).bind()
    context.pushMemoryAddress(instruction.memoryIndex).bind()
}
