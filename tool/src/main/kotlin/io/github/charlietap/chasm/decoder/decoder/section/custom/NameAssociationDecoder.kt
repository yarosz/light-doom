package io.github.charlietap.chasm.decoder.decoder.section.custom

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.name.NameAssociation
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ReaderContext
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun NameAssociationDecoder(
    context: ReaderContext,
): Result<NameAssociation, WasmDecodeError> = NameAssociationDecoder(
    context = context,
    nameValueDecoder = ::NameValueDecoder,
)

internal inline fun NameAssociationDecoder(
    context: ReaderContext,
    crossinline nameValueDecoder: ReaderDecoder<NameValue>,
) = binding {

    val index = context.reader.uint()
    val name = nameValueDecoder(context).bind()

    NameAssociation(
        idx = index,
        name = name,
    )
}
