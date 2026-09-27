package io.github.charlietap.chasm.decoder.decoder.component.name

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.name.NameAssociation
import io.github.charlietap.chasm.ast.name.NameMap
import io.github.charlietap.chasm.decoder.context.ReaderContext
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.section.custom.NameAssociationDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderContextVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ReaderNameMapDecoder(
    context: ReaderContext,
): Result<NameMap, WasmDecodeError> = ReaderNameMapDecoder(
    context = context,
    associationDecoder = ::NameAssociationDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun ReaderNameMapDecoder(
    context: ReaderContext,
    noinline associationDecoder: ReaderDecoder<NameAssociation>,
    crossinline vectorDecoder: ReaderContextVectorDecoder<NameAssociation>,
): Result<NameMap, WasmDecodeError> = binding {
    vectorDecoder(context, associationDecoder).bind().vector
}
