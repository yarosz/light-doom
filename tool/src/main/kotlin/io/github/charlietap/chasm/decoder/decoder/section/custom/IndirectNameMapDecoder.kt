package io.github.charlietap.chasm.decoder.decoder.section.custom

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.name.IndirectNameAssociation
import io.github.charlietap.chasm.ast.name.IndirectNameMap
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.decoder.vector.VectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun IndirectNameMapDecoder(
    context: ModuleDecoderContext,
): Result<IndirectNameMap, WasmDecodeError> = IndirectNameMapDecoder(
    context = context,
    indirectNameAssociationDecoder = ::IndirectNameAssociationDecoder,
    vectorDecoder = ::VectorDecoder,
)

internal inline fun IndirectNameMapDecoder(
    context: ModuleDecoderContext,
    noinline indirectNameAssociationDecoder: Decoder<IndirectNameAssociation>,
    crossinline vectorDecoder: VectorDecoder<IndirectNameAssociation>,
) = binding {
    vectorDecoder(context, indirectNameAssociationDecoder).bind().vector
}
