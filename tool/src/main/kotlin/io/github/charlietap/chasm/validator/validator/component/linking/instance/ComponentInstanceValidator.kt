package io.github.charlietap.chasm.validator.validator.component.linking.instance

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.InstanceDefinition
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun ComponentInstanceValidator(
    context: ComponentValidationContext,
    instance: InstanceDefinition,
): Result<Unit, ComponentValidatorError> = ComponentInstanceValidator(
    context = context,
    instance = instance,
    instantiateValidator = ::ComponentInstantiationValidator,
    inlineExportsValidator = ::ComponentInlineInstanceValidator,
)

internal inline fun ComponentInstanceValidator(
    context: ComponentValidationContext,
    instance: InstanceDefinition,
    crossinline instantiateValidator: (
        ComponentValidationContext,
        InstanceDefinition.Instantiate,
    ) -> Result<Unit, ComponentValidatorError>,
    crossinline inlineExportsValidator: (
        ComponentValidationContext,
        InstanceDefinition.InlineExports,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = when (instance) {
    is InstanceDefinition.Instantiate -> instantiateValidator(context, instance)
    is InstanceDefinition.InlineExports -> inlineExportsValidator(context, instance)
}
