package io.github.charlietap.chasm.embedding

import com.github.michaelbull.result.fold
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import io.github.charlietap.chasm.config.ComponentConfig
import io.github.charlietap.chasm.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.WasmComponentDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecoderError
import io.github.charlietap.chasm.embedding.error.ChasmError.DecodeError
import io.github.charlietap.chasm.embedding.shapes.ChasmResult
import io.github.charlietap.chasm.embedding.shapes.ChasmResult.Error
import io.github.charlietap.chasm.embedding.shapes.ChasmResult.Success
import io.github.charlietap.chasm.embedding.shapes.Component
import io.github.charlietap.chasm.stream.SourceReader

fun component(
    sourceReader: SourceReader,
    config: ComponentConfig = ComponentConfig(),
): ChasmResult<Component, DecodeError> {
    return component(
        source = sourceReader,
        config = config,
        decoder = ::WasmComponentDecoder,
    )
}

fun component(
    bytes: ByteArray,
    config: ComponentConfig = ComponentConfig(),
): ChasmResult<Component, DecodeError> {
    return component(
        source = bytes,
        config = config,
        decoder = ::WasmComponentDecoder,
    )
}

internal fun <Source> component(
    source: Source,
    config: ComponentConfig,
    decoder: ComponentDecoder<Source>,
): ChasmResult<Component, DecodeError> {
    return decoder(config, source)
        .mapError(ComponentDecoderError::toString)
        .mapError(::DecodeError)
        .map { internal -> Component(config, internal) }
        .fold(::Success, ::Error)
}
