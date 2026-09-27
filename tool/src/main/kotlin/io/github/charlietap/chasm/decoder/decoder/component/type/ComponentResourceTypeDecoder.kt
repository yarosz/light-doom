package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.ComponentOptionalDecoder
import io.github.charlietap.chasm.decoder.decoder.section.index.FunctionIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.type.value.ValueTypeDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.ast.module.Index.FunctionIndex as ModuleFunctionIndex
import io.github.charlietap.chasm.type.ValueType as CoreValueType

internal fun ComponentResourceTypeDecoder(
    context: ComponentDecoderContext,
): Result<TypeDefinition.Resource, WasmDecodeError> = ComponentResourceTypeDecoder(
    context = context,
    valueTypeDecoder = { componentContext ->
        ValueTypeDecoder(componentContext.moduleContext)
    },
    optionalDestructorDecoder = { scopedContext ->
        ComponentOptionalDecoder(scopedContext) { componentContext ->
            FunctionIndexDecoder(componentContext.moduleContext)
        }
    },
)

internal inline fun ComponentResourceTypeDecoder(
    context: ComponentDecoderContext,
    crossinline valueTypeDecoder: ComponentDecoder<CoreValueType>,
    crossinline optionalDestructorDecoder: ComponentDecoder<ModuleFunctionIndex?>,
): Result<TypeDefinition.Resource, WasmDecodeError> = binding {
    val opcode = context.reader.ubyte()
    if (opcode != TYPE_RESOURCE) {
        Err(ComponentTypeDecodeError.UnknownTypeDefinition(opcode)).bind<Unit>()
    }

    TypeDefinition.Resource(
        representation = valueTypeDecoder(context).bind(),
        destructor = optionalDestructorDecoder(context).bind(),
    )
}

private const val TYPE_RESOURCE: UByte = 0x3Fu
