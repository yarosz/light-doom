package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.decoder.type.recursive.RecursiveTypeDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.type.RecursiveType

internal fun CoreTypeDefinitionDecoder(
    context: ComponentDecoderContext,
): Result<CoreTypeDefinition, WasmDecodeError> = CoreTypeDefinitionDecoder(
    context = context,
    recursiveTypeDecoder = ::RecursiveTypeDecoder,
    moduleTypeDecoder = ::CoreModuleTypeDecoder,
)

internal inline fun CoreTypeDefinitionDecoder(
    context: ComponentDecoderContext,
    crossinline recursiveTypeDecoder: Decoder<RecursiveType>,
    crossinline moduleTypeDecoder: ComponentDecoder<CoreTypeDefinition.ModuleType>,
): Result<CoreTypeDefinition, WasmDecodeError> = binding {
    when (context.reader.peekUByte()) {
        CORE_MODULE_TYPE -> moduleTypeDecoder(context).bind()
        PREFIXED_OPEN_SUBTYPE -> {
            context.reader.ubyte()
            val subtype = context.reader.peekUByte()
            if (subtype != OPEN_SUBTYPE) {
                Err(ComponentTypeDecodeError.InvalidCoreType(subtype)).bind<Unit>()
            }
            CoreTypeDefinition.DefinedType(recursiveTypeDecoder(context.moduleContext).bind())
        }
        else -> CoreTypeDefinition.DefinedType(recursiveTypeDecoder(context.moduleContext).bind())
    }
}

private const val PREFIXED_OPEN_SUBTYPE: UByte = 0x00u
private const val OPEN_SUBTYPE: UByte = 0x50u
private const val CORE_MODULE_TYPE: UByte = 0x50u
