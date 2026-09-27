package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.type.component.LabeledComponentValueType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiContext
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiDescriptor
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.CanonicalFunctionTypeLowering
import io.github.charlietap.chasm.validator.validator.component.type.ComponentValueTypeResolver

internal fun TaskReturnCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition.TaskReturn,
): Result<Unit, ComponentValidatorError> =
    TaskReturnCanonicalDefinitionValidator(
        context = context,
        definition = definition,
        optionsValidator = ::CanonicalOptionValidator,
        valueTypeResolver = ::ComponentValueTypeResolver,
        featureValidator = ::RequireComponentFeature,
    )

internal inline fun TaskReturnCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition.TaskReturn,
    crossinline optionsValidator: CanonicalOptionsValidator,
    crossinline valueTypeResolver: (
        ComponentValidationContext,
        ValueType,
    ) -> Result<ComponentValueType, ComponentValidatorError>,
    crossinline featureValidator: (
        ComponentValidationContext,
        ComponentFeature,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    featureValidator(context, ComponentFeature.Async).bind()
    val resultType = definition.result?.let { type -> valueTypeResolver(context, type).bind() }
    val options = optionsValidator(
        context,
        definition.options,
        CanonicalOptionUse.TaskReturn,
        null,
    ).bind()
    val functionType = ComponentFunctionType(
        params = resultType?.let { type -> listOf(LabeledComponentValueType("result", type)) }.orEmpty(),
        result = null,
        async = false,
    )
    val lowering = CanonicalFunctionTypeLowering(functionType, options, CanonicalAbiContext.Lower)
        ?: Err(ComponentValidatorError.InvalidCanonicalDefinition("task result cannot be flattened")).bind()
    CanonicalAbiRequirementsValidator(lowering, options).bind()
    context.frame.coreFunctions += lowering.type
    context.frame.canonicalAbi += CanonicalAbiDescriptor(
        type = lowering.type,
        requiresMemory = lowering.requiresMemory,
        requiresRealloc = lowering.requiresRealloc,
    )
}
