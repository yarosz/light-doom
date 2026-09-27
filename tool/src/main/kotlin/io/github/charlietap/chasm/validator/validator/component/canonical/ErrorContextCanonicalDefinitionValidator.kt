package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiDescriptor
import io.github.charlietap.chasm.type.component.canonical.CanonicalCoreFunctionType
import io.github.charlietap.chasm.type.component.canonical.coreValueType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun ErrorContextCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
): Result<Unit, ComponentValidatorError> =
    ErrorContextCanonicalDefinitionValidator(
        context = context,
        definition = definition,
        optionsValidator = ::CanonicalOptionValidator,
    )

internal inline fun ErrorContextCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
    crossinline optionsValidator: CanonicalOptionsValidator,
): Result<Unit, ComponentValidatorError> = binding {
    val functionType = when (definition) {
        is CanonicalDefinition.ErrorContextNew -> {
            val options = optionsValidator(
                context,
                definition.options,
                CanonicalOptionUse.ErrorContextNew,
                null,
            ).bind()
            if (options.memory == null) {
                Err(ComponentValidatorError.InvalidCanonicalDefinition("canonical option memory is required")).bind<Unit>()
            }
            val address = options.addressType.coreValueType()
            CanonicalCoreFunctionType(
                params = listOf(address, address),
                results = listOf(I32),
            )
        }

        is CanonicalDefinition.ErrorContextDebugMessage -> {
            val options = optionsValidator(
                context,
                definition.options,
                CanonicalOptionUse.ErrorContextDebugMessage,
                null,
            ).bind()
            if (options.memory == null) {
                Err(ComponentValidatorError.InvalidCanonicalDefinition("canonical option memory is required")).bind<Unit>()
            }
            if (options.realloc == null) {
                Err(ComponentValidatorError.InvalidCanonicalDefinition("canonical option realloc is required")).bind<Unit>()
            }
            CanonicalCoreFunctionType(
                params = listOf(I32, options.addressType.coreValueType()),
            )
        }

        CanonicalDefinition.ErrorContextDrop -> CanonicalCoreFunctionType(params = listOf(I32))
        else -> Err(ComponentValidatorError.InvalidCanonicalDefinition("expected an error-context canonical definition"))
            .bind()
    }
    context.frame.coreFunctions += functionType
    context.frame.canonicalAbi += CanonicalAbiDescriptor(functionType)
}

private val I32 = io.github.charlietap.chasm.type.ValueType.Number(io.github.charlietap.chasm.type.NumberType.I32)
