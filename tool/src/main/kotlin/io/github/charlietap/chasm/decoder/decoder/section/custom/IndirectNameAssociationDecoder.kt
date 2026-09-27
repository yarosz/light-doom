package io.github.charlietap.chasm.decoder.decoder.section.custom

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.name.IndirectNameAssociation
import io.github.charlietap.chasm.ast.name.NameMap
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun IndirectNameAssociationDecoder(
    context: ModuleDecoderContext,
): Result<IndirectNameAssociation, WasmDecodeError> = IndirectNameAssociationDecoder(
    context = context,
    nameMapDecoder = ::NameMapDecoder,
)

internal inline fun IndirectNameAssociationDecoder(
    context: ModuleDecoderContext,
    crossinline nameMapDecoder: Decoder<NameMap>,
) = binding {

    val index = context.reader.uint()
    val nameMap = nameMapDecoder(context).bind()

    IndirectNameAssociation(
        idx = index,
        nameMap = nameMap,
    )
}
