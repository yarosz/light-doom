package io.github.charlietap.chasm.decoder.decoder.component.instance

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExportTarget
import io.github.charlietap.chasm.ast.component.InstantiateArgument
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.component.sort.ExportTargetDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun InstantiateArgumentDecoder(
    context: ComponentDecoderContext,
): Result<InstantiateArgument, WasmDecodeError> = InstantiateArgumentDecoder(
    context = context,
    nameValueDecoder = ::NameValueDecoder,
    targetDecoder = ::ExportTargetDecoder,
)

internal inline fun InstantiateArgumentDecoder(
    context: ComponentDecoderContext,
    crossinline nameValueDecoder: ReaderDecoder<NameValue>,
    crossinline targetDecoder: ComponentDecoder<ExportTarget>,
): Result<InstantiateArgument, WasmDecodeError> = binding {
    InstantiateArgument(
        name = nameValueDecoder(context).bind(),
        target = targetDecoder(context).bind(),
    )
}
