package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.ast.module.Index.TableIndex
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.AddressType
import io.github.charlietap.chasm.type.ConcreteHeapType
import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.type.NumberType
import io.github.charlietap.chasm.type.ReferenceType
import io.github.charlietap.chasm.type.TableType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiDescriptor
import io.github.charlietap.chasm.type.component.canonical.CanonicalCoreFunctionType
import io.github.charlietap.chasm.type.component.canonical.coreValueType
import io.github.charlietap.chasm.type.ext.functionType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun ThreadCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
): Result<Unit, ComponentValidatorError> =
    ThreadCanonicalDefinitionValidator(
        context = context,
        definition = definition,
        coreFunctionType = ::CanonicalCoreFunctionType,
        coreTable = ::CoreTable,
        featureValidator = ::RequireComponentFeature,
        memory64Validator = ::RequireMemory64,
    )

internal inline fun ThreadCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
    crossinline coreFunctionType: (
        ComponentValidationContext,
        ComponentTypeIndex,
    ) -> Result<DefinedType, ComponentValidatorError>,
    crossinline coreTable: (
        ComponentValidationContext,
        TableIndex,
    ) -> Result<TableType, ComponentValidatorError>,
    crossinline featureValidator: (
        ComponentValidationContext,
        ComponentFeature,
    ) -> Result<Unit, ComponentValidatorError>,
    crossinline memory64Validator: (
        ComponentValidationContext,
        AddressType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    featureValidator(context, ComponentFeature.Threading).bind()

    val functionType = when (definition) {
        CanonicalDefinition.ThreadIndex -> CanonicalCoreFunctionType(results = listOf(I32))
        is CanonicalDefinition.ThreadNewIndirect -> {
            val target = coreFunctionType(context, definition.typeIndex).bind()
            val closureType = target.threadClosureType().bind()
            memory64Validator(context, closureType.addressType()).bind()
            val table = coreTable(context, definition.tableIndex).bind()
            validateFunctionTable(context, table, memory64Validator).bind()
            CanonicalCoreFunctionType(
                params = listOf(table.addressType.coreValueType(), closureType),
                results = listOf(I32),
            )
        }

        CanonicalDefinition.ThreadResumeLater -> CanonicalCoreFunctionType(params = listOf(I32))
        is CanonicalDefinition.ThreadSuspend -> CanonicalCoreFunctionType(results = listOf(I32))
        is CanonicalDefinition.ThreadSuspendThenResume,
        is CanonicalDefinition.ThreadYieldThenResume,
        is CanonicalDefinition.ThreadSuspendThenPromote,
        is CanonicalDefinition.ThreadYieldThenPromote,
        -> CanonicalCoreFunctionType(params = listOf(I32), results = listOf(I32))

        is CanonicalDefinition.ThreadSpawnRef -> {
            val target = coreFunctionType(context, definition.typeIndex).bind()
            val closureType = target.threadClosureType().bind()
            memory64Validator(context, closureType.addressType()).bind()
            CanonicalCoreFunctionType(
                params = listOf(
                    ValueType.Reference(ReferenceType.RefNull(ConcreteHeapType.Defined(target))),
                    closureType,
                ),
                results = listOf(I32),
            )
        }

        is CanonicalDefinition.ThreadSpawnIndirect -> {
            val target = coreFunctionType(context, definition.typeIndex).bind()
            val closureType = target.threadClosureType().bind()
            memory64Validator(context, closureType.addressType()).bind()
            val table = coreTable(context, definition.tableIndex).bind()
            validateFunctionTable(context, table, memory64Validator).bind()
            CanonicalCoreFunctionType(
                params = listOf(table.addressType.coreValueType(), closureType),
                results = listOf(I32),
            )
        }

        is CanonicalDefinition.ThreadAvailableParallelism -> CanonicalCoreFunctionType(results = listOf(I32))
        else -> Err(ComponentValidatorError.InvalidCanonicalDefinition("expected a thread canonical definition")).bind()
    }
    context.frame.coreFunctions += functionType
    context.frame.canonicalAbi += CanonicalAbiDescriptor(functionType)
}

private fun DefinedType.threadClosureType(): Result<ValueType, ComponentValidatorError> {
    val functionType = functionType()
        ?: return InvalidCanonicalDefinition("thread function type must be a core function type")
    val parameter = functionType.params.types.singleOrNull()
    return if (functionType.results.types.isEmpty() && (parameter == I32 || parameter == I64)) {
        com.github.michaelbull.result.Ok(parameter)
    } else {
        InvalidCanonicalDefinition("thread function type must have one i32 or i64 parameter and no results")
    }
}

private inline fun validateFunctionTable(
    context: ComponentValidationContext,
    table: TableType,
    crossinline memory64Validator: (ComponentValidationContext, AddressType) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    memory64Validator(context, table.addressType).bind()
    if (!TableContainsFunctionReferences(table)) {
        Err(ComponentValidatorError.InvalidCanonicalDefinition("thread table must contain function references")).bind<Unit>()
    }
}

private fun ValueType.addressType(): AddressType = if (this == I64) AddressType.I64 else AddressType.I32

private val I32 = ValueType.Number(NumberType.I32)
private val I64 = ValueType.Number(NumberType.I64)
