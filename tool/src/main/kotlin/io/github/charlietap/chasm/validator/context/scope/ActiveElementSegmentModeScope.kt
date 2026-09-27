package io.github.charlietap.chasm.validator.context.scope

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import io.github.charlietap.chasm.ast.module.ElementSegment
import io.github.charlietap.chasm.type.AddressType
import io.github.charlietap.chasm.type.NumberType
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.ext.tableType

internal fun ActiveElementSegmentModeScope(
    context: ModuleValidationContext,
    mode: ElementSegment.Mode.Active,
    block: (ModuleValidationContext) -> Result<Unit, ModuleValidatorError>,
): Result<Unit, ModuleValidatorError> {
    val tableType = context.tableType(mode.tableIndex).getOrElse { error ->
        return Err(error)
    }
    val numberType = when (tableType.addressType) {
        AddressType.I32 -> NumberType.I32
        AddressType.I64 -> NumberType.I64
    }
    val previousResultType = context.expressionResultType
    context.expressionResultType = ResultType(listOf(ValueType.Number(numberType)))

    val result = block(context)
    context.expressionResultType = previousResultType
    return result
}
