package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.PrimitiveValueType
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentPrimitiveValueTypeDecoder(
    context: ComponentDecoderContext,
): Result<PrimitiveValueType, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        TYPE_BOOL -> ValueType.Bool
        TYPE_S8 -> ValueType.S8
        TYPE_U8 -> ValueType.U8
        TYPE_S16 -> ValueType.S16
        TYPE_U16 -> ValueType.U16
        TYPE_S32 -> ValueType.S32
        TYPE_U32 -> ValueType.U32
        TYPE_S64 -> ValueType.S64
        TYPE_U64 -> ValueType.U64
        TYPE_F32 -> ValueType.F32
        TYPE_F64 -> ValueType.F64
        TYPE_CHAR -> ValueType.Char
        TYPE_STRING -> ValueType.String
        TYPE_ERROR_CONTEXT -> ValueType.ErrorContext
        else -> Err(ComponentTypeDecodeError.UnknownPrimitiveValueType(opcode)).bind<PrimitiveValueType>()
    }
}

private const val TYPE_BOOL: UByte = 0x7Fu
private const val TYPE_S8: UByte = 0x7Eu
private const val TYPE_U8: UByte = 0x7Du
private const val TYPE_S16: UByte = 0x7Cu
private const val TYPE_U16: UByte = 0x7Bu
private const val TYPE_S32: UByte = 0x7Au
private const val TYPE_U32: UByte = 0x79u
private const val TYPE_S64: UByte = 0x78u
private const val TYPE_U64: UByte = 0x77u
private const val TYPE_F32: UByte = 0x76u
private const val TYPE_F64: UByte = 0x75u
private const val TYPE_CHAR: UByte = 0x74u
private const val TYPE_STRING: UByte = 0x73u
private const val TYPE_ERROR_CONTEXT: UByte = 0x64u
