package io.github.charlietap.chasm.validator.validator.component.linking.value

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ComponentValue
import io.github.charlietap.chasm.ast.component.ComponentValueLiteral
import io.github.charlietap.chasm.ast.component.ExternalType
import io.github.charlietap.chasm.ast.component.ValueBound
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentValueEntry
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentExternalTypeResolver
import io.github.charlietap.chasm.validator.validator.component.type.ComponentExternalTypeResolver as ResolveComponentExternalType

internal fun ComponentValueValidator(
    context: ComponentValidationContext,
    value: ComponentValue,
): Result<Unit, ComponentValidatorError> = ComponentValueValidator(
    context = context,
    value = value,
    externalTypeResolver = ::ResolveComponentExternalType,
    literalValidator = ::ComponentValueLiteralValidator,
)

internal inline fun ComponentValueValidator(
    context: ComponentValidationContext,
    value: ComponentValue,
    crossinline externalTypeResolver: ComponentExternalTypeResolver,
    crossinline literalValidator: (ComponentValueType, ComponentValueLiteral) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    if (ComponentFeature.Values !in context.config.features) {
        Err(ComponentValidatorError.FeatureDisabled(ComponentFeature.Values)).bind<Unit>()
    }

    val externalType = ExternalType.Value(ValueBound.Type(value.type))
    val entity = externalTypeResolver(context, externalType).bind()
    val type = (entity as? ComponentEntityType.Value)?.type
        ?: Err(ComponentValidatorError.SortMismatch("value", entity::class.simpleName.orEmpty())).bind()
    literalValidator(type, value.value).bind()
    context.frame.values += ComponentValueEntry(type)
}
