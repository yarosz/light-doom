package io.github.charlietap.chasm.decoder.decoder.component.name

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.NameSort
import io.github.charlietap.chasm.ast.component.SortNameSubsection
import io.github.charlietap.chasm.ast.name.NameMap
import io.github.charlietap.chasm.decoder.context.ReaderContext
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun SortNameSubsectionDecoder(
    context: ReaderContext,
): Result<SortNameSubsection, WasmDecodeError> = SortNameSubsectionDecoder(
    context = context,
    sortDecoder = ::ComponentNameSortDecoder,
    nameMapDecoder = ::ReaderNameMapDecoder,
)

internal inline fun SortNameSubsectionDecoder(
    context: ReaderContext,
    crossinline sortDecoder: ReaderDecoder<NameSort>,
    crossinline nameMapDecoder: ReaderDecoder<NameMap>,
): Result<SortNameSubsection, WasmDecodeError> = binding {
    SortNameSubsection(
        sort = sortDecoder(context).bind(),
        nameMap = nameMapDecoder(context).bind(),
    )
}
