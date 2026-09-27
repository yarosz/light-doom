package io.github.charlietap.chasm.decoder.decoder.component.index

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.Index.ComponentModuleInstanceIndex
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.section.index.IndexDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentModuleInstanceIndexDecoder(
    context: ComponentDecoderContext,
): Result<ComponentModuleInstanceIndex, WasmDecodeError> = IndexDecoder(context, ::ComponentModuleInstanceIndex)
