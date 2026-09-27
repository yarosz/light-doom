package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.ast.component.TypeBound
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentTypeIndexDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentTypeBoundDecoder(
    context: ComponentDecoderContext,
): Result<TypeBound, WasmDecodeError> = ComponentTypeBoundDecoder(
    context = context,
    typeIndexDecoder = ::ComponentTypeIndexDecoder,
)

internal inline fun ComponentTypeBoundDecoder(
    context: ComponentDecoderContext,
    crossinline typeIndexDecoder: ComponentDecoder<ComponentTypeIndex>,
): Result<TypeBound, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        TYPE_BOUND_EQUALS -> TypeBound.Equals(typeIndexDecoder(context).bind())
        TYPE_BOUND_SUB_RESOURCE -> TypeBound.SubResource
        else -> Err(ComponentTypeDecodeError.UnknownTypeBound(opcode)).bind<TypeBound>()
    }
}

private const val TYPE_BOUND_EQUALS: UByte = 0x00u
private const val TYPE_BOUND_SUB_RESOURCE: UByte = 0x01u
