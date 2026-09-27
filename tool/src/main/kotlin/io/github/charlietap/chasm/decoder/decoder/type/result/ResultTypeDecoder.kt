package io.github.charlietap.chasm.decoder.decoder.type.result

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.decoder.type.value.ValueTypeDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.VectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.ValueType

internal fun ResultTypeDecoder(
    context: ModuleDecoderContext,
): Result<ResultType, WasmDecodeError> = ResultTypeDecoder(
    context,
    ::VectorDecoder,
    ::ValueTypeDecoder,
)

internal inline fun ResultTypeDecoder(
    context: ModuleDecoderContext,
    crossinline vectorDecoder: VectorDecoder<ValueType>,
    noinline valueTypeDecoder: Decoder<ValueType>,
): Result<ResultType, WasmDecodeError> = binding {
    val valueTypes = vectorDecoder(context, valueTypeDecoder).bind()
    ResultType(valueTypes.vector)
}
