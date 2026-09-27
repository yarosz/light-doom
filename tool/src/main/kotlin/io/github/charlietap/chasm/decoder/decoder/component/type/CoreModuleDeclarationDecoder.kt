package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreExternalType
import io.github.charlietap.chasm.ast.component.CoreModuleDeclaration
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.decoder.section.index.TypeIndexDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.ast.module.Index.TypeIndex as ModuleTypeIndex

internal fun CoreModuleDeclarationDecoder(
    context: ComponentDecoderContext,
): Result<CoreModuleDeclaration, WasmDecodeError> = CoreModuleDeclarationDecoder(
    context = context,
    coreTypeDecoder = ::CoreTypeDefinitionDecoder,
    nameDecoder = ::NameValueDecoder,
    externalTypeDecoder = ::CoreExternalTypeDecoder,
    typeIndexDecoder = ::TypeIndexDecoder,
)

internal inline fun CoreModuleDeclarationDecoder(
    context: ComponentDecoderContext,
    crossinline coreTypeDecoder: ComponentDecoder<CoreTypeDefinition>,
    crossinline nameDecoder: ReaderDecoder<NameValue>,
    crossinline externalTypeDecoder: ComponentDecoder<CoreExternalType>,
    crossinline typeIndexDecoder: Decoder<ModuleTypeIndex>,
): Result<CoreModuleDeclaration, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        CORE_MODULE_DECLARATION_IMPORT -> CoreModuleDeclaration.Import(
            moduleName = nameDecoder(context).bind(),
            entityName = nameDecoder(context).bind(),
            descriptor = externalTypeDecoder(context).bind(),
        )
        CORE_MODULE_DECLARATION_TYPE -> CoreModuleDeclaration.Type(coreTypeDecoder(context).bind())
        CORE_MODULE_DECLARATION_OUTER_ALIAS -> {
            val sort = context.reader.ubyte()
            if (sort != CORE_TYPE_SORT) {
                Err(ComponentTypeDecodeError.InvalidReservedByte(CORE_TYPE_SORT, sort)).bind<Unit>()
            }
            val alias = context.reader.ubyte()
            if (alias != CORE_OUTER_ALIAS) {
                Err(ComponentTypeDecodeError.InvalidReservedByte(CORE_OUTER_ALIAS, alias)).bind<Unit>()
            }
            CoreModuleDeclaration.OuterAlias(
                count = context.reader.uint(),
                typeIndex = typeIndexDecoder(context.moduleContext).bind(),
            )
        }
        CORE_MODULE_DECLARATION_EXPORT -> CoreModuleDeclaration.Export(
            name = nameDecoder(context).bind(),
            descriptor = externalTypeDecoder(context).bind(),
        )
        else -> Err(
            ComponentTypeDecodeError.UnknownCoreModuleDeclaration(opcode),
        ).bind<CoreModuleDeclaration>()
    }
}

private const val CORE_MODULE_DECLARATION_IMPORT: UByte = 0x00u
private const val CORE_MODULE_DECLARATION_TYPE: UByte = 0x01u
private const val CORE_MODULE_DECLARATION_OUTER_ALIAS: UByte = 0x02u
private const val CORE_MODULE_DECLARATION_EXPORT: UByte = 0x03u
private const val CORE_TYPE_SORT: UByte = 0x10u
private const val CORE_OUTER_ALIAS: UByte = 0x01u
