package io.github.charlietap.chasm.validator.validator.instruction.table

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.instruction.TableInstruction
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.ext.popTableAddress
import io.github.charlietap.chasm.validator.ext.push
import io.github.charlietap.chasm.validator.ext.tableType

internal fun TableGetInstructionValidator(
    context: ModuleValidationContext,
    instruction: TableInstruction.TableGet,
): Result<Unit, ModuleValidatorError> = binding {

    val tableType = context.tableType(instruction.tableIdx).bind()

    context.popTableAddress(instruction.tableIdx).bind()
    context.push(ValueType.Reference(tableType.referenceType))
}
