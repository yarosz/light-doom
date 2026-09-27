package io.github.charlietap.chasm.validator.validator.instruction.aggregate

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.instruction.AggregateInstruction
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun AggregateInstructionValidator(
    context: ModuleValidationContext,
    instruction: AggregateInstruction,
): Result<Unit, ModuleValidatorError> =
    AggregateInstructionValidator(
        context = context,
        instruction = instruction,
        refI31Validator = ::RefI31InstructionValidator,
        anyConvertExternValidator = ::AnyConvertExternInstructionValidator,
        arrayCopyValidator = ::ArrayCopyInstructionValidator,
        arrayFillValidator = ::ArrayFillInstructionValidator,
        arrayGetValidator = ::ArrayGetInstructionValidator,
        arrayGetSignedValidator = ::ArrayGetSignedInstructionValidator,
        arrayGetUnsignedValidator = ::ArrayGetUnsignedInstructionValidator,
        arrayInitDataValidator = ::ArrayInitDataInstructionValidator,
        arrayInitElementValidator = ::ArrayInitElemInstructionValidator,
        arrayLenValidator = ::ArrayLenInstructionValidator,
        arrayNewValidator = ::ArrayNewInstructionValidator,
        arrayNewDataValidator = ::ArrayNewDataInstructionValidator,
        arrayNewDefaultValidator = ::ArrayNewDefaultInstructionValidator,
        arrayNewElementValidator = ::ArrayNewElementInstructionValidator,
        arrayNewFixedValidator = ::ArrayNewFixedInstructionValidator,
        arraySetValidator = ::ArraySetInstructionValidator,
        externConvertAnyValidator = ::ExternConvertAnyInstructionValidator,
        structGetValidator = ::StructGetInstructionValidator,
        structGetSignedValidator = ::StructGetSignedInstructionValidator,
        structGetUnsignedValidator = ::StructGetUnsignedInstructionValidator,
        structNewValidator = ::StructNewInstructionValidator,
        structNewDefaultValidator = ::StructNewDefaultInstructionValidator,
        structSetValidator = ::StructSetInstructionValidator,
        i31GetSignedValidator = ::I31GetSignedInstructionValidator,
        i31GetUnsignedValidator = ::I31GetUnsignedInstructionValidator,
    )

internal inline fun AggregateInstructionValidator(
    context: ModuleValidationContext,
    instruction: AggregateInstruction,
    crossinline refI31Validator: ModuleValidator<AggregateInstruction.RefI31>,
    crossinline anyConvertExternValidator: ModuleValidator<AggregateInstruction.AnyConvertExtern>,
    crossinline arrayCopyValidator: ModuleValidator<AggregateInstruction.ArrayCopy>,
    crossinline arrayFillValidator: ModuleValidator<AggregateInstruction.ArrayFill>,
    crossinline arrayGetValidator: ModuleValidator<AggregateInstruction.ArrayGet>,
    crossinline arrayGetSignedValidator: ModuleValidator<AggregateInstruction.ArrayGetSigned>,
    crossinline arrayGetUnsignedValidator: ModuleValidator<AggregateInstruction.ArrayGetUnsigned>,
    crossinline arrayInitDataValidator: ModuleValidator<AggregateInstruction.ArrayInitData>,
    crossinline arrayInitElementValidator: ModuleValidator<AggregateInstruction.ArrayInitElement>,
    crossinline arrayLenValidator: ModuleValidator<AggregateInstruction.ArrayLen>,
    crossinline arrayNewValidator: ModuleValidator<AggregateInstruction.ArrayNew>,
    crossinline arrayNewDataValidator: ModuleValidator<AggregateInstruction.ArrayNewData>,
    crossinline arrayNewDefaultValidator: ModuleValidator<AggregateInstruction.ArrayNewDefault>,
    crossinline arrayNewElementValidator: ModuleValidator<AggregateInstruction.ArrayNewElement>,
    crossinline arrayNewFixedValidator: ModuleValidator<AggregateInstruction.ArrayNewFixed>,
    crossinline arraySetValidator: ModuleValidator<AggregateInstruction.ArraySet>,
    crossinline externConvertAnyValidator: ModuleValidator<AggregateInstruction.ExternConvertAny>,
    crossinline structGetValidator: ModuleValidator<AggregateInstruction.StructGet>,
    crossinline structGetSignedValidator: ModuleValidator<AggregateInstruction.StructGetSigned>,
    crossinline structGetUnsignedValidator: ModuleValidator<AggregateInstruction.StructGetUnsigned>,
    crossinline structNewValidator: ModuleValidator<AggregateInstruction.StructNew>,
    crossinline structNewDefaultValidator: ModuleValidator<AggregateInstruction.StructNewDefault>,
    crossinline structSetValidator: ModuleValidator<AggregateInstruction.StructSet>,
    crossinline i31GetSignedValidator: ModuleValidator<AggregateInstruction.I31GetSigned>,
    crossinline i31GetUnsignedValidator: ModuleValidator<AggregateInstruction.I31GetUnsigned>,
): Result<Unit, ModuleValidatorError> {
    return when (instruction) {
        is AggregateInstruction.AnyConvertExtern -> anyConvertExternValidator(context, instruction)
        is AggregateInstruction.ArrayCopy -> arrayCopyValidator(context, instruction)
        is AggregateInstruction.ArrayFill -> arrayFillValidator(context, instruction)
        is AggregateInstruction.ArrayGet -> arrayGetValidator(context, instruction)
        is AggregateInstruction.ArrayGetSigned -> arrayGetSignedValidator(context, instruction)
        is AggregateInstruction.ArrayGetUnsigned -> arrayGetUnsignedValidator(context, instruction)
        is AggregateInstruction.ArrayInitData -> arrayInitDataValidator(context, instruction)
        is AggregateInstruction.ArrayInitElement -> arrayInitElementValidator(context, instruction)
        is AggregateInstruction.ArrayLen -> arrayLenValidator(context, instruction)
        is AggregateInstruction.ArrayNew -> arrayNewValidator(context, instruction)
        is AggregateInstruction.ArrayNewData -> arrayNewDataValidator(context, instruction)
        is AggregateInstruction.ArrayNewDefault -> arrayNewDefaultValidator(context, instruction)
        is AggregateInstruction.ArrayNewElement -> arrayNewElementValidator(context, instruction)
        is AggregateInstruction.ArrayNewFixed -> arrayNewFixedValidator(context, instruction)
        is AggregateInstruction.ArraySet -> arraySetValidator(context, instruction)
        is AggregateInstruction.ExternConvertAny -> externConvertAnyValidator(context, instruction)
        is AggregateInstruction.I31GetSigned -> i31GetSignedValidator(context, instruction)
        is AggregateInstruction.I31GetUnsigned -> i31GetUnsignedValidator(context, instruction)
        is AggregateInstruction.RefI31 -> refI31Validator(context, instruction)
        is AggregateInstruction.StructGet -> structGetValidator(context, instruction)
        is AggregateInstruction.StructGetSigned -> structGetSignedValidator(context, instruction)
        is AggregateInstruction.StructGetUnsigned -> structGetUnsignedValidator(context, instruction)
        is AggregateInstruction.StructNew -> structNewValidator(context, instruction)
        is AggregateInstruction.StructNewDefault -> structNewDefaultValidator(context, instruction)
        is AggregateInstruction.StructSet -> structSetValidator(context, instruction)
    }
}
