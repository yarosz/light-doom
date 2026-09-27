package io.github.charlietap.chasm.decoder.context.scope

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import io.github.charlietap.chasm.decoder.context.ReaderContext
import io.github.charlietap.chasm.decoder.decoder.ContextDecoder
import io.github.charlietap.chasm.decoder.error.ReaderDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal inline fun <C : ReaderContext, T> ReaderByteScope(
    context: C,
    size: UInt,
    crossinline decoder: ContextDecoder<C, T>,
): Result<T, WasmDecodeError> {
    val start = context.reader.position()
    val previousLimit = context.reader.pushLimit(size)
    val result = decoder(context)
    val consumed = context.reader.position() - start
    context.reader.restoreLimit(previousLimit)

    return result.fold(
        success = { value ->
            if (consumed == size) {
                Ok(value)
            } else {
                Err(ReaderDecodeError.SizeMismatch(size, consumed))
            }
        },
        failure = ::Err,
    )
}
