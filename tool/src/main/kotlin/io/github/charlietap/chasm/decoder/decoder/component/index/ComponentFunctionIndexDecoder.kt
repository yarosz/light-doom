package io.github.charlietap.chasm.decoder.decoder.component.index

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.Index.ComponentFunctionIndex
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.section.index.IndexDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentFunctionIndexDecoder(
    context: ComponentDecoderContext,
): Result<ComponentFunctionIndex, WasmDecodeError> = IndexDecoder(context, ::ComponentFunctionIndex)
