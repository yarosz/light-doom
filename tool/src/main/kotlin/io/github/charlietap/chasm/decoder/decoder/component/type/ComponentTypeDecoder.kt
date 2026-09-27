package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ComponentDeclaration
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentTypeDecoder(
    context: ComponentDecoderContext,
): Result<TypeDefinition.Component, WasmDecodeError> = ComponentTypeDecoder(
    context = context,
    declarationDecoder = ::ComponentDeclarationDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun ComponentTypeDecoder(
    context: ComponentDecoderContext,
    noinline declarationDecoder: ComponentDecoder<ComponentDeclaration>,
    crossinline vectorDecoder: ComponentVectorDecoder<ComponentDeclaration>,
): Result<TypeDefinition.Component, WasmDecodeError> = binding {
    val opcode = context.reader.ubyte()
    if (opcode != TYPE_COMPONENT) {
        Err(ComponentTypeDecodeError.UnknownTypeDefinition(opcode)).bind<Unit>()
    }

    TypeDefinition.Component(vectorDecoder(context, declarationDecoder).bind().vector)
}

private const val TYPE_COMPONENT: UByte = 0x41u
