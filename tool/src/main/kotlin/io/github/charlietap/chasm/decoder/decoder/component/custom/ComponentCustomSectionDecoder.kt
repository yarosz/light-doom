package io.github.charlietap.chasm.decoder.decoder.component.custom

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.fold
import io.github.charlietap.chasm.ast.component.Custom
import io.github.charlietap.chasm.ast.component.NameData
import io.github.charlietap.chasm.ast.component.Uninterpreted
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.context.ReaderContext
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.component.name.ComponentNameDataDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeException
import io.github.charlietap.chasm.decoder.ext.trackBytes
import io.github.charlietap.chasm.decoder.reader.BinaryReader
import io.github.charlietap.chasm.decoder.reader.WasmBinaryReader

internal fun ComponentCustomSectionDecoder(
    context: ComponentDecoderContext,
): Result<Custom, WasmDecodeError> = ComponentCustomSectionDecoder(
    context = context,
    nameDecoder = ::NameValueDecoder,
    nameDataDecoder = ::ComponentNameDataDecoder,
)

internal inline fun ComponentCustomSectionDecoder(
    context: ComponentDecoderContext,
    crossinline nameDecoder: ReaderDecoder<NameValue>,
    crossinline nameDataDecoder: ReaderDecoder<NameData>,
): Result<Custom, WasmDecodeError> = binding {
    val (name, nameBytes) = context.reader.trackBytes {
        nameDecoder(context).bind()
    }
    val payloadSize = context.sectionSize.size - nameBytes
    val payload = context.reader.ubytes(payloadSize)

    if (name.name == COMPONENT_NAME_SECTION && context.config.decodeNameSection) {
        val payloadContext = object : ReaderContext {
            override var reader: WasmBinaryReader = BinaryReader(payload.asByteArray())
        }
        try {
            nameDataDecoder(payloadContext).fold(
                success = { data -> data },
                failure = { Uninterpreted(name, payload) },
            )
        } catch (_: WasmDecodeException) {
            Uninterpreted(name, payload)
        } catch (_: NoSuchElementException) {
            Uninterpreted(name, payload)
        }
    } else {
        Uninterpreted(name, payload)
    }
}

private const val COMPONENT_NAME_SECTION = "component-name"
