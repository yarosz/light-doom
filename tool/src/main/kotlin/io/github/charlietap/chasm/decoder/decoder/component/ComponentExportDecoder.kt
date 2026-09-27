package io.github.charlietap.chasm.decoder.decoder.component

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Export
import io.github.charlietap.chasm.ast.component.ExportTarget
import io.github.charlietap.chasm.ast.component.ExternalType
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.name.ComponentNameAttributesDecoder
import io.github.charlietap.chasm.decoder.decoder.component.sort.ExportTargetDecoder
import io.github.charlietap.chasm.decoder.decoder.component.type.ComponentExternalTypeDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentExportDecoder(
    context: ComponentDecoderContext,
): Result<Export, WasmDecodeError> = ComponentExportDecoder(
    context = context,
    nameAttributesDecoder = ::ComponentNameAttributesDecoder,
    targetDecoder = ::ExportTargetDecoder,
    optionalExternalTypeDecoder = { scopedContext ->
        ComponentOptionalDecoder(scopedContext, ::ComponentExternalTypeDecoder)
    },
)

internal inline fun ComponentExportDecoder(
    context: ComponentDecoderContext,
    crossinline nameAttributesDecoder: ComponentDecoder<NameAttributes>,
    crossinline targetDecoder: ComponentDecoder<ExportTarget>,
    crossinline optionalExternalTypeDecoder: ComponentDecoder<ExternalType?>,
): Result<Export, WasmDecodeError> = binding {
    Export(
        name = nameAttributesDecoder(context).bind(),
        target = targetDecoder(context).bind(),
        type = optionalExternalTypeDecoder(context).bind(),
    )
}
