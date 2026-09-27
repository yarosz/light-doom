package io.github.charlietap.chasm.validator.validator.instruction.reference

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.instruction.ReferenceInstruction
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun ReferenceInstructionValidator(
    context: ModuleValidationContext,
    instruction: ReferenceInstruction,
): Result<Unit, ModuleValidatorError> =
    ReferenceInstructionValidator(
        context = context,
        instruction = instruction,
        refFuncValidator = ::RefFuncInstructionValidator,
        refIsNullValidator = ::RefIsNullInstructionValidator,
        refAsNonNullValidator = ::RefAsNonNullInstructionValidator,
        refCastValidator = ::RefCastInstructionValidator,
        refEqValidator = ::RefEqInstructionValidator,
        refNullValidator = ::RefNullInstructionValidator,
        refTestValidator = ::RefTestInstructionValidator,
    )

internal inline fun ReferenceInstructionValidator(
    context: ModuleValidationContext,
    instruction: ReferenceInstruction,
    crossinline refFuncValidator: ModuleValidator<ReferenceInstruction.RefFunc>,
    crossinline refIsNullValidator: ModuleValidator<ReferenceInstruction.RefIsNull>,
    crossinline refAsNonNullValidator: ModuleValidator<ReferenceInstruction.RefAsNonNull>,
    crossinline refCastValidator: ModuleValidator<ReferenceInstruction.RefCast>,
    crossinline refEqValidator: ModuleValidator<ReferenceInstruction.RefEq>,
    crossinline refNullValidator: ModuleValidator<ReferenceInstruction.RefNull>,
    crossinline refTestValidator: ModuleValidator<ReferenceInstruction.RefTest>,
): Result<Unit, ModuleValidatorError> {
    return when (instruction) {
        is ReferenceInstruction.RefFunc -> refFuncValidator(context, instruction)
        is ReferenceInstruction.RefAsNonNull -> refAsNonNullValidator(context, instruction)
        is ReferenceInstruction.RefCast -> refCastValidator(context, instruction)
        is ReferenceInstruction.RefEq -> refEqValidator(context, instruction)
        is ReferenceInstruction.RefIsNull -> refIsNullValidator(context, instruction)
        is ReferenceInstruction.RefNull -> refNullValidator(context, instruction)
        is ReferenceInstruction.RefTest -> refTestValidator(context, instruction)
    }
}
