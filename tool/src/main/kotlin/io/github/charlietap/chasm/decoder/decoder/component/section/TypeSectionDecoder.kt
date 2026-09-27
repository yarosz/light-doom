package io.github.charlietap.chasm.decoder.decoder.component.section

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Type
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.decoder.component.section.ComponentSection
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.type.ComponentTypeDefinitionDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun TypeSectionDecoder(
    context: ComponentDecoderContext,
): Result<ComponentSection, WasmDecodeError> = TypeSectionDecoder(
    context = context,
    typeDecoder = ::ComponentTypeDefinitionDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun TypeSectionDecoder(
    context: ComponentDecoderContext,
    noinline typeDecoder: ComponentDecoder<TypeDefinition>,
    crossinline vectorDecoder: ComponentVectorDecoder<TypeDefinition>,
): Result<ComponentSection, WasmDecodeError> = binding {
    val definitions = vectorDecoder(context, typeDecoder).bind().vector.map(::Type)
    ComponentSection.Definitions(definitions)
}
