package io.github.charlietap.chasm.validator.validator.component

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Component
import io.github.charlietap.chasm.ast.component.Definition
import io.github.charlietap.chasm.validator.ComponentValidator
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun ComponentValidator(
    context: ComponentValidationContext,
    component: Component,
): Result<Unit, ComponentValidatorError> = ComponentValidator(
    context = context,
    component = component,
    definitionValidator = ::ComponentDefinitionValidator,
)

internal inline fun ComponentValidator(
    context: ComponentValidationContext,
    component: Component,
    crossinline definitionValidator: ComponentValidator<Definition>,
): Result<Unit, ComponentValidatorError> = binding {
    component.definitions.forEach { definition ->
        definitionValidator(context, definition).bind()
    }
    context.frame.requireAllValuesConsumed().bind()
}
