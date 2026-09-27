package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.InstanceDeclaration
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentInstanceTypeDecoder(
    context: ComponentDecoderContext,
): Result<TypeDefinition.Instance, WasmDecodeError> = ComponentInstanceTypeDecoder(
    context = context,
    declarationDecoder = ::InstanceDeclarationDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun ComponentInstanceTypeDecoder(
    context: ComponentDecoderContext,
    noinline declarationDecoder: ComponentDecoder<InstanceDeclaration>,
    crossinline vectorDecoder: ComponentVectorDecoder<InstanceDeclaration>,
): Result<TypeDefinition.Instance, WasmDecodeError> = binding {
    val opcode = context.reader.ubyte()
    if (opcode != TYPE_INSTANCE) {
        Err(ComponentTypeDecodeError.UnknownTypeDefinition(opcode)).bind<Unit>()
    }

    TypeDefinition.Instance(vectorDecoder(context, declarationDecoder).bind().vector)
}

private const val TYPE_INSTANCE: UByte = 0x42u
