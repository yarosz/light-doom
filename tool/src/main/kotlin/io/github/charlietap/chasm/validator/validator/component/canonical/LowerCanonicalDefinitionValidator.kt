package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiContext
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiDescriptor
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.CanonicalAbiOptions
import io.github.charlietap.chasm.validator.type.component.CanonicalFunctionTypeLowering

internal fun LowerCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition.Lower,
): Result<Unit, ComponentValidatorError> =
    LowerCanonicalDefinitionValidator(
        context = context,
        definition = definition,
        optionsValidator = ::CanonicalOptionValidator,
        componentFunction = ::ComponentFunction,
    )

internal inline fun LowerCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition.Lower,
    crossinline optionsValidator: CanonicalOptionsValidator,
    crossinline componentFunction: (
        ComponentValidationContext,
        io.github.charlietap.chasm.ast.component.Index.ComponentFunctionIndex,
    ) -> Result<ComponentFunctionType, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    val functionType = componentFunction(context, definition.functionIndex).bind()
    val options = optionsValidator(
        context,
        definition.options,
        CanonicalOptionUse.Lower,
        functionType,
    ).bind()

    val lowering = CanonicalFunctionTypeLowering(functionType, options, CanonicalAbiContext.Lower)
        ?: Err(ComponentValidatorError.InvalidCanonicalDefinition("component function cannot be flattened")).bind()
    CanonicalAbiRequirementsValidator(lowering, options).bind()
    context.frame.coreFunctions += lowering.type
    context.frame.canonicalAbi += CanonicalAbiDescriptor(
        type = lowering.type,
        requiresMemory = lowering.requiresMemory,
        requiresRealloc = lowering.requiresRealloc,
    )
}
