package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiContext
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiDescriptor
import io.github.charlietap.chasm.type.component.canonical.CanonicalCoreFunctionType
import io.github.charlietap.chasm.type.ext.functionType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.CanonicalAbiOptions
import io.github.charlietap.chasm.validator.type.component.CanonicalFunctionTypeLowering
import io.github.charlietap.chasm.validator.type.component.sortName

internal fun LiftCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition.Lift,
): Result<Unit, ComponentValidatorError> =
    LiftCanonicalDefinitionValidator(
        context = context,
        definition = definition,
        optionsValidator = ::CanonicalOptionValidator,
        coreFunction = ::CoreFunction,
        coreFunctionTypeValidator = ::RequireCoreFunctionType,
    )

internal inline fun LiftCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition.Lift,
    crossinline optionsValidator: CanonicalOptionsValidator,
    crossinline coreFunction: (
        ComponentValidationContext,
        io.github.charlietap.chasm.ast.module.Index.FunctionIndex,
    ) -> Result<io.github.charlietap.chasm.type.DefinedType, ComponentValidatorError>,
    crossinline coreFunctionTypeValidator: (
        io.github.charlietap.chasm.type.DefinedType,
        io.github.charlietap.chasm.type.DefinedType,
        String,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    val definedType = context.frame.types.getOrNull(definition.typeIndex.idx.toInt())?.type
        ?: Err(ComponentValidatorError.UnknownIndex("component type", definition.typeIndex.idx)).bind()
    val functionType = (definedType as? ComponentDefinedType.Function)?.type
        ?: Err(ComponentValidatorError.SortMismatch("component function type", definedType.sortName())).bind()
    val callee = coreFunction(context, definition.functionIndex).bind()
    val options = optionsValidator(
        context,
        definition.options,
        CanonicalOptionUse.Lift,
        functionType,
    ).bind()

    val lowering = CanonicalFunctionTypeLowering(functionType, options, CanonicalAbiContext.Lift)
        ?: Err(ComponentValidatorError.InvalidCanonicalDefinition("component function cannot be flattened")).bind()
    CanonicalAbiRequirementsValidator(lowering, options).bind()
    val expected = lowering.type
    coreFunctionTypeValidator(
        callee,
        expected,
        "canon lift uses a core function with an incorrect signature",
    ).bind()

    options.postReturn?.let { postReturn ->
        val expectedPostReturn = CanonicalCoreFunctionType(
            params = expected.functionType()?.results?.types.orEmpty(),
        )
        coreFunctionTypeValidator(
            postReturn,
            expectedPostReturn,
            "post-return uses a core function with an incorrect signature",
        ).bind()
    }

    context.frame.functions += functionType
    context.frame.canonicalAbi += CanonicalAbiDescriptor(
        type = lowering.type,
        requiresMemory = lowering.requiresMemory,
        requiresRealloc = lowering.requiresRealloc,
    )
}
