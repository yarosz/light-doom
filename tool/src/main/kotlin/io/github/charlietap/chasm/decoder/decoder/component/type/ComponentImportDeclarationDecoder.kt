package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ComponentDeclaration
import io.github.charlietap.chasm.ast.component.ExternalType
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.name.ComponentNameAttributesDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentImportDeclarationDecoder(
    context: ComponentDecoderContext,
): Result<ComponentDeclaration.Import, WasmDecodeError> = ComponentImportDeclarationDecoder(
    context = context,
    nameAttributesDecoder = ::ComponentNameAttributesDecoder,
    externalTypeDecoder = ::ComponentExternalTypeDecoder,
)

internal inline fun ComponentImportDeclarationDecoder(
    context: ComponentDecoderContext,
    crossinline nameAttributesDecoder: ComponentDecoder<NameAttributes>,
    crossinline externalTypeDecoder: ComponentDecoder<ExternalType>,
): Result<ComponentDeclaration.Import, WasmDecodeError> = binding {
    ComponentDeclaration.Import(
        name = nameAttributesDecoder(context).bind(),
        type = externalTypeDecoder(context).bind(),
    )
}
