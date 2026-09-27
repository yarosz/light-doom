package io.github.charlietap.chasm.validator.validator.instruction.atomic

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.instruction.AtomicMemoryInstruction
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun AtomicMemoryInstructionValidator(
    context: ModuleValidationContext,
    instruction: AtomicMemoryInstruction,
): Result<Unit, ModuleValidatorError> =
    AtomicMemoryInstructionValidator(
        context = context,
        instruction = instruction,
        fenceValidator = ::AtomicFenceInstructionValidator,
        notifyValidator = ::AtomicNotifyInstructionValidator,
        i32WaitValidator = ::AtomicI32WaitInstructionValidator,
        i64WaitValidator = ::AtomicI64WaitInstructionValidator,
        loadValidator = ::AtomicLoadInstructionValidator,
        storeValidator = ::AtomicStoreInstructionValidator,
        readModifyWriteValidator = ::AtomicReadModifyWriteInstructionValidator,
        compareExchangeValidator = ::AtomicCompareExchangeInstructionValidator,
    )

internal inline fun AtomicMemoryInstructionValidator(
    context: ModuleValidationContext,
    instruction: AtomicMemoryInstruction,
    crossinline fenceValidator: ModuleValidator<AtomicMemoryInstruction.Fence>,
    crossinline notifyValidator: ModuleValidator<AtomicMemoryInstruction.Notify>,
    crossinline i32WaitValidator: ModuleValidator<AtomicMemoryInstruction.I32Wait>,
    crossinline i64WaitValidator: ModuleValidator<AtomicMemoryInstruction.I64Wait>,
    crossinline loadValidator: ModuleValidator<AtomicMemoryInstruction.Load>,
    crossinline storeValidator: ModuleValidator<AtomicMemoryInstruction.Store>,
    crossinline readModifyWriteValidator: ModuleValidator<AtomicMemoryInstruction.ReadModifyWrite>,
    crossinline compareExchangeValidator: ModuleValidator<AtomicMemoryInstruction.CompareExchange>,
): Result<Unit, ModuleValidatorError> = binding {
    when (instruction) {
        AtomicMemoryInstruction.Fence -> fenceValidator(context, AtomicMemoryInstruction.Fence).bind()
        is AtomicMemoryInstruction.Notify -> notifyValidator(context, instruction).bind()
        is AtomicMemoryInstruction.I32Wait -> i32WaitValidator(context, instruction).bind()
        is AtomicMemoryInstruction.I64Wait -> i64WaitValidator(context, instruction).bind()
        is AtomicMemoryInstruction.Load -> loadValidator(context, instruction).bind()
        is AtomicMemoryInstruction.Store -> storeValidator(context, instruction).bind()
        is AtomicMemoryInstruction.ReadModifyWrite -> readModifyWriteValidator(context, instruction).bind()
        is AtomicMemoryInstruction.CompareExchange -> compareExchangeValidator(context, instruction).bind()
    }
}
