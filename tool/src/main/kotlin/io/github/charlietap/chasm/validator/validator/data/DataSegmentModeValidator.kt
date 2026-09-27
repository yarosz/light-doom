package io.github.charlietap.chasm.validator.validator.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.instruction.Expression
import io.github.charlietap.chasm.ast.module.DataSegment
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.context.scope.ActiveDataSegmentScope
import io.github.charlietap.chasm.validator.context.scope.Scope
import io.github.charlietap.chasm.validator.error.DataSegmentValidatorError
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.validator.instruction.ExpressionValidator

internal fun DataSegmentModeValidator(
    context: ModuleValidationContext,
    mode: DataSegment.Mode,
): Result<Unit, ModuleValidatorError> =
    DataSegmentModeValidator(
        context = context,
        mode = mode,
        scope = ::ActiveDataSegmentScope,
        expressionValidator = ::ExpressionValidator,
    )

internal inline fun DataSegmentModeValidator(
    context: ModuleValidationContext,
    mode: DataSegment.Mode,
    crossinline scope: Scope<DataSegment.Mode.Active>,
    crossinline expressionValidator: ModuleValidator<Expression>,
): Result<Unit, ModuleValidatorError> = binding {
    when (mode) {
        is DataSegment.Mode.Active -> {
            if (mode.memoryIndex.idx.toInt() !in context.memories.indices) {
                Err(DataSegmentValidatorError.UnknownMemory).bind<Unit>()
            }
            scope(context, mode) { scopedContext ->
                expressionValidator(scopedContext, mode.offset)
            }.bind()
        }
        DataSegment.Mode.Passive -> Unit
    }
}
