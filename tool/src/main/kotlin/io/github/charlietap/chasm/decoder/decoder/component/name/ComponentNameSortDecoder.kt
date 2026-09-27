package io.github.charlietap.chasm.decoder.decoder.component.name

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.NameSort
import io.github.charlietap.chasm.decoder.context.ReaderContext
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentNameSortDecoder(
    context: ReaderContext,
): Result<NameSort, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        SORT_CORE -> when (val coreOpcode = context.reader.ubyte()) {
            CORE_SORT_FUNCTION -> NameSort.CoreFunction
            CORE_SORT_TABLE -> NameSort.CoreTable
            CORE_SORT_MEMORY -> NameSort.CoreMemory
            CORE_SORT_GLOBAL -> NameSort.CoreGlobal
            CORE_SORT_TAG -> NameSort.CoreTag
            CORE_SORT_TYPE -> NameSort.CoreType
            CORE_SORT_MODULE -> NameSort.CoreModule
            CORE_SORT_INSTANCE -> NameSort.CoreInstance
            else -> Err(ComponentDecodeError.UnknownCoreSort(coreOpcode)).bind()
        }
        SORT_FUNCTION -> NameSort.Function
        SORT_VALUE -> NameSort.Value
        SORT_TYPE -> NameSort.Type
        SORT_COMPONENT -> NameSort.Component
        SORT_INSTANCE -> NameSort.Instance
        else -> Err(ComponentDecodeError.UnknownSort(opcode)).bind()
    }
}

private const val SORT_CORE: UByte = 0x00u
private const val SORT_FUNCTION: UByte = 0x01u
private const val SORT_VALUE: UByte = 0x02u
private const val SORT_TYPE: UByte = 0x03u
private const val SORT_COMPONENT: UByte = 0x04u
private const val SORT_INSTANCE: UByte = 0x05u
private const val CORE_SORT_FUNCTION: UByte = 0x00u
private const val CORE_SORT_TABLE: UByte = 0x01u
private const val CORE_SORT_MEMORY: UByte = 0x02u
private const val CORE_SORT_GLOBAL: UByte = 0x03u
private const val CORE_SORT_TAG: UByte = 0x04u
private const val CORE_SORT_TYPE: UByte = 0x10u
private const val CORE_SORT_MODULE: UByte = 0x11u
private const val CORE_SORT_INSTANCE: UByte = 0x12u
