package io.github.charlietap.chasm.ast.component

import io.github.charlietap.chasm.ast.component.Index.ComponentFunctionIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentModuleIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentModuleInstanceIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentValueIndex
import io.github.charlietap.chasm.ast.value.NameValue

sealed interface CoreInstanceDefinition {

    data class Instantiate(
        val moduleIndex: ComponentModuleIndex,
        val args: List<CoreInstantiateArgument>,
    ) : CoreInstanceDefinition

    data class InlineExports(val exports: List<CoreExport>) : CoreInstanceDefinition
}

data class CoreInstantiateArgument(
    val name: NameValue,
    val instanceIndex: ComponentModuleInstanceIndex,
)

data class CoreExport(
    val name: NameValue,
    val target: CoreExportTarget,
)

sealed interface InstanceDefinition {

    data class Instantiate(
        val componentIndex: ComponentIndex,
        val args: List<InstantiateArgument>,
    ) : InstanceDefinition

    data class InlineExports(val exports: List<InlineExport>) : InstanceDefinition
}

data class InstantiateArgument(
    val name: NameValue,
    val target: ExportTarget,
)

data class InlineExport(
    val name: NameAttributes,
    val target: ExportTarget,
)

data class StartDefinition(
    val functionIndex: ComponentFunctionIndex,
    val args: List<ComponentValueIndex>,
    val resultCount: UInt,
)

data class ComponentValue(
    val type: ValueType,
    val value: ComponentValueLiteral,
)

sealed interface ComponentValueLiteral {

    data class Bool(val value: Boolean) : ComponentValueLiteral

    data class S8(val value: Byte) : ComponentValueLiteral

    data class U8(val value: UByte) : ComponentValueLiteral

    data class S16(val value: Short) : ComponentValueLiteral

    data class U16(val value: UShort) : ComponentValueLiteral

    data class S32(val value: Int) : ComponentValueLiteral

    data class U32(val value: UInt) : ComponentValueLiteral

    data class S64(val value: Long) : ComponentValueLiteral

    data class U64(val value: ULong) : ComponentValueLiteral

    data class F32(val value: Float) : ComponentValueLiteral

    data class F64(val value: Double) : ComponentValueLiteral

    data object Nan : ComponentValueLiteral

    data class Char(val codePoint: UInt) : ComponentValueLiteral

    data class String(val value: kotlin.String) : ComponentValueLiteral

    data class Record(val fields: List<ComponentValueLiteral>) : ComponentValueLiteral

    data class Variant(
        val label: NameValue,
        val value: ComponentValueLiteral?,
    ) : ComponentValueLiteral

    data class ListValue(val elements: List<ComponentValueLiteral>) : ComponentValueLiteral

    data class Tuple(val elements: List<ComponentValueLiteral>) : ComponentValueLiteral

    data class Flags(val labels: List<NameValue>) : ComponentValueLiteral

    data class Enum(val label: NameValue) : ComponentValueLiteral

    data object None : ComponentValueLiteral

    data class Some(val value: ComponentValueLiteral) : ComponentValueLiteral

    data object Ok : ComponentValueLiteral

    data class OkValue(val value: ComponentValueLiteral) : ComponentValueLiteral

    data object Error : ComponentValueLiteral

    data class ErrorValue(val value: ComponentValueLiteral) : ComponentValueLiteral

    data class Binary(val bytes: List<UByte>) : ComponentValueLiteral
}
