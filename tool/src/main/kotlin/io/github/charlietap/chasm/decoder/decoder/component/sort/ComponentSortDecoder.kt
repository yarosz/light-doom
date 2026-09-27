package io.github.charlietap.chasm.decoder.decoder.component.sort

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentSortDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSort, WasmDecodeError> = ComponentSortDecoder(
    context = context,
    coreSortDecoder = ::CoreSortDecoder,
)

internal inline fun ComponentSortDecoder(
    context: ComponentDecoderContext,
    crossinline coreSortDecoder: ComponentDecoder<CoreSort>,
): Result<ComponentSort, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        SORT_CORE -> ComponentSort.Core(coreSortDecoder(context).bind())
        SORT_FUNCTION -> ComponentSort.Function
        SORT_VALUE -> ComponentSort.Value
        SORT_TYPE -> ComponentSort.Type
        SORT_COMPONENT -> ComponentSort.Component
        SORT_INSTANCE -> ComponentSort.Instance
        else -> Err(ComponentDecodeError.UnknownSort(opcode)).bind<ComponentSort>()
    }
}

private const val SORT_CORE: UByte = 0x00u
private const val SORT_FUNCTION: UByte = 0x01u
private const val SORT_VALUE: UByte = 0x02u
private const val SORT_TYPE: UByte = 0x03u
private const val SORT_COMPONENT: UByte = 0x04u
private const val SORT_INSTANCE: UByte = 0x05u
