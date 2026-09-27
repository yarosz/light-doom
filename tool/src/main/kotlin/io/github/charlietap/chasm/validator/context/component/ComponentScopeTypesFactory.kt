package io.github.charlietap.chasm.validator.context.component

import io.github.charlietap.chasm.type.ArrayType
import io.github.charlietap.chasm.type.CompositeType
import io.github.charlietap.chasm.type.ConcreteHeapType
import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.type.FieldType
import io.github.charlietap.chasm.type.FunctionType
import io.github.charlietap.chasm.type.GlobalType
import io.github.charlietap.chasm.type.HeapType
import io.github.charlietap.chasm.type.Limits
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.type.RecursiveType
import io.github.charlietap.chasm.type.ReferenceType
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.StorageType
import io.github.charlietap.chasm.type.StructType
import io.github.charlietap.chasm.type.SubType
import io.github.charlietap.chasm.type.TableType
import io.github.charlietap.chasm.type.TagType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.ComponentInstanceType
import io.github.charlietap.chasm.type.component.ComponentItemType
import io.github.charlietap.chasm.type.component.ComponentScopeTypes
import io.github.charlietap.chasm.type.component.ComponentType
import io.github.charlietap.chasm.type.component.ComponentTypeDefinition
import io.github.charlietap.chasm.type.component.ComponentTypeId
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.type.component.CoreInstanceType
import io.github.charlietap.chasm.type.component.CoreModuleType
import io.github.charlietap.chasm.type.component.CoreType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiDescriptor

internal fun ComponentScopeTypesFactory(
    frame: ComponentValidationFrame,
): ComponentScopeTypes = ComponentTypeCopyContext().copy(frame)

private class ComponentTypeCopyContext {

    private val coreTypes = mutableListOf<Pair<DefinedType, DefinedType>>()
    private val componentTypes = mutableMapOf<ComponentTypeId, ComponentTypeDefinition>()

    fun copy(frame: ComponentValidationFrame): ComponentScopeTypes = ComponentScopeTypes(
        type = componentType(frame.componentType()),
        coreTypes = frame.coreTypes.map(::coreType),
        coreFunctions = frame.coreFunctions.map(::definedType),
        coreTables = frame.coreTables.map(::tableType),
        coreMemories = frame.coreMemories.map(::memoryType),
        coreGlobals = frame.coreGlobals.map(::globalType),
        coreTags = frame.coreTags.map(::tagType),
        coreModules = frame.coreModules.map(::coreModuleType),
        coreInstances = frame.coreInstances.map(::coreInstanceType),
        types = frame.types.map { entry -> componentTypeDefinition(entry.definition) },
        functions = frame.functions.map(::componentFunctionType),
        values = frame.values.map { entry -> componentValueType(entry.type) },
        components = frame.components.map(::componentItemType),
        instances = frame.instances.map(::componentInstanceType),
        localResourceRepresentations = frame.localResourceRepresentations.mapValues { (_, type) -> valueType(type) },
        canonicalAbi = frame.canonicalAbi.map(::canonicalAbiDescriptor),
    )

    private fun componentItemType(item: ComponentItemType): ComponentItemType = item.nested?.let { item } ?: item.copy(
        type = componentType(item.type),
    )

    private fun componentTypeDefinition(type: ComponentTypeDefinition): ComponentTypeDefinition =
        componentTypes[type.id] ?: ComponentTypeDefinition(
            id = type.id,
            type = componentDefinedType(type.type),
        ).also { definition -> componentTypes[type.id] = definition }

    private fun componentDefinedType(type: ComponentDefinedType): ComponentDefinedType = when (type) {
        is ComponentDefinedType.Value -> type.copy(type = componentDefinedValueType(type.type))
        is ComponentDefinedType.Function -> type.copy(type = componentFunctionType(type.type))
        is ComponentDefinedType.Component -> type.copy(type = componentType(type.type))
        is ComponentDefinedType.Instance -> type.copy(type = componentInstanceType(type.type))
        is ComponentDefinedType.Resource -> type
    }

    private fun componentEntityType(type: ComponentEntityType): ComponentEntityType = when (type) {
        is ComponentEntityType.CoreModule -> type.copy(type = coreModuleType(type.type))
        is ComponentEntityType.Function -> type.copy(type = componentFunctionType(type.type))
        is ComponentEntityType.Value -> type.copy(type = componentValueType(type.type))
        is ComponentEntityType.Type -> type.copy(referenced = componentTypeDefinition(type.referenced))
        is ComponentEntityType.Component -> type.copy(type = componentType(type.type))
        is ComponentEntityType.Instance -> type.copy(type = componentInstanceType(type.type))
    }

    private fun componentValueType(type: ComponentValueType): ComponentValueType = when (type) {
        is ComponentValueType.Primitive -> type
        is ComponentValueType.Defined -> type.copy(definition = componentTypeDefinition(type.definition))
    }

    private fun componentDefinedValueType(type: ComponentDefinedValueType): ComponentDefinedValueType = when (type) {
        is ComponentDefinedValueType.Primitive -> type
        is ComponentDefinedValueType.Record -> type.copy(
            fields = type.fields.map { field -> field.copy(type = componentValueType(field.type)) },
        )
        is ComponentDefinedValueType.Variant -> type.copy(
            cases = type.cases.map { case -> case.copy(type = case.type?.let(::componentValueType)) },
        )
        is ComponentDefinedValueType.ListValue -> type.copy(element = componentValueType(type.element))
        is ComponentDefinedValueType.FixedLengthList -> type.copy(element = componentValueType(type.element))
        is ComponentDefinedValueType.Map -> type.copy(value = componentValueType(type.value))
        is ComponentDefinedValueType.Tuple -> type.copy(elements = type.elements.map(::componentValueType))
        is ComponentDefinedValueType.Flags -> type
        is ComponentDefinedValueType.Enum -> type
        is ComponentDefinedValueType.Option -> type.copy(value = componentValueType(type.value))
        is ComponentDefinedValueType.Result -> type.copy(
            ok = type.ok?.let(::componentValueType),
            error = type.error?.let(::componentValueType),
        )
        is ComponentDefinedValueType.Own -> type
        is ComponentDefinedValueType.Borrow -> type
        is ComponentDefinedValueType.Stream -> type.copy(element = type.element?.let(::componentValueType))
        is ComponentDefinedValueType.Future -> type.copy(value = type.value?.let(::componentValueType))
    }

    private fun componentFunctionType(type: ComponentFunctionType): ComponentFunctionType = type.copy(
        params = type.params.map { parameter -> parameter.copy(type = componentValueType(parameter.type)) },
        result = type.result?.let(::componentValueType),
    )

    private fun componentType(type: ComponentType): ComponentType = type.copy(
        imports = type.imports.mapValues { (_, entity) -> componentEntityType(entity) },
        exports = type.exports.mapValues { (_, entity) -> componentEntityType(entity) },
        importedResources = type.importedResources.mapValues { (_, path) -> path.toList() },
        definedResources = type.definedResources.mapValues { (_, path) -> path.toList() },
        explicitResources = type.explicitResources.mapValues { (_, path) -> path.toList() },
    )

    private fun componentInstanceType(type: ComponentInstanceType): ComponentInstanceType = type.copy(
        exports = type.exports.mapValues { (_, entity) -> componentEntityType(entity) },
        definedResources = type.definedResources.toSet(),
        explicitResources = type.explicitResources.mapValues { (_, path) -> path.toList() },
    )

    private fun coreType(type: CoreType): CoreType = when (type) {
        is CoreType.Defined -> type.copy(type = definedType(type.type))
        is CoreType.Module -> type.copy(type = coreModuleType(type.type))
    }

    private fun coreEntityType(type: CoreEntityType): CoreEntityType = when (type) {
        is CoreEntityType.Function -> type.copy(type = definedType(type.type))
        is CoreEntityType.Table -> type.copy(type = tableType(type.type))
        is CoreEntityType.Memory -> type.copy(type = memoryType(type.type))
        is CoreEntityType.Global -> type.copy(type = globalType(type.type))
        is CoreEntityType.Tag -> type.copy(type = tagType(type.type))
        is CoreEntityType.Type -> type.copy(type = coreType(type.type))
        is CoreEntityType.Module -> type.copy(type = coreModuleType(type.type))
        is CoreEntityType.Instance -> type.copy(type = coreInstanceType(type.type))
    }

    private fun coreModuleType(type: CoreModuleType): CoreModuleType = type.copy(
        imports = type.imports.mapValues { (_, entity) -> coreEntityType(entity) },
        exports = type.exports.mapValues { (_, entity) -> coreEntityType(entity) },
    )

    private fun coreInstanceType(type: CoreInstanceType): CoreInstanceType = type.copy(
        exports = type.exports.mapValues { (_, entity) -> coreEntityType(entity) },
    )

    private fun canonicalAbiDescriptor(descriptor: CanonicalAbiDescriptor): CanonicalAbiDescriptor = descriptor.copy(
        type = definedType(descriptor.type),
    )

    private fun definedType(type: DefinedType): DefinedType {
        coreTypes.firstOrNull { (source) -> source === type }?.let { (_, snapshot) -> return snapshot }

        val recursiveType = RecursiveType(emptyList(), type.recursiveType.state)
        val snapshot = DefinedType(recursiveType, type.recursiveTypeIndex, type.typeIndex)
        coreTypes += type to snapshot
        recursiveType.subTypes = type.recursiveType.subTypes.map(::subType)
        return snapshot
    }

    private fun subType(type: SubType): SubType = when (type) {
        is SubType.Open -> type.copy(
            superTypes = type.superTypes.map(::heapType),
            compositeType = compositeType(type.compositeType),
        )
        is SubType.Final -> type.copy(
            superTypes = type.superTypes.map(::heapType),
            compositeType = compositeType(type.compositeType),
        )
    }

    private fun compositeType(type: CompositeType): CompositeType = when (type) {
        is CompositeType.Array -> CompositeType.Array(arrayType(type.arrayType))
        is CompositeType.Function -> CompositeType.Function(functionType(type.functionType))
        is CompositeType.Struct -> CompositeType.Struct(structType(type.structType))
    }

    private fun functionType(type: FunctionType): FunctionType = type.copy(
        params = resultType(type.params),
        results = resultType(type.results),
    )

    private fun resultType(type: ResultType): ResultType = ResultType(type.types.map(::valueType))

    private fun structType(type: StructType): StructType = StructType(type.fields.map(::fieldType))

    private fun arrayType(type: ArrayType): ArrayType = ArrayType(fieldType(type.fieldType))

    private fun fieldType(type: FieldType): FieldType = type.copy(storageType = storageType(type.storageType))

    private fun storageType(type: StorageType): StorageType = when (type) {
        is StorageType.Packed -> type
        is StorageType.Value -> StorageType.Value(valueType(type.type))
    }

    private fun valueType(type: ValueType): ValueType = when (type) {
        is ValueType.Bottom -> type
        is ValueType.Number -> type
        is ValueType.Reference -> ValueType.Reference(referenceType(type.referenceType))
        is ValueType.Vector -> type
    }

    private fun referenceType(type: ReferenceType): ReferenceType = when (type) {
        is ReferenceType.Ref -> ReferenceType.Ref(heapType(type.heapType))
        is ReferenceType.RefNull -> ReferenceType.RefNull(heapType(type.heapType))
    }

    private fun heapType(type: HeapType): HeapType = when (type) {
        is ConcreteHeapType.Defined -> ConcreteHeapType.Defined(definedType(type.definedType))
        is ConcreteHeapType.RecursiveTypeIndex -> type
        is ConcreteHeapType.TypeIndex -> type
        else -> type
    }

    private fun tableType(type: TableType): TableType = type.copy(
        referenceType = referenceType(type.referenceType),
        limits = limits(type.limits),
    )

    private fun memoryType(type: MemoryType): MemoryType = type.copy(limits = limits(type.limits))

    private fun globalType(type: GlobalType): GlobalType = type.copy(valueType = valueType(type.valueType))

    private fun tagType(type: TagType): TagType = type.copy(functionType = functionType(type.functionType))

    private fun limits(limits: Limits): Limits = limits.copy()
}
