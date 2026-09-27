package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.FunctionType
import io.github.charlietap.chasm.ast.component.LabeledValueType
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentFunctionTypeDecoder(
    context: ComponentDecoderContext,
): Result<FunctionType, WasmDecodeError> = ComponentFunctionTypeDecoder(
    context = context,
    paramDecoder = ::ComponentLabeledValueTypeDecoder,
    paramVectorDecoder = ::ReaderVectorDecoder,
    resultListDecoder = ::ComponentResultListDecoder,
)

internal inline fun ComponentFunctionTypeDecoder(
    context: ComponentDecoderContext,
    noinline paramDecoder: ComponentDecoder<LabeledValueType>,
    crossinline paramVectorDecoder: ComponentVectorDecoder<LabeledValueType>,
    crossinline resultListDecoder: ComponentDecoder<ValueType?>,
): Result<FunctionType, WasmDecodeError> = binding {
    val async = when (val opcode = context.reader.ubyte()) {
        TYPE_FUNCTION -> false
        TYPE_ASYNC_FUNCTION -> true
        else -> Err(ComponentTypeDecodeError.UnknownTypeDefinition(opcode)).bind<Boolean>()
    }

    FunctionType(
        params = paramVectorDecoder(context, paramDecoder).bind().vector,
        result = resultListDecoder(context).bind(),
        async = async,
    )
}

private const val TYPE_FUNCTION: UByte = 0x40u
private const val TYPE_ASYNC_FUNCTION: UByte = 0x43u
