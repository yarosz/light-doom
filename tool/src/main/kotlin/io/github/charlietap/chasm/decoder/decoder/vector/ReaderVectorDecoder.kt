package io.github.charlietap.chasm.decoder.decoder.vector

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.context.ReaderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.ContextDecoder
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal typealias ComponentVectorDecoder<T> = (
    ComponentDecoderContext,
    ComponentDecoder<T>,
) -> Result<Vector<T>, WasmDecodeError>

internal typealias ReaderContextVectorDecoder<T> = (
    ReaderContext,
    ReaderDecoder<T>,
) -> Result<Vector<T>, WasmDecodeError>

internal inline fun <C : ReaderContext, T> ReaderVectorDecoder(
    context: C,
    crossinline elementDecoder: ContextDecoder<C, T>,
): Result<Vector<T>, WasmDecodeError> = binding {
    val size = context.reader.uint()
    if (size > MAX_COMPONENT_VECTOR_SIZE) {
        Err(ComponentDecodeError.VectorTooLarge(size)).bind<Unit>()
    }

    Vector(List(size.toInt()) { elementDecoder(context).bind() })
}

internal const val MAX_COMPONENT_VECTOR_SIZE = 1_000_000u
