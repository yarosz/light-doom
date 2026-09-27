package io.github.charlietap.chasm.decoder.decoder.section.element

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.error.SectionDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ElementKindDecoder(
    context: ModuleDecoderContext,
): Result<ElementKind, WasmDecodeError> = binding {
    when (val byte = context.reader.byte()) {
        ElementKind.FuncRef.byte -> ElementKind.FuncRef
        else -> Err(SectionDecodeError.UnknownElementKind(byte)).bind<ElementKind>()
    }
}
