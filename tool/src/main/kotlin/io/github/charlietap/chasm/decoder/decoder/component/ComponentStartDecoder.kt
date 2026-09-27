package io.github.charlietap.chasm.decoder.decoder.component

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Index.ComponentFunctionIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentValueIndex
import io.github.charlietap.chasm.ast.component.StartDefinition
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentFunctionIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentValueIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentStartDecoder(
    context: ComponentDecoderContext,
): Result<StartDefinition, WasmDecodeError> = ComponentStartDecoder(
    context = context,
    functionIndexDecoder = ::ComponentFunctionIndexDecoder,
    valueIndexDecoder = ::ComponentValueIndexDecoder,
    vectorDecoder = ::ReaderVectorDecoder,
)

internal inline fun ComponentStartDecoder(
    context: ComponentDecoderContext,
    crossinline functionIndexDecoder: ComponentDecoder<ComponentFunctionIndex>,
    noinline valueIndexDecoder: ComponentDecoder<ComponentValueIndex>,
    crossinline vectorDecoder: ComponentVectorDecoder<ComponentValueIndex>,
): Result<StartDefinition, WasmDecodeError> = binding {
    StartDefinition(
        functionIndex = functionIndexDecoder(context).bind(),
        args = vectorDecoder(context, valueIndexDecoder).bind().vector,
        resultCount = context.reader.uint(),
    )
}
