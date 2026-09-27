package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExternalType
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.ast.component.TypeBound
import io.github.charlietap.chasm.ast.component.ValueBound
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.Decoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentTypeIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.section.index.TypeIndexDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.ast.module.Index.TypeIndex as ModuleTypeIndex

internal fun ComponentExternalTypeDecoder(
    context: ComponentDecoderContext,
): Result<ExternalType, WasmDecodeError> = ComponentExternalTypeDecoder(
    context = context,
    moduleTypeIndexDecoder = ::TypeIndexDecoder,
    componentTypeIndexDecoder = ::ComponentTypeIndexDecoder,
    valueBoundDecoder = ::ComponentValueBoundDecoder,
    typeBoundDecoder = ::ComponentTypeBoundDecoder,
)

internal inline fun ComponentExternalTypeDecoder(
    context: ComponentDecoderContext,
    crossinline moduleTypeIndexDecoder: Decoder<ModuleTypeIndex>,
    crossinline componentTypeIndexDecoder: ComponentDecoder<ComponentTypeIndex>,
    crossinline valueBoundDecoder: ComponentDecoder<ValueBound>,
    crossinline typeBoundDecoder: ComponentDecoder<TypeBound>,
): Result<ExternalType, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        EXTERNAL_TYPE_CORE -> {
            val marker = context.reader.ubyte()
            if (marker != CORE_MODULE_SORT) {
                Err(
                    ComponentTypeDecodeError.InvalidReservedByte(
                        expected = CORE_MODULE_SORT,
                        actual = marker,
                    ),
                ).bind<Unit>()
            }
            ExternalType.CoreModule(moduleTypeIndexDecoder(context.moduleContext).bind())
        }
        EXTERNAL_TYPE_FUNCTION -> ExternalType.Function(componentTypeIndexDecoder(context).bind())
        EXTERNAL_TYPE_VALUE -> ExternalType.Value(valueBoundDecoder(context).bind())
        EXTERNAL_TYPE_TYPE -> ExternalType.Type(typeBoundDecoder(context).bind())
        EXTERNAL_TYPE_COMPONENT -> ExternalType.Component(componentTypeIndexDecoder(context).bind())
        EXTERNAL_TYPE_INSTANCE -> ExternalType.Instance(componentTypeIndexDecoder(context).bind())
        else -> Err(ComponentTypeDecodeError.UnknownExternalType(opcode)).bind<ExternalType>()
    }
}

private const val EXTERNAL_TYPE_CORE: UByte = 0x00u
private const val EXTERNAL_TYPE_FUNCTION: UByte = 0x01u
private const val EXTERNAL_TYPE_VALUE: UByte = 0x02u
private const val EXTERNAL_TYPE_TYPE: UByte = 0x03u
private const val EXTERNAL_TYPE_COMPONENT: UByte = 0x04u
private const val EXTERNAL_TYPE_INSTANCE: UByte = 0x05u
private const val CORE_MODULE_SORT: UByte = 0x11u
