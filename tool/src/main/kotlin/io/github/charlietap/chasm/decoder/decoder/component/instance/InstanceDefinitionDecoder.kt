package io.github.charlietap.chasm.decoder.decoder.component.instance

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Index.ComponentIndex
import io.github.charlietap.chasm.ast.component.InlineExport
import io.github.charlietap.chasm.ast.component.InstanceDefinition
import io.github.charlietap.chasm.ast.component.InstantiateArgument
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun InstanceDefinitionDecoder(
    context: ComponentDecoderContext,
): Result<InstanceDefinition, WasmDecodeError> = InstanceDefinitionDecoder(
    context = context,
    componentIndexDecoder = ::ComponentIndexDecoder,
    instantiateArgumentDecoder = ::InstantiateArgumentDecoder,
    inlineExportDecoder = ::InlineExportDecoder,
    instantiateArgumentVectorDecoder = ::ReaderVectorDecoder,
    inlineExportVectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun InstanceDefinitionDecoder(
    context: ComponentDecoderContext,
    crossinline componentIndexDecoder: ComponentDecoder<ComponentIndex>,
    noinline instantiateArgumentDecoder: ComponentDecoder<InstantiateArgument>,
    noinline inlineExportDecoder: ComponentDecoder<InlineExport>,
    crossinline instantiateArgumentVectorDecoder: ComponentVectorDecoder<InstantiateArgument>,
    crossinline inlineExportVectorDecoder: ComponentVectorDecoder<InlineExport>,
): Result<InstanceDefinition, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        INSTANCE_INSTANTIATE -> InstanceDefinition.Instantiate(
            componentIndex = componentIndexDecoder(context).bind(),
            args = instantiateArgumentVectorDecoder(context, instantiateArgumentDecoder).bind().vector,
        )

        INSTANCE_INLINE_EXPORTS -> InstanceDefinition.InlineExports(
            inlineExportVectorDecoder(context, inlineExportDecoder).bind().vector,
        )

        else -> Err(ComponentDecodeError.UnknownInstanceExpression(opcode)).bind()
    }
}

private const val INSTANCE_INSTANTIATE: UByte = 0x00u
private const val INSTANCE_INLINE_EXPORTS: UByte = 0x01u
