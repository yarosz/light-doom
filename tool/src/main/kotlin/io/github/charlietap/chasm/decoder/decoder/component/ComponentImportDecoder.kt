package io.github.charlietap.chasm.decoder.decoder.component

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExternalType
import io.github.charlietap.chasm.ast.component.Import
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.name.ComponentNameAttributesDecoder
import io.github.charlietap.chasm.decoder.decoder.component.type.ComponentExternalTypeDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentImportDecoder(
    context: ComponentDecoderContext,
): Result<Import, WasmDecodeError> = ComponentImportDecoder(
    context = context,
    nameAttributesDecoder = ::ComponentNameAttributesDecoder,
    externalTypeDecoder = ::ComponentExternalTypeDecoder,
)

internal inline fun ComponentImportDecoder(
    context: ComponentDecoderContext,
    crossinline nameAttributesDecoder: ComponentDecoder<NameAttributes>,
    crossinline externalTypeDecoder: ComponentDecoder<ExternalType>,
): Result<Import, WasmDecodeError> = binding {
    Import(
        name = nameAttributesDecoder(context).bind(),
        type = externalTypeDecoder(context).bind(),
    )
}
