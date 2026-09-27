package io.github.charlietap.chasm.decoder.decoder.component.name

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.NameData
import io.github.charlietap.chasm.ast.component.NameSubsection
import io.github.charlietap.chasm.decoder.context.ReaderContext
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentNameDataDecoder(
    context: ReaderContext,
): Result<NameData, WasmDecodeError> = ComponentNameDataDecoder(
    context = context,
    subsectionDecoder = ::ComponentNameSubsectionDecoder,
)

internal inline fun ComponentNameDataDecoder(
    context: ReaderContext,
    crossinline subsectionDecoder: ReaderDecoder<NameSubsection?>,
): Result<NameData, WasmDecodeError> = binding {
    val subsections = mutableListOf<NameSubsection>()
    while (!context.reader.exhausted()) {
        subsectionDecoder(context).bind()?.let(subsections::add)
    }
    NameData(subsections)
}
