package io.github.charlietap.chasm.validator.validator.element

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.instruction.Expression
import io.github.charlietap.chasm.ast.module.ElementSegment
import io.github.charlietap.chasm.type.ReferenceType
import io.github.charlietap.chasm.type.matching.ReferenceTypeMatcher
import io.github.charlietap.chasm.type.matching.TypeMatcher
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.context.scope.ActiveElementSegmentModeScope
import io.github.charlietap.chasm.validator.context.scope.Scope
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.error.TypeValidatorError
import io.github.charlietap.chasm.validator.ext.elementSegmentType
import io.github.charlietap.chasm.validator.ext.tableType
import io.github.charlietap.chasm.validator.validator.instruction.ExpressionValidator

internal fun ElementSegmentModeValidator(
    context: ModuleValidationContext,
    mode: ElementSegment.Mode,
): Result<Unit, ModuleValidatorError> =
    ElementSegmentModeValidator(
        context = context,
        mode = mode,
        scope = ::ActiveElementSegmentModeScope,
        expressionValidator = ::ExpressionValidator,
        typeMatcher = ::ReferenceTypeMatcher,
    )

internal inline fun ElementSegmentModeValidator(
    context: ModuleValidationContext,
    mode: ElementSegment.Mode,
    crossinline scope: Scope<ElementSegment.Mode.Active>,
    crossinline expressionValidator: ModuleValidator<Expression>,
    crossinline typeMatcher: TypeMatcher<ReferenceType>,
): Result<Unit, ModuleValidatorError> = binding {

    when (mode) {
        is ElementSegment.Mode.Active -> {

            val tableType = context.tableType(mode.tableIndex).bind()
            val segmentType = context.elementSegmentType().bind()

            if (!typeMatcher(segmentType, tableType.referenceType, context)) {
                Err(TypeValidatorError.TypeMismatch).bind<Unit>()
            }

            scope(context, mode) { scopedContext ->
                expressionValidator(scopedContext, mode.offsetExpr)
            }.bind()
        }
        ElementSegment.Mode.Declarative -> Unit
        ElementSegment.Mode.Passive -> Unit
    }
}
