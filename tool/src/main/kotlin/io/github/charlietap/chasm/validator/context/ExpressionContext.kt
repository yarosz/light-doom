package io.github.charlietap.chasm.validator.context

import io.github.charlietap.chasm.type.ResultType

internal interface ExpressionContext {
    var expressionResultType: ResultType?
}

internal class ExpressionContextImpl(
    override var expressionResultType: ResultType? = null,
) : ExpressionContext
