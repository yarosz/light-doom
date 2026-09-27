package io.github.charlietap.chasm.validator.validator.global

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.instruction.Expression
import io.github.charlietap.chasm.ast.module.Global
import io.github.charlietap.chasm.type.GlobalType
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.context.scope.GlobalScope
import io.github.charlietap.chasm.validator.context.scope.Scope
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.validator.instruction.ExpressionValidator
import io.github.charlietap.chasm.validator.validator.type.GlobalTypeValidator

internal fun GlobalValidator(
    context: ModuleValidationContext,
    global: Global,
): Result<Unit, ModuleValidatorError> =
    GlobalValidator(
        context = context,
        global = global,
        scope = ::GlobalScope,
        expressionValidator = ::ExpressionValidator,
        globalTypeValidator = ::GlobalTypeValidator,
    )

internal inline fun GlobalValidator(
    context: ModuleValidationContext,
    global: Global,
    crossinline scope: Scope<Global>,
    crossinline expressionValidator: ModuleValidator<Expression>,
    crossinline globalTypeValidator: ModuleValidator<GlobalType>,
): Result<Unit, ModuleValidatorError> = binding {
    scope(context, global) { scopedContext ->
        binding {
            expressionValidator(scopedContext, global.initExpression).bind()
            globalTypeValidator(scopedContext, global.type).bind()
        }
    }.bind()
}
