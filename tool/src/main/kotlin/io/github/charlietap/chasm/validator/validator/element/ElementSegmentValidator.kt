package io.github.charlietap.chasm.validator.validator.element

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.instruction.Expression
import io.github.charlietap.chasm.ast.module.ElementSegment
import io.github.charlietap.chasm.type.ReferenceType
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.context.scope.ElementSegmentScope
import io.github.charlietap.chasm.validator.context.scope.Scope
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.validator.instruction.ExpressionValidator
import io.github.charlietap.chasm.validator.validator.type.ReferenceTypeValidator

internal fun ElementSegmentValidator(
    context: ModuleValidationContext,
    segment: ElementSegment,
): Result<Unit, ModuleValidatorError> =
    ElementSegmentValidator(
        context = context,
        segment = segment,
        scope = ::ElementSegmentScope,
        referenceTypeValidator = ::ReferenceTypeValidator,
        segmentModeValidator = ::ElementSegmentModeValidator,
        expressionValidator = ::ExpressionValidator,
    )

internal inline fun ElementSegmentValidator(
    context: ModuleValidationContext,
    segment: ElementSegment,
    crossinline scope: Scope<ElementSegment>,
    crossinline referenceTypeValidator: ModuleValidator<ReferenceType>,
    crossinline segmentModeValidator: ModuleValidator<ElementSegment.Mode>,
    crossinline expressionValidator: ModuleValidator<Expression>,
): Result<Unit, ModuleValidatorError> = binding {
    scope(context, segment) { scopedContext ->
        binding {
            referenceTypeValidator(scopedContext, segment.type).bind()
            segmentModeValidator(scopedContext, segment.mode).bind()

            segment.initExpressions.forEach { expression ->
                expressionValidator(scopedContext, expression).bind()
            }
        }
    }.bind()
}
