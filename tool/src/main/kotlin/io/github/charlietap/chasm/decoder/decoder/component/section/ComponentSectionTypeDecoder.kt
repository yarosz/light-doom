package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.component.section.ComponentSectionType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentSectionTypeDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSectionType, WasmDecodeError> = binding {
    val id = context.reader.ubyte()
    ComponentSectionType.entries
        .firstOrNull { section -> section.id == id }
        ?: Err(ComponentDecodeError.UnknownSection(id)).bind()
}
