package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.NumberType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiDescriptor
import io.github.charlietap.chasm.type.component.canonical.CanonicalCoreFunctionType
import io.github.charlietap.chasm.type.component.canonical.containsListOrString
import io.github.charlietap.chasm.type.component.canonical.coreValueType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.sortName

internal fun StreamFutureCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
): Result<Unit, ComponentValidatorError> =
    StreamFutureCanonicalDefinitionValidator(
        context = context,
        definition = definition,
        optionsValidator = ::CanonicalOptionValidator,
        componentDefinedType = ::ComponentDefinedType,
        featureValidator = ::RequireComponentFeature,
    )

internal inline fun StreamFutureCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
    crossinline optionsValidator: CanonicalOptionsValidator,
    crossinline componentDefinedType: (
        ComponentValidationContext,
        ComponentTypeIndex,
    ) -> Result<ComponentDefinedType, ComponentValidatorError>,
    crossinline featureValidator: (
        ComponentValidationContext,
        ComponentFeature,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    featureValidator(context, ComponentFeature.Async).bind()
    val typeIndex = definition.typeIndex()
    val definitionType = componentDefinedType(context, typeIndex).bind()
    val valueType = (definitionType as? ComponentDefinedType.Value)?.type
        ?: Err(ComponentValidatorError.SortMismatch("stream or future type", definitionType.sortName())).bind()
    val element = when {
        definition.isStreamDefinition() && valueType is ComponentDefinedValueType.Stream -> valueType.element
        definition.isFutureDefinition() && valueType is ComponentDefinedValueType.Future -> valueType.value
        else -> Err(ComponentValidatorError.SortMismatch(definition.expectedTypeName(), valueType.sortName())).bind()
    }

    val functionType = when (definition) {
        is CanonicalDefinition.StreamNew,
        is CanonicalDefinition.FutureNew,
        -> CanonicalCoreFunctionType(results = listOf(I64))

        is CanonicalDefinition.StreamRead,
        is CanonicalDefinition.StreamWrite,
        is CanonicalDefinition.FutureRead,
        is CanonicalDefinition.FutureWrite,
        -> {
            val options = optionsValidator(
                context,
                definition.options(),
                definition.optionUse(),
                null,
            ).bind()
            if (element != null && options.memory == null) {
                Err(ComponentValidatorError.InvalidCanonicalDefinition("canonical option memory is required")).bind<Unit>()
            }
            if (definition.isReadDefinition() && element?.containsListOrString() == true && options.realloc == null) {
                Err(ComponentValidatorError.InvalidCanonicalDefinition("canonical option realloc is required")).bind<Unit>()
            }
            val address = options.addressType.coreValueType()
            if (definition.isStreamDefinition()) {
                CanonicalCoreFunctionType(
                    params = listOf(I32, address, address),
                    results = listOf(address),
                )
            } else {
                CanonicalCoreFunctionType(
                    params = listOf(I32, address),
                    results = listOf(I32),
                )
            }
        }

        is CanonicalDefinition.StreamCancelRead,
        is CanonicalDefinition.StreamCancelWrite,
        is CanonicalDefinition.FutureCancelRead,
        is CanonicalDefinition.FutureCancelWrite,
        -> {
            if (definition.asyncFlag()) featureValidator(context, ComponentFeature.MoreAsyncBuiltins).bind()
            CanonicalCoreFunctionType(params = listOf(I32), results = listOf(I32))
        }

        is CanonicalDefinition.StreamDropReadable,
        is CanonicalDefinition.StreamDropWritable,
        is CanonicalDefinition.FutureDropReadable,
        is CanonicalDefinition.FutureDropWritable,
        -> CanonicalCoreFunctionType(params = listOf(I32))

        else -> Err(ComponentValidatorError.InvalidCanonicalDefinition("expected a stream or future canonical definition"))
            .bind()
    }
    context.frame.coreFunctions += functionType
    context.frame.canonicalAbi += CanonicalAbiDescriptor(functionType)
}

private fun CanonicalDefinition.typeIndex(): ComponentTypeIndex = when (this) {
    is CanonicalDefinition.StreamNew -> typeIndex
    is CanonicalDefinition.StreamRead -> typeIndex
    is CanonicalDefinition.StreamWrite -> typeIndex
    is CanonicalDefinition.StreamCancelRead -> typeIndex
    is CanonicalDefinition.StreamCancelWrite -> typeIndex
    is CanonicalDefinition.StreamDropReadable -> typeIndex
    is CanonicalDefinition.StreamDropWritable -> typeIndex
    is CanonicalDefinition.FutureNew -> typeIndex
    is CanonicalDefinition.FutureRead -> typeIndex
    is CanonicalDefinition.FutureWrite -> typeIndex
    is CanonicalDefinition.FutureCancelRead -> typeIndex
    is CanonicalDefinition.FutureCancelWrite -> typeIndex
    is CanonicalDefinition.FutureDropReadable -> typeIndex
    is CanonicalDefinition.FutureDropWritable -> typeIndex
    else -> error("not a stream or future canonical definition")
}

private fun CanonicalDefinition.options(): List<io.github.charlietap.chasm.ast.component.CanonicalOption> = when (this) {
    is CanonicalDefinition.StreamRead -> options
    is CanonicalDefinition.StreamWrite -> options
    is CanonicalDefinition.FutureRead -> options
    is CanonicalDefinition.FutureWrite -> options
    else -> error("canonical definition has no options")
}

private fun CanonicalDefinition.optionUse(): CanonicalOptionUse = when (this) {
    is CanonicalDefinition.StreamRead -> CanonicalOptionUse.StreamRead
    is CanonicalDefinition.StreamWrite -> CanonicalOptionUse.StreamWrite
    is CanonicalDefinition.FutureRead -> CanonicalOptionUse.FutureRead
    is CanonicalDefinition.FutureWrite -> CanonicalOptionUse.FutureWrite
    else -> error("canonical definition has no options")
}

private fun CanonicalDefinition.asyncFlag(): Boolean = when (this) {
    is CanonicalDefinition.StreamCancelRead -> async
    is CanonicalDefinition.StreamCancelWrite -> async
    is CanonicalDefinition.FutureCancelRead -> async
    is CanonicalDefinition.FutureCancelWrite -> async
    else -> false
}

private fun CanonicalDefinition.isStreamDefinition(): Boolean = when (this) {
    is CanonicalDefinition.StreamNew,
    is CanonicalDefinition.StreamRead,
    is CanonicalDefinition.StreamWrite,
    is CanonicalDefinition.StreamCancelRead,
    is CanonicalDefinition.StreamCancelWrite,
    is CanonicalDefinition.StreamDropReadable,
    is CanonicalDefinition.StreamDropWritable,
    -> true

    else -> false
}

private fun CanonicalDefinition.isFutureDefinition(): Boolean = !isStreamDefinition()

private fun CanonicalDefinition.isReadDefinition(): Boolean =
    this is CanonicalDefinition.StreamRead || this is CanonicalDefinition.FutureRead

private fun CanonicalDefinition.expectedTypeName(): String =
    if (isStreamDefinition()) "stream type" else "future type"

private val I32 = ValueType.Number(NumberType.I32)
private val I64 = ValueType.Number(NumberType.I64)
