package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreExternalType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.decoder.section.index.TypeIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.type.global.GlobalTypeDecoder
import io.github.charlietap.chasm.decoder.decoder.type.memory.MemoryTypeDecoder
import io.github.charlietap.chasm.decoder.decoder.type.table.TableTypeDecoder
import io.github.charlietap.chasm.decoder.decoder.type.tag.AttributeDecoder
import io.github.charlietap.chasm.decoder.error.SectionDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.type.GlobalType
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.type.TableType
import io.github.charlietap.chasm.type.TagType
import io.github.charlietap.chasm.ast.module.Index.TypeIndex as ModuleTypeIndex

internal fun CoreExternalTypeDecoder(
    context: ComponentDecoderContext,
): Result<CoreExternalType, WasmDecodeError> = CoreExternalTypeDecoder(
    context = context,
    typeIndexDecoder = ::TypeIndexDecoder,
    tableTypeDecoder = ::TableTypeDecoder,
    memoryTypeDecoder = ::MemoryTypeDecoder,
    globalTypeDecoder = ::GlobalTypeDecoder,
    tagAttributeDecoder = ::AttributeDecoder,
)

internal inline fun CoreExternalTypeDecoder(
    context: ComponentDecoderContext,
    crossinline typeIndexDecoder: Decoder<ModuleTypeIndex>,
    crossinline tableTypeDecoder: Decoder<TableType>,
    crossinline memoryTypeDecoder: Decoder<MemoryType>,
    crossinline globalTypeDecoder: Decoder<GlobalType>,
    crossinline tagAttributeDecoder: Decoder<TagType.Attribute>,
): Result<CoreExternalType, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        EXTERNAL_TYPE_FUNCTION -> CoreExternalType.Function(
            typeIndexDecoder(context.moduleContext).bind(),
        )
        EXTERNAL_TYPE_TABLE -> CoreExternalType.Table(
            tableTypeDecoder(context.moduleContext).bind(),
        )
        EXTERNAL_TYPE_MEMORY -> CoreExternalType.Memory(
            memoryTypeDecoder(context.moduleContext).bind(),
        )
        EXTERNAL_TYPE_GLOBAL -> CoreExternalType.Global(
            globalTypeDecoder(context.moduleContext).bind(),
        )
        EXTERNAL_TYPE_TAG -> CoreExternalType.Tag(
            attribute = tagAttributeDecoder(context.moduleContext).bind(),
            typeIndex = typeIndexDecoder(context.moduleContext).bind(),
        )
        else -> Err(SectionDecodeError.UnknownImportDescriptor(opcode)).bind<CoreExternalType>()
    }
}

private const val EXTERNAL_TYPE_FUNCTION: UByte = 0x00u
private const val EXTERNAL_TYPE_TABLE: UByte = 0x01u
private const val EXTERNAL_TYPE_MEMORY: UByte = 0x02u
private const val EXTERNAL_TYPE_GLOBAL: UByte = 0x03u
private const val EXTERNAL_TYPE_TAG: UByte = 0x04u
