package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.type.AddressType
import io.github.charlietap.chasm.type.NumberType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiDescriptor
import io.github.charlietap.chasm.type.component.canonical.CanonicalCoreFunctionType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.sortName

internal fun ResourceCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
): Result<Unit, ComponentValidatorError> =
    ResourceCanonicalDefinitionValidator(
        context = context,
        definition = definition,
        componentDefinedType = ::ComponentDefinedType,
        memory64Validator = ::RequireMemory64,
    )

internal inline fun ResourceCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
    crossinline componentDefinedType: (
        ComponentValidationContext,
        ComponentTypeIndex,
    ) -> Result<ComponentDefinedType, ComponentValidatorError>,
    crossinline memory64Validator: (
        ComponentValidationContext,
        AddressType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    val typeIndex = when (definition) {
        is CanonicalDefinition.ResourceNew -> definition.typeIndex
        is CanonicalDefinition.ResourceDrop -> definition.typeIndex
        is CanonicalDefinition.ResourceRep -> definition.typeIndex
        else -> Err(ComponentValidatorError.InvalidCanonicalDefinition("expected a resource canonical definition")).bind()
    }
    val definedType = componentDefinedType(context, typeIndex).bind()
    val resource = definedType as? ComponentDefinedType.Resource
        ?: Err(ComponentValidatorError.SortMismatch("resource type", definedType.sortName())).bind()

    val representation = if (definition is CanonicalDefinition.ResourceDrop) {
        null
    } else {
        context.frame.localResourceRepresentations[resource.id]
            ?: Err(
                ComponentValidatorError.InvalidCanonicalDefinition(
                    "resource.new and resource.rep require a locally-defined resource type",
                ),
            ).bind()
    }
    if (representation == I64) memory64Validator(context, AddressType.I64).bind()

    val functionType = when (definition) {
        is CanonicalDefinition.ResourceNew -> CanonicalCoreFunctionType(
            params = listOf(representation!!),
            results = listOf(I32),
        )

        is CanonicalDefinition.ResourceDrop -> CanonicalCoreFunctionType(params = listOf(I32))
        is CanonicalDefinition.ResourceRep -> CanonicalCoreFunctionType(
            params = listOf(I32),
            results = listOf(representation!!),
        )
    }
    context.frame.coreFunctions += functionType
    context.frame.canonicalAbi += CanonicalAbiDescriptor(functionType)
}

private val I32 = ValueType.Number(NumberType.I32)
private val I64 = ValueType.Number(NumberType.I64)
