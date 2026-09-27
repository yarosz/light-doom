package io.github.charlietap.chasm.type.component

import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.type.GlobalType
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.type.TableType
import io.github.charlietap.chasm.type.TagType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiDescriptor

data class ComponentTypes(
    val root: ComponentScopeTypes,
)

data class ComponentScopeTypes(
    val type: ComponentType = ComponentType(),
    val coreTypes: List<CoreType> = emptyList(),
    val coreFunctions: List<DefinedType> = emptyList(),
    val coreTables: List<TableType> = emptyList(),
    val coreMemories: List<MemoryType> = emptyList(),
    val coreGlobals: List<GlobalType> = emptyList(),
    val coreTags: List<TagType> = emptyList(),
    val coreModules: List<CoreModuleType> = emptyList(),
    val coreInstances: List<CoreInstanceType> = emptyList(),
    val types: List<ComponentTypeDefinition> = emptyList(),
    val functions: List<ComponentFunctionType> = emptyList(),
    val values: List<ComponentValueType> = emptyList(),
    val components: List<ComponentItemType> = emptyList(),
    val instances: List<ComponentInstanceType> = emptyList(),
    val localResourceRepresentations: Map<ComponentResourceTypeId, ValueType> = emptyMap(),
    val canonicalAbi: List<CanonicalAbiDescriptor> = emptyList(),
)

data class ComponentItemType(
    val type: ComponentType,
    val nested: ComponentScopeTypes? = null,
)
