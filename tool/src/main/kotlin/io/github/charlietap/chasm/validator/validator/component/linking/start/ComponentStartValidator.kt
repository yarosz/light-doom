package io.github.charlietap.chasm.validator.validator.component.linking.start

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.StartDefinition
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentValueEntry
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentEntityTypeMatcher
import io.github.charlietap.chasm.validator.validator.component.linking.type.componentIndex
import io.github.charlietap.chasm.validator.validator.component.linking.value.ComponentValueConsumer
import io.github.charlietap.chasm.validator.type.component.ComponentEntityTypeMatcher as MatchComponentEntityType

internal fun ComponentStartValidator(
    context: ComponentValidationContext,
    start: StartDefinition,
): Result<Unit, ComponentValidatorError> = ComponentStartValidator(
    context = context,
    start = start,
    entityTypeMatcher = ::MatchComponentEntityType,
)

internal inline fun ComponentStartValidator(
    context: ComponentValidationContext,
    start: StartDefinition,
    crossinline entityTypeMatcher: ComponentEntityTypeMatcher,
): Result<Unit, ComponentValidatorError> = binding {
    if (ComponentFeature.Values !in context.config.features) {
        Err(ComponentValidatorError.FeatureDisabled(ComponentFeature.Values)).bind<Unit>()
    }
    if (context.frame.hasStart) {
        Err(ComponentValidatorError.InvalidComponent("component cannot have more than one start definition")).bind<Unit>()
    }

    val function = context.frame.functions.componentIndex(start.functionIndex.idx)
        ?: Err(ComponentValidatorError.UnknownIndex("function", start.functionIndex.idx)).bind()
    if (function.params.size != start.args.size) {
        Err(ComponentValidatorError.TypeMismatch("${function.params.size} start arguments", "${start.args.size}")).bind<Unit>()
    }
    val expectedResultCount = if (function.result == null) 0u else 1u
    if (start.resultCount != expectedResultCount) {
        Err(ComponentValidatorError.TypeMismatch("$expectedResultCount start results", "${start.resultCount}")).bind<Unit>()
    }

    for (index in function.params.indices) {
        val argumentType = ComponentValueConsumer(context, start.args[index]).bind()
        entityTypeMatcher(
            context,
            ComponentEntityType.Value(argumentType),
            ComponentEntityType.Value(function.params[index].type),
        ).bind()
    }

    function.result?.let { result -> context.frame.values += ComponentValueEntry(result) }
    context.frame.hasStart = true
}
