package io.github.charlietap.chasm.decoder.decoder.component.name

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ComponentNameSubsection
import io.github.charlietap.chasm.ast.component.NameSubsection
import io.github.charlietap.chasm.ast.component.SortNameSubsection
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ReaderContext
import io.github.charlietap.chasm.decoder.context.scope.ReaderByteScope
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentNameSubsectionDecoder(
    context: ReaderContext,
): Result<NameSubsection?, WasmDecodeError> = ComponentNameSubsectionDecoder(
    context = context,
    nameDecoder = ::NameValueDecoder,
    sortNameDecoder = ::SortNameSubsectionDecoder,
)

internal inline fun ComponentNameSubsectionDecoder(
    context: ReaderContext,
    crossinline nameDecoder: ReaderDecoder<NameValue>,
    crossinline sortNameDecoder: ReaderDecoder<SortNameSubsection>,
): Result<NameSubsection?, WasmDecodeError> = binding {
    val subsectionId = context.reader.ubyte()
    val subsectionSize = context.reader.uint()

    ReaderByteScope(context, subsectionSize) { scopedContext ->
        binding {
            when (subsectionId) {
                COMPONENT_NAME_SUBSECTION -> ComponentNameSubsection(nameDecoder(scopedContext).bind())
                SORT_NAME_SUBSECTION -> sortNameDecoder(scopedContext).bind()
                else -> {
                    scopedContext.reader.ubytes(subsectionSize)
                    null
                }
            }
        }
    }.bind()
}

private const val COMPONENT_NAME_SUBSECTION: UByte = 0x00u
private const val SORT_NAME_SUBSECTION: UByte = 0x01u
