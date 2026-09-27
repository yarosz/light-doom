package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ComponentValue
import io.github.charlietap.chasm.ast.component.Value
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.value.ComponentValueDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ValueSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = ValueSectionDecoder(
    context = context,
    valueDecoder = ::ComponentValueDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun ValueSectionDecoder(
    context: ComponentDecoderContext,
    noinline valueDecoder: ComponentDecoder<ComponentValue>,
    crossinline vectorDecoder: ComponentVectorDecoder<ComponentValue>,
): Result<ComponentSection, WasmDecodeError> = binding {
    val definitions = vectorDecoder(context, valueDecoder).bind().vector.map(::Value)
    ComponentSection.Definitions(definitions)
}
