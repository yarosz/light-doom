package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.FunctionType
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.type.component.LabeledComponentValueType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.containsBorrow

internal fun ComponentFunctionTypeResolver(
    context: ComponentValidationContext,
    type: FunctionType,
): Result<ComponentFunctionType, ComponentValidatorError> = ComponentFunctionTypeResolver(
    context = context,
    type = type,
    valueTypeResolver = ::ComponentValueTypeResolver,
)

internal inline fun ComponentFunctionTypeResolver(
    context: ComponentValidationContext,
    type: FunctionType,
    crossinline valueTypeResolver: ComponentTypeResolver<ValueType, ComponentValueType>,
): Result<ComponentFunctionType, ComponentValidatorError> = binding {
    if (type.async && ComponentFeature.Async !in context.config.features) {
        Err(ComponentValidatorError.FeatureDisabled(ComponentFeature.Async)).bind<Unit>()
    }

    StronglyUniqueLabelValidator(type.params) { parameter -> parameter.label.name }.bind()

    val params = type.params.map { param ->
        LabeledComponentValueType(
            label = param.label.name,
            type = valueTypeResolver(context, param.type).bind(),
        )
    }
    val result = type.result?.let { resultType -> valueTypeResolver(context, resultType).bind() }

    if (result?.containsBorrow(context.frame::componentTypeInfo) == true) {
        Err(ComponentValidatorError.InvalidType("function result cannot contain a borrow type")).bind<Unit>()
    }

    ComponentFunctionType(
        params = params,
        result = result,
        async = type.async,
    )
}
