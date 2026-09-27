package io.github.charlietap.chasm.validator.validator.instruction.table

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.instruction.TableInstruction
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun TableInstructionValidator(
    context: ModuleValidationContext,
    instruction: TableInstruction,
): Result<Unit, ModuleValidatorError> =
    TableInstructionValidator(
        context = context,
        instruction = instruction,
        elementDropValidator = ::ElementDropInstructionValidator,
        tableCopyValidator = ::TableCopyInstructionValidator,
        tableFillValidator = ::TableFillInstructionValidator,
        tableGetValidator = ::TableGetInstructionValidator,
        tableSetValidator = ::TableSetInstructionValidator,
        tableGrowValidator = ::TableGrowInstructionValidator,
        tableInitValidator = ::TableInitInstructionValidator,
        tableSizeValidator = ::TableSizeInstructionValidator,
    )

internal inline fun TableInstructionValidator(
    context: ModuleValidationContext,
    instruction: TableInstruction,
    crossinline elementDropValidator: ModuleValidator<TableInstruction.ElemDrop>,
    crossinline tableCopyValidator: ModuleValidator<TableInstruction.TableCopy>,
    crossinline tableFillValidator: ModuleValidator<TableInstruction.TableFill>,
    crossinline tableGetValidator: ModuleValidator<TableInstruction.TableGet>,
    crossinline tableSetValidator: ModuleValidator<TableInstruction.TableSet>,
    crossinline tableGrowValidator: ModuleValidator<TableInstruction.TableGrow>,
    crossinline tableInitValidator: ModuleValidator<TableInstruction.TableInit>,
    crossinline tableSizeValidator: ModuleValidator<TableInstruction.TableSize>,
): Result<Unit, ModuleValidatorError> {
    return when (instruction) {
        is TableInstruction.ElemDrop -> elementDropValidator(context, instruction)
        is TableInstruction.TableCopy -> tableCopyValidator(context, instruction)
        is TableInstruction.TableFill -> tableFillValidator(context, instruction)
        is TableInstruction.TableGet -> tableGetValidator(context, instruction)
        is TableInstruction.TableGrow -> tableGrowValidator(context, instruction)
        is TableInstruction.TableInit -> tableInitValidator(context, instruction)
        is TableInstruction.TableSet -> tableSetValidator(context, instruction)
        is TableInstruction.TableSize -> tableSizeValidator(context, instruction)
    }
}
