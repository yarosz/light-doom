package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentLabelDecoder(
    context: ComponentDecoderContext,
): Result<NameValue, WasmDecodeError> = ComponentLabelDecoder(
    context = context,
    nameDecoder = ::NameValueDecoder,
)

internal inline fun ComponentLabelDecoder(
    context: ComponentDecoderContext,
    crossinline nameDecoder: ReaderDecoder<NameValue>,
): Result<NameValue, WasmDecodeError> = nameDecoder(context)
