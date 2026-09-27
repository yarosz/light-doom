package io.github.charlietap.chasm.decoder.decoder.component.instance

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreExport
import io.github.charlietap.chasm.ast.component.CoreExportTarget
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.component.sort.CoreExportTargetDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CoreInlineExportDecoder(
    context: ComponentDecoderContext,
): Result<CoreExport, WasmDecodeError> = CoreInlineExportDecoder(
    context = context,
    nameValueDecoder = ::NameValueDecoder,
    targetDecoder = ::CoreExportTargetDecoder,
)

internal inline fun CoreInlineExportDecoder(
    context: ComponentDecoderContext,
    crossinline nameValueDecoder: ReaderDecoder<NameValue>,
    crossinline targetDecoder: ComponentDecoder<CoreExportTarget>,
): Result<CoreExport, WasmDecodeError> = binding {
    CoreExport(
        name = nameValueDecoder(context).bind(),
        target = targetDecoder(context).bind(),
    )
}
