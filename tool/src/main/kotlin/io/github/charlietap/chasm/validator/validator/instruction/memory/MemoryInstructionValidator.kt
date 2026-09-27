package io.github.charlietap.chasm.validator.validator.instruction.memory

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.instruction.MemoryInstruction
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun MemoryInstructionValidator(
    context: ModuleValidationContext,
    instruction: MemoryInstruction,
): Result<Unit, ModuleValidatorError> =
    MemoryInstructionValidator(
        context = context,
        instruction = instruction,
        dataDropValidator = ::DataDropInstructionValidator,
        loadValidator = ::MemoryLoadInstructionValidator,
        storeValidator = ::MemoryStoreInstructionValidator,
        memoryCopyValidator = ::MemoryCopyInstructionValidator,
        memoryFillValidator = ::MemoryFillInstructionValidator,
        memoryGrowValidator = ::MemoryGrowInstructionValidator,
        memoryInitValidator = ::MemoryInitInstructionValidator,
        memorySizeValidator = ::MemorySizeInstructionValidator,
    )

internal inline fun MemoryInstructionValidator(
    context: ModuleValidationContext,
    instruction: MemoryInstruction,
    crossinline dataDropValidator: ModuleValidator<MemoryInstruction.DataDrop>,
    crossinline loadValidator: ModuleValidator<MemoryInstruction.Load>,
    crossinline storeValidator: ModuleValidator<MemoryInstruction.Store>,
    crossinline memoryCopyValidator: ModuleValidator<MemoryInstruction.MemoryCopy>,
    crossinline memoryFillValidator: ModuleValidator<MemoryInstruction.MemoryFill>,
    crossinline memoryGrowValidator: ModuleValidator<MemoryInstruction.MemoryGrow>,
    crossinline memoryInitValidator: ModuleValidator<MemoryInstruction.MemoryInit>,
    crossinline memorySizeValidator: ModuleValidator<MemoryInstruction.MemorySize>,
): Result<Unit, ModuleValidatorError> {
    return when (instruction) {
        is MemoryInstruction.Load -> loadValidator(context, instruction)
        is MemoryInstruction.Store -> storeValidator(context, instruction)
        is MemoryInstruction.DataDrop -> dataDropValidator(context, instruction)
        is MemoryInstruction.MemoryInit -> memoryInitValidator(context, instruction)
        is MemoryInstruction.MemoryCopy -> memoryCopyValidator(context, instruction)
        is MemoryInstruction.MemoryFill -> memoryFillValidator(context, instruction)
        is MemoryInstruction.MemoryGrow -> memoryGrowValidator(context, instruction)
        is MemoryInstruction.MemorySize -> memorySizeValidator(context, instruction)
    }
}
