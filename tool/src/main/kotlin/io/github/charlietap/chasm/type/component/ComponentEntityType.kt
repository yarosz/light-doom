package io.github.charlietap.chasm.type.component

import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.type.GlobalType
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.type.TableType
import io.github.charlietap.chasm.type.TagType
import kotlin.jvm.JvmInline

@JvmInline
value class ComponentResourceTypeId(val value: UInt)

@JvmInline
value class ComponentTypeId(val value: UInt)

enum class ComponentPrimitiveType {
    Bool,
    S8,
    U8,
    S16,
    U16,
    S32,
    U32,
    S64,
    U64,
    F32,
    F64,
    Char,
    String,
    ErrorContext,
}

data class ComponentTypeDefinition(
    val id: ComponentTypeId,
    val type: ComponentDefinedType,
)

sealed interface CoreType {

    data class Defined(val type: DefinedType) : CoreType

    data class Module(val type: CoreModuleType) : CoreType
}

sealed interface CoreEntityType {

    data class Function(val type: DefinedType) : CoreEntityType

    data class Table(val type: TableType) : CoreEntityType

    data class Memory(val type: MemoryType) : CoreEntityType

    data class Global(val type: GlobalType) : CoreEntityType

    data class Tag(val type: TagType) : CoreEntityType

    data class Type(val type: CoreType) : CoreEntityType

    data class Module(val type: CoreModuleType) : CoreEntityType

    data class Instance(val type: CoreInstanceType) : CoreEntityType
}

data class CoreImportName(
    val module: String,
    val entity: String,
)

data class CoreModuleType(
    val imports: Map<CoreImportName, CoreEntityType> = emptyMap(),
    val exports: Map<String, CoreEntityType> = emptyMap(),
)

data class CoreInstanceType(
    val exports: Map<String, CoreEntityType> = emptyMap(),
)

sealed interface ComponentValueType {

    data class Primitive(val type: ComponentPrimitiveType) : ComponentValueType

    data class Defined(val definition: ComponentTypeDefinition) : ComponentValueType
}

sealed interface ComponentDefinedValueType {

    data class Primitive(val type: ComponentPrimitiveType) : ComponentDefinedValueType

    data class Record(val fields: List<LabeledComponentValueType>) : ComponentDefinedValueType

    data class Variant(val cases: List<ComponentVariantCase>) : ComponentDefinedValueType

    data class ListValue(val element: ComponentValueType) : ComponentDefinedValueType

    data class FixedLengthList(val element: ComponentValueType, val length: UInt) : ComponentDefinedValueType

    data class Map(val key: ComponentPrimitiveType, val value: ComponentValueType) : ComponentDefinedValueType

    data class Tuple(val elements: List<ComponentValueType>) : ComponentDefinedValueType

    data class Flags(val labels: List<String>) : ComponentDefinedValueType

    data class Enum(val labels: List<String>) : ComponentDefinedValueType

    data class Option(val value: ComponentValueType) : ComponentDefinedValueType

    data class Result(val ok: ComponentValueType?, val error: ComponentValueType?) : ComponentDefinedValueType

    data class Own(
        val id: ComponentTypeId,
        val resource: ComponentResourceTypeId,
    ) : ComponentDefinedValueType

    data class Borrow(
        val id: ComponentTypeId,
        val resource: ComponentResourceTypeId,
    ) : ComponentDefinedValueType

    data class Stream(val element: ComponentValueType?) : ComponentDefinedValueType

    data class Future(val value: ComponentValueType?) : ComponentDefinedValueType
}

data class LabeledComponentValueType(
    val label: String,
    val type: ComponentValueType,
)

data class ComponentVariantCase(
    val label: String,
    val type: ComponentValueType?,
)

data class ComponentFunctionType(
    val params: List<LabeledComponentValueType>,
    val result: ComponentValueType?,
    val async: Boolean,
)

sealed interface ComponentDefinedType {

    data class Value(val type: ComponentDefinedValueType) : ComponentDefinedType

    data class Function(val type: ComponentFunctionType) : ComponentDefinedType

    data class Component(val type: ComponentType) : ComponentDefinedType

    data class Instance(val type: ComponentInstanceType) : ComponentDefinedType

    data class Resource(
        val id: ComponentResourceTypeId,
    ) : ComponentDefinedType
}

sealed interface ComponentEntityType {

    data class CoreModule(val type: CoreModuleType) : ComponentEntityType

    data class Function(val type: ComponentFunctionType) : ComponentEntityType

    data class Value(val type: ComponentValueType) : ComponentEntityType

    data class Type(
        val referenced: ComponentTypeDefinition,
        val createdId: ComponentTypeId,
    ) : ComponentEntityType

    data class Component(val type: ComponentType) : ComponentEntityType

    data class Instance(val type: ComponentInstanceType) : ComponentEntityType
}

data class ComponentType(
    val imports: Map<String, ComponentEntityType> = emptyMap(),
    val exports: Map<String, ComponentEntityType> = emptyMap(),
    val importedResources: Map<ComponentResourceTypeId, List<String>> = emptyMap(),
    val definedResources: Map<ComponentResourceTypeId, List<String>> = emptyMap(),
    val explicitResources: Map<ComponentResourceTypeId, List<String>> = emptyMap(),
)

data class ComponentInstanceType(
    val exports: Map<String, ComponentEntityType> = emptyMap(),
    val definedResources: Set<ComponentResourceTypeId> = emptySet(),
    val explicitResources: Map<ComponentResourceTypeId, List<String>> = emptyMap(),
)
