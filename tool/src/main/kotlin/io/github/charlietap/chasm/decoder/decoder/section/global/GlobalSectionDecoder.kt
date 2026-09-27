package io.github.charlietap.chasm.decoder.decoder.section.global

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.Global
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.decoder.vector.VectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.decoder.section.GlobalSection

internal fun GlobalSectionDecoder(
    context: ModuleDecoderContext,
): Result<GlobalSection, WasmDecodeError> =
    GlobalSectionDecoder(
        context = context,
        vectorDecoder = ::VectorDecoder,
        globalDecoder = ::GlobalDecoder,
    )

internal inline fun GlobalSectionDecoder(
    context: ModuleDecoderContext,
    crossinline vectorDecoder: VectorDecoder<Global>,
    noinline globalDecoder: Decoder<Global>,
): Result<GlobalSection, WasmDecodeError> = binding {

    val globals = vectorDecoder(context, globalDecoder).bind()

    GlobalSection(globals.vector)
}
