package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.AddressType
import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.type.NumberType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiDescriptor
import io.github.charlietap.chasm.type.component.canonical.CanonicalCoreFunctionType
import io.github.charlietap.chasm.type.component.canonical.coreValueType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun AsyncBuiltinCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
): Result<Unit, ComponentValidatorError> =
    AsyncBuiltinCanonicalDefinitionValidator(
        context = context,
        definition = definition,
        coreMemory = ::CoreMemory,
        featureValidator = ::RequireComponentFeature,
        memory64Validator = ::RequireMemory64,
    )

internal inline fun AsyncBuiltinCanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
    crossinline coreMemory: (
        ComponentValidationContext,
        io.github.charlietap.chasm.ast.module.Index.MemoryIndex,
    ) -> Result<MemoryType, ComponentValidatorError>,
    crossinline featureValidator: (
        ComponentValidationContext,
        ComponentFeature,
    ) -> Result<Unit, ComponentValidatorError>,
    crossinline memory64Validator: (
        ComponentValidationContext,
        AddressType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    featureValidator(context, ComponentFeature.Async).bind()

    val functionType: DefinedType = when (definition) {
        CanonicalDefinition.BackpressureSet -> CanonicalCoreFunctionType(params = listOf(I32))
        CanonicalDefinition.BackpressureInc,
        CanonicalDefinition.BackpressureDec,
        CanonicalDefinition.TaskCancel,
        -> CanonicalCoreFunctionType()

        is CanonicalDefinition.ContextGet -> {
            val contextType = contextType(context, definition.type, definition.index, memory64Validator).bind()
            CanonicalCoreFunctionType(results = listOf(contextType))
        }

        is CanonicalDefinition.ContextSet -> {
            val contextType = contextType(context, definition.type, definition.index, memory64Validator).bind()
            CanonicalCoreFunctionType(params = listOf(contextType))
        }

        is CanonicalDefinition.SubtaskCancel -> {
            if (definition.async) featureValidator(context, ComponentFeature.MoreAsyncBuiltins).bind()
            CanonicalCoreFunctionType(params = listOf(I32), results = listOf(I32))
        }

        CanonicalDefinition.SubtaskDrop -> CanonicalCoreFunctionType(params = listOf(I32))
        CanonicalDefinition.WaitableSetNew -> CanonicalCoreFunctionType(results = listOf(I32))
        is CanonicalDefinition.WaitableSetWait -> waitableSetFunctionType(
            context,
            definition.memoryIndex,
            coreMemory,
            memory64Validator,
        ).bind()

        is CanonicalDefinition.WaitableSetPoll -> waitableSetFunctionType(
            context,
            definition.memoryIndex,
            coreMemory,
            memory64Validator,
        ).bind()

        CanonicalDefinition.WaitableSetDrop -> CanonicalCoreFunctionType(params = listOf(I32))
        CanonicalDefinition.WaitableJoin -> CanonicalCoreFunctionType(params = listOf(I32, I32))
        is CanonicalDefinition.ThreadYield -> CanonicalCoreFunctionType(results = listOf(I32))
        else -> Err(ComponentValidatorError.InvalidCanonicalDefinition("expected an async canonical builtin")).bind()
    }
    context.frame.coreFunctions += functionType
    context.frame.canonicalAbi += CanonicalAbiDescriptor(functionType)
}

private inline fun contextType(
    context: ComponentValidationContext,
    type: ValueType,
    index: UInt,
    crossinline memory64Validator: (ComponentValidationContext, AddressType) -> Result<Unit, ComponentValidatorError>,
): Result<ValueType, ComponentValidatorError> = binding {
    if (index >= 2u) {
        Err(ComponentValidatorError.InvalidCanonicalDefinition("context index must be less than 2")).bind<Unit>()
    }
    val addressType = when (type) {
        ValueType.Number(NumberType.I32) -> AddressType.I32
        ValueType.Number(NumberType.I64) -> AddressType.I64
        else -> Err(ComponentValidatorError.InvalidCanonicalDefinition("context type must be i32 or i64")).bind()
    }
    memory64Validator(context, addressType).bind()
    val previous = context.frame.contextType
    if (previous != null && previous != type) {
        Err(ComponentValidatorError.InvalidCanonicalDefinition("all context builtins must use the same type")).bind<Unit>()
    }
    context.frame.contextType = type
    addressType.coreValueType()
}

private inline fun waitableSetFunctionType(
    context: ComponentValidationContext,
    index: io.github.charlietap.chasm.ast.module.Index.MemoryIndex,
    crossinline coreMemory: (
        ComponentValidationContext,
        io.github.charlietap.chasm.ast.module.Index.MemoryIndex,
    ) -> Result<MemoryType, ComponentValidatorError>,
    crossinline memory64Validator: (ComponentValidationContext, AddressType) -> Result<Unit, ComponentValidatorError>,
): Result<DefinedType, ComponentValidatorError> = binding {
    val memory = coreMemory(context, index).bind()
    memory64Validator(context, memory.addressType).bind()
    CanonicalCoreFunctionType(
        params = listOf(I32, memory.addressType.coreValueType()),
        results = listOf(I32),
    )
}

private val I32 = ValueType.Number(NumberType.I32)
