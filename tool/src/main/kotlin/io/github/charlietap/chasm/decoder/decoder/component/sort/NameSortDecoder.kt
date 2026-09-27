package io.github.charlietap.chasm.decoder.decoder.component.sort

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.NameSort
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun NameSortDecoder(
    context: ComponentDecoderContext,
): Result<NameSort, WasmDecodeError> = NameSortDecoder(
    context = context,
    sortDecoder = ::ComponentSortDecoder,
)

internal inline fun NameSortDecoder(
    context: ComponentDecoderContext,
    crossinline sortDecoder: ComponentDecoder<ComponentSort>,
): Result<NameSort, WasmDecodeError> = binding {
    when (val sort = sortDecoder(context).bind()) {
        is ComponentSort.Core -> when (sort.sort) {
            CoreSort.Function -> NameSort.CoreFunction
            CoreSort.Table -> NameSort.CoreTable
            CoreSort.Memory -> NameSort.CoreMemory
            CoreSort.Global -> NameSort.CoreGlobal
            CoreSort.Tag -> NameSort.CoreTag
            CoreSort.Type -> NameSort.CoreType
            CoreSort.Module -> NameSort.CoreModule
            CoreSort.Instance -> NameSort.CoreInstance
        }
        ComponentSort.Function -> NameSort.Function
        ComponentSort.Value -> NameSort.Value
        ComponentSort.Type -> NameSort.Type
        ComponentSort.Component -> NameSort.Component
        ComponentSort.Instance -> NameSort.Instance
    }
}
