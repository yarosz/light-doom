package io.github.charlietap.chasm.decoder.decoder.section.index

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ReaderContext
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun <T> IndexDecoder(
    context: ReaderContext,
    indexFactory: (UInt) -> T,
): Result<T, WasmDecodeError> = binding {
    val idx = context.reader.uint()
    indexFactory(idx)
}
