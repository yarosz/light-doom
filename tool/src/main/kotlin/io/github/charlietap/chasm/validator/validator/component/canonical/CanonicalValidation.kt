package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.ast.module.Index.FunctionIndex
import io.github.charlietap.chasm.ast.module.Index.MemoryIndex
import io.github.charlietap.chasm.ast.module.Index.TableIndex
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.AbstractHeapType
import io.github.charlietap.chasm.type.AddressType
import io.github.charlietap.chasm.type.ConcreteHeapType
import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.type.FunctionType
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.type.ReferenceType
import io.github.charlietap.chasm.type.TableType
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.CoreType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiLowering
import io.github.charlietap.chasm.type.ext.functionType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.CanonicalAbiOptions

internal fun RequireComponentFeature(
    context: ComponentValidationContext,
    feature: ComponentFeature,
): Result<Unit, ComponentValidatorError> = if (feature in context.config.features) {
    Ok(Unit)
} else {
    Err(ComponentValidatorError.FeatureDisabled(feature))
}

internal fun CoreFunction(
    context: ComponentValidationContext,
    index: FunctionIndex,
): Result<DefinedType, ComponentValidatorError> = context.frame.coreFunctions
    .getOrNull(index.idx.toInt())
    ?.let(::Ok)
    ?: Err(ComponentValidatorError.UnknownIndex("core function", index.idx))

internal fun CoreMemory(
    context: ComponentValidationContext,
    index: MemoryIndex,
): Result<MemoryType, ComponentValidatorError> = context.frame.coreMemories
    .getOrNull(index.idx.toInt())
    ?.let(::Ok)
    ?: Err(ComponentValidatorError.UnknownIndex("core memory", index.idx))

internal fun CoreTable(
    context: ComponentValidationContext,
    index: TableIndex,
): Result<TableType, ComponentValidatorError> = context.frame.coreTables
    .getOrNull(index.idx.toInt())
    ?.let(::Ok)
    ?: Err(ComponentValidatorError.UnknownIndex("core table", index.idx))

internal fun ComponentFunction(
    context: ComponentValidationContext,
    index: io.github.charlietap.chasm.ast.component.Index.ComponentFunctionIndex,
): Result<ComponentFunctionType, ComponentValidatorError> = context.frame.functions
    .getOrNull(index.idx.toInt())
    ?.let(::Ok)
    ?: Err(ComponentValidatorError.UnknownIndex("component function", index.idx))

internal fun ComponentDefinedType(
    context: ComponentValidationContext,
    index: ComponentTypeIndex,
): Result<ComponentDefinedType, ComponentValidatorError> = context.frame.types
    .getOrNull(index.idx.toInt())
    ?.type
    ?.let(::Ok)
    ?: Err(ComponentValidatorError.UnknownIndex("component type", index.idx))

// The current AST uses ComponentTypeIndex for these canonical immediates even
// though Binary.md resolves them through the core type index space.
internal fun CanonicalCoreFunctionType(
    context: ComponentValidationContext,
    index: ComponentTypeIndex,
): Result<DefinedType, ComponentValidatorError> {
    val coreType = context.frame.coreTypes.getOrNull(index.idx.toInt())
        ?: return Err(ComponentValidatorError.UnknownIndex("core type", index.idx))
    val definedType = (coreType as? CoreType.Defined)?.type
        ?: return Err(ComponentValidatorError.SortMismatch("core function type", "core module type"))
    return if (definedType.functionType() == null) {
        Err(ComponentValidatorError.SortMismatch("core function type", "core defined type"))
    } else {
        Ok(definedType)
    }
}

internal fun RequireCoreFunctionType(
    actual: DefinedType,
    expected: DefinedType,
    reason: String,
): Result<Unit, ComponentValidatorError> = if (actual.functionType() == expected.functionType()) {
    Ok(Unit)
} else {
    Err(ComponentValidatorError.InvalidCanonicalDefinition(reason))
}

internal fun RequireCoreFunctionShape(
    actual: DefinedType,
    predicate: (FunctionType) -> Boolean,
    reason: String,
): Result<Unit, ComponentValidatorError> {
    val functionType = actual.functionType()
        ?: return Err(ComponentValidatorError.InvalidCanonicalDefinition(reason))
    return if (predicate(functionType)) {
        Ok(Unit)
    } else {
        Err(ComponentValidatorError.InvalidCanonicalDefinition(reason))
    }
}

internal fun RequireMemory64(
    context: ComponentValidationContext,
    addressType: AddressType,
): Result<Unit, ComponentValidatorError> = if (addressType == AddressType.I64) {
    RequireComponentFeature(context, ComponentFeature.Memory64)
} else {
    Ok(Unit)
}

internal fun TableContainsFunctionReferences(table: TableType): Boolean {
    val heapType = table.referenceType.heapType
    return when (heapType) {
        AbstractHeapType.Func -> true
        is ConcreteHeapType.Defined -> heapType.definedType.functionType() != null
        else -> false
    } && (table.referenceType is ReferenceType.Ref || table.referenceType is ReferenceType.RefNull)
}

internal fun CanonicalAbiRequirementsValidator(
    lowering: CanonicalAbiLowering,
    options: CanonicalAbiOptions,
): Result<Unit, ComponentValidatorError> = when {
    (lowering.requiresMemory || lowering.requiresRealloc) && options.memory == null ->
        InvalidCanonicalDefinition("canonical option memory is required")
    lowering.requiresRealloc && options.realloc == null ->
        InvalidCanonicalDefinition("canonical option realloc is required")
    else -> Ok(Unit)
}

internal fun <T> InvalidCanonicalDefinition(reason: String): Result<T, ComponentValidatorError> =
    Err(ComponentValidatorError.InvalidCanonicalDefinition(reason))
