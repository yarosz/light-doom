package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.DefinedValueType
import io.github.charlietap.chasm.ast.component.FunctionType
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentTypeDefinitionDecoder(
    context: ComponentDecoderContext,
): Result<TypeDefinition, WasmDecodeError> = ComponentTypeDefinitionDecoder(
    context = context,
    definedValueTypeDecoder = ::ComponentDefinedValueTypeDecoder,
    functionTypeDecoder = ::ComponentFunctionTypeDecoder,
    componentTypeDecoder = ::ComponentTypeDecoder,
    instanceTypeDecoder = ::ComponentInstanceTypeDecoder,
    resourceTypeDecoder = ::ComponentResourceTypeDecoder,
)

internal inline fun ComponentTypeDefinitionDecoder(
    context: ComponentDecoderContext,
    crossinline definedValueTypeDecoder: ComponentDecoder<DefinedValueType>,
    crossinline functionTypeDecoder: ComponentDecoder<FunctionType>,
    crossinline componentTypeDecoder: ComponentDecoder<TypeDefinition.Component>,
    crossinline instanceTypeDecoder: ComponentDecoder<TypeDefinition.Instance>,
    crossinline resourceTypeDecoder: ComponentDecoder<TypeDefinition.Resource>,
): Result<TypeDefinition, WasmDecodeError> = binding {
    when (context.reader.peekUByte()) {
        TYPE_FUNCTION,
        TYPE_ASYNC_FUNCTION,
        -> TypeDefinition.Function(functionTypeDecoder(context).bind())
        TYPE_COMPONENT -> componentTypeDecoder(context).bind()
        TYPE_INSTANCE -> instanceTypeDecoder(context).bind()
        TYPE_RESOURCE -> resourceTypeDecoder(context).bind()
        else -> TypeDefinition.Value(definedValueTypeDecoder(context).bind())
    }
}

private const val TYPE_RESOURCE: UByte = 0x3Fu
private const val TYPE_FUNCTION: UByte = 0x40u
private const val TYPE_COMPONENT: UByte = 0x41u
private const val TYPE_INSTANCE: UByte = 0x42u
private const val TYPE_ASYNC_FUNCTION: UByte = 0x43u
