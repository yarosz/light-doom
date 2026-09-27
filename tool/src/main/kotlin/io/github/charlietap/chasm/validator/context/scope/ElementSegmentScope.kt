package io.github.charlietap.chasm.validator.context.scope

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.module.ElementSegment
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun ElementSegmentScope(
    context: ModuleValidationContext,
    segment: ElementSegment,
    block: (ModuleValidationContext) -> Result<Unit, ModuleValidatorError>,
): Result<Unit, ModuleValidatorError> {
    val previousElementType = context.elementSegmentType
    val previousResultType = context.expressionResultType
    context.elementSegmentType = segment.type
    context.expressionResultType = ResultType(listOf(ValueType.Reference(segment.type)))

    val result = block(context)
    context.elementSegmentType = previousElementType
    context.expressionResultType = previousResultType
    return result
}
