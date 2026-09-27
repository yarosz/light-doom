package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ComponentDeclaration
import io.github.charlietap.chasm.ast.component.InstanceDeclaration
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentDeclarationDecoder(
    context: ComponentDecoderContext,
): Result<ComponentDeclaration, WasmDecodeError> = ComponentDeclarationDecoder(
    context = context,
    importDecoder = ::ComponentImportDeclarationDecoder,
    instanceDeclarationDecoder = ::InstanceDeclarationDecoder,
)

internal inline fun ComponentDeclarationDecoder(
    context: ComponentDecoderContext,
    crossinline importDecoder: ComponentDecoder<ComponentDeclaration.Import>,
    crossinline instanceDeclarationDecoder: ComponentDecoder<InstanceDeclaration>,
): Result<ComponentDeclaration, WasmDecodeError> = binding {
    if (context.reader.peekUByte() == DECLARATION_IMPORT) {
        context.reader.ubyte()
        importDecoder(context).bind()
    } else {
        when (val declaration = instanceDeclarationDecoder(context).bind()) {
            is InstanceDeclaration.CoreType -> ComponentDeclaration.CoreType(declaration.type)
            is InstanceDeclaration.Type -> ComponentDeclaration.Type(declaration.type)
            is InstanceDeclaration.Alias -> ComponentDeclaration.Alias(declaration.alias)
            is InstanceDeclaration.Export -> ComponentDeclaration.Export(
                name = declaration.name,
                type = declaration.type,
            )
        }
    }
}

private const val DECLARATION_IMPORT: UByte = 0x03u
