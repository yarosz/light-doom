package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.AliasDefinition
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.ast.component.InstanceDeclaration
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.alias.AliasDefinitionDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun InstanceDeclarationDecoder(
    context: ComponentDecoderContext,
): Result<InstanceDeclaration, WasmDecodeError> = InstanceDeclarationDecoder(
    context = context,
    coreTypeDecoder = ::CoreTypeDefinitionDecoder,
    typeDecoder = ::ComponentTypeDefinitionDecoder,
    aliasDecoder = ::AliasDefinitionDecoder,
    exportDecoder = ::ComponentExportDeclarationDecoder,
)

internal inline fun InstanceDeclarationDecoder(
    context: ComponentDecoderContext,
    crossinline coreTypeDecoder: ComponentDecoder<CoreTypeDefinition>,
    crossinline typeDecoder: ComponentDecoder<TypeDefinition>,
    crossinline aliasDecoder: ComponentDecoder<AliasDefinition>,
    crossinline exportDecoder: ComponentDecoder<InstanceDeclaration.Export>,
): Result<InstanceDeclaration, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        DECLARATION_CORE_TYPE -> InstanceDeclaration.CoreType(coreTypeDecoder(context).bind())
        DECLARATION_TYPE -> InstanceDeclaration.Type(typeDecoder(context).bind())
        DECLARATION_ALIAS -> InstanceDeclaration.Alias(aliasDecoder(context).bind())
        DECLARATION_EXPORT -> exportDecoder(context).bind()
        else -> Err(ComponentTypeDecodeError.UnknownTypeDefinition(opcode)).bind<InstanceDeclaration>()
    }
}

private const val DECLARATION_CORE_TYPE: UByte = 0x00u
private const val DECLARATION_TYPE: UByte = 0x01u
private const val DECLARATION_ALIAS: UByte = 0x02u
private const val DECLARATION_EXPORT: UByte = 0x04u
