package io.github.charlietap.chasm.decoder.decoder.component.instance

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreExport
import io.github.charlietap.chasm.ast.component.CoreInstanceDefinition
import io.github.charlietap.chasm.ast.component.CoreInstantiateArgument
import io.github.charlietap.chasm.ast.component.Index.ComponentModuleIndex
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentModuleIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CoreInstanceDefinitionDecoder(
    context: ComponentDecoderContext,
): Result<CoreInstanceDefinition, WasmDecodeError> = CoreInstanceDefinitionDecoder(
    context = context,
    moduleIndexDecoder = ::ComponentModuleIndexDecoder,
    instantiateArgumentDecoder = ::CoreInstantiateArgumentDecoder,
    inlineExportDecoder = ::CoreInlineExportDecoder,
    instantiateArgumentVectorDecoder = ::ReaderVectorDecoder,
    inlineExportVectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun CoreInstanceDefinitionDecoder(
    context: ComponentDecoderContext,
    crossinline moduleIndexDecoder: ComponentDecoder<ComponentModuleIndex>,
    noinline instantiateArgumentDecoder: ComponentDecoder<CoreInstantiateArgument>,
    noinline inlineExportDecoder: ComponentDecoder<CoreExport>,
    crossinline instantiateArgumentVectorDecoder: ComponentVectorDecoder<CoreInstantiateArgument>,
    crossinline inlineExportVectorDecoder: ComponentVectorDecoder<CoreExport>,
): Result<CoreInstanceDefinition, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        CORE_INSTANCE_INSTANTIATE -> CoreInstanceDefinition.Instantiate(
            moduleIndex = moduleIndexDecoder(context).bind(),
            args = instantiateArgumentVectorDecoder(context, instantiateArgumentDecoder).bind().vector,
        )

        CORE_INSTANCE_INLINE_EXPORTS -> CoreInstanceDefinition.InlineExports(
            inlineExportVectorDecoder(context, inlineExportDecoder).bind().vector,
        )

        else -> Err(ComponentDecodeError.UnknownCoreInstanceExpression(opcode)).bind()
    }
}

private const val CORE_INSTANCE_INSTANTIATE: UByte = 0x00u
private const val CORE_INSTANCE_INLINE_EXPORTS: UByte = 0x01u
