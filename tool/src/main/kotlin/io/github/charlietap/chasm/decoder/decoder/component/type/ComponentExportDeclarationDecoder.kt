package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExternalType
import io.github.charlietap.chasm.ast.component.InstanceDeclaration
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.name.ComponentNameAttributesDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentExportDeclarationDecoder(
    context: ComponentDecoderContext,
): Result<InstanceDeclaration.Export, WasmDecodeError> = ComponentExportDeclarationDecoder(
    context = context,
    nameAttributesDecoder = ::ComponentNameAttributesDecoder,
    externalTypeDecoder = ::ComponentExternalTypeDecoder,
)

internal inline fun ComponentExportDeclarationDecoder(
    context: ComponentDecoderContext,
    crossinline nameAttributesDecoder: ComponentDecoder<NameAttributes>,
    crossinline externalTypeDecoder: ComponentDecoder<ExternalType>,
): Result<InstanceDeclaration.Export, WasmDecodeError> = binding {
    InstanceDeclaration.Export(
        name = nameAttributesDecoder(context).bind(),
        type = externalTypeDecoder(context).bind(),
    )
}
