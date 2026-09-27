package io.github.charlietap.chasm.decoder.context.scope

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ContextDecoder
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal typealias ScopedContextDecoder<C, S, T> = (
    C,
    S,
    ContextDecoder<C, T>,
) -> Result<T, WasmDecodeError>

internal typealias ScopedDecoder<S, T> = ScopedContextDecoder<ModuleDecoderContext, S, T>

internal typealias ComponentScopedDecoder<S, T> = ScopedContextDecoder<ComponentDecoderContext, S, T>
