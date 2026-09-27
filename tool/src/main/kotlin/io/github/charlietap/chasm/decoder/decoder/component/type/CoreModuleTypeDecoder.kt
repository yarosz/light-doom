package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreModuleDeclaration
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CoreModuleTypeDecoder(
    context: ComponentDecoderContext,
): Result<CoreTypeDefinition.ModuleType, WasmDecodeError> = CoreModuleTypeDecoder(
    context = context,
    declarationDecoder = ::CoreModuleDeclarationDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun CoreModuleTypeDecoder(
    context: ComponentDecoderContext,
    noinline declarationDecoder: ComponentDecoder<CoreModuleDeclaration>,
    crossinline vectorDecoder: ComponentVectorDecoder<CoreModuleDeclaration>,
): Result<CoreTypeDefinition.ModuleType, WasmDecodeError> = binding {
    val opcode = context.reader.ubyte()
    if (opcode != CORE_MODULE_TYPE) {
        Err(ComponentTypeDecodeError.InvalidCoreType(opcode)).bind<Unit>()
    }

    CoreTypeDefinition.ModuleType(vectorDecoder(context, declarationDecoder).bind().vector)
}

private const val CORE_MODULE_TYPE: UByte = 0x50u
