package io.github.charlietap.chasm.validator.context.scope

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import io.github.charlietap.chasm.ast.module.DataSegment
import io.github.charlietap.chasm.type.AddressType
import io.github.charlietap.chasm.type.NumberType
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.ext.memoryType

internal fun ActiveDataSegmentScope(
    context: ModuleValidationContext,
    mode: DataSegment.Mode.Active,
    block: (ModuleValidationContext) -> Result<Unit, ModuleValidatorError>,
): Result<Unit, ModuleValidatorError> {
    val memory = context.memoryType(mode.memoryIndex).getOrElse { error ->
        return Err(error)
    }
    val valueType = when (memory.addressType) {
        AddressType.I32 -> ValueType.Number(NumberType.I32)
        AddressType.I64 -> ValueType.Number(NumberType.I64)
    }
    val previousResultType = context.expressionResultType
    context.expressionResultType = ResultType(listOf(valueType))

    val result = block(context)
    context.expressionResultType = previousResultType
    return result
}
