package io.github.charlietap.chasm.validator.validator.component.linking.component

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import io.github.charlietap.chasm.ast.component.Component
import io.github.charlietap.chasm.type.component.ComponentItemType
import io.github.charlietap.chasm.type.component.ComponentScopeTypes
import io.github.charlietap.chasm.validator.ComponentValidator
import io.github.charlietap.chasm.validator.context.component.ComponentScopeKind
import io.github.charlietap.chasm.validator.context.component.ComponentScopeTypesFactory
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.ComponentValidator as ValidateComponent

internal fun NestedComponentValidator(
    context: ComponentValidationContext,
    component: Component,
): Result<Unit, ComponentValidatorError> = NestedComponentValidator(
    context = context,
    component = component,
    componentValidator = ::ValidateComponent,
)

internal inline fun NestedComponentValidator(
    context: ComponentValidationContext,
    component: Component,
    crossinline componentValidator: ComponentValidator<Component>,
): Result<Unit, ComponentValidatorError> {
    if (!context.canPush()) {
        return Err(ComponentValidatorError.InvalidComponent(COMPONENT_NESTING_TOO_DEEP))
    }
    context.push(ComponentScopeKind.Component)
    var componentTypes: ComponentScopeTypes? = null
    val result = componentValidator(context, component).fold(
        success = {
            componentTypes = ComponentScopeTypesFactory(context.frame)
            Ok(Unit)
        },
        failure = ::Err,
    )
    context.pop()
    componentTypes?.let { types -> context.frame.components += ComponentItemType(types.type, types) }
    return result
}

private const val COMPONENT_NESTING_TOO_DEEP = "component nesting is too deep"
