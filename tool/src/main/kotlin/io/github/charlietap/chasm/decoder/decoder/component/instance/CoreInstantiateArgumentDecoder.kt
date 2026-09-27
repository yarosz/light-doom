package io.github.charlietap.chasm.decoder.decoder.component.instance

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreInstantiateArgument
import io.github.charlietap.chasm.ast.component.Index.ComponentModuleInstanceIndex
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentModuleInstanceIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CoreInstantiateArgumentDecoder(
    context: ComponentDecoderContext,
): Result<CoreInstantiateArgument, WasmDecodeError> = CoreInstantiateArgumentDecoder(
    context = context,
    nameValueDecoder = ::NameValueDecoder,
    instanceIndexDecoder = ::ComponentModuleInstanceIndexDecoder,
)

internal inline fun CoreInstantiateArgumentDecoder(
    context: ComponentDecoderContext,
    crossinline nameValueDecoder: ReaderDecoder<NameValue>,
    crossinline instanceIndexDecoder: ComponentDecoder<ComponentModuleInstanceIndex>,
): Result<CoreInstantiateArgument, WasmDecodeError> = binding {
    val name = nameValueDecoder(context).bind()
    val marker = context.reader.ubyte()
    if (marker != CORE_INSTANCE_SORT) {
        Err(ComponentDecodeError.InvalidMarker(CORE_INSTANCE_SORT, marker)).bind<Unit>()
    }

    CoreInstantiateArgument(
        name = name,
        instanceIndex = instanceIndexDecoder(context).bind(),
    )
}

private const val CORE_INSTANCE_SORT: UByte = 0x12u
