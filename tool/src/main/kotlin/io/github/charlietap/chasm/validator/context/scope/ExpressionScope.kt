package io.github.charlietap.chasm.validator.context.scope

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.instruction.Expression
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun ExpressionScope(
    context: ModuleValidationContext,
    expression: Expression,
    block: (ModuleValidationContext) -> Result<Unit, ModuleValidatorError>,
): Result<Unit, ModuleValidatorError> {

    val labels = context.functionContext.labels
    val locals = context.functionContext.locals
    val expressionResult = context.functionContext.result
    val operands = context.functionContext.operands

    val result = block(context)

    context.labels = labels
    context.locals = locals
    context.result = expressionResult
    context.operands = operands

    return result
}
