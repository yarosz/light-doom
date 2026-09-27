package io.github.charlietap.chasm.decoder.decoder.component.instance

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExportTarget
import io.github.charlietap.chasm.ast.component.InlineExport
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.name.ComponentNameAttributesDecoder
import io.github.charlietap.chasm.decoder.decoder.component.sort.ExportTargetDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun InlineExportDecoder(
    context: ComponentDecoderContext,
): Result<InlineExport, WasmDecodeError> = InlineExportDecoder(
    context = context,
    nameAttributesDecoder = ::ComponentNameAttributesDecoder,
    targetDecoder = ::ExportTargetDecoder,
)

internal inline fun InlineExportDecoder(
    context: ComponentDecoderContext,
    crossinline nameAttributesDecoder: ComponentDecoder<NameAttributes>,
    crossinline targetDecoder: ComponentDecoder<ExportTarget>,
): Result<InlineExport, WasmDecodeError> = binding {
    InlineExport(
        name = nameAttributesDecoder(context).bind(),
        target = targetDecoder(context).bind(),
    )
}
