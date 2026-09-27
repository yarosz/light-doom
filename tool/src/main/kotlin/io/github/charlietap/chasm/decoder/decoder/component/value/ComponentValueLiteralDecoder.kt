package io.github.charlietap.chasm.decoder.decoder.component.value

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ComponentValueLiteral
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.error.ComponentValueDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.decoder.ext.toDoubleLe
import io.github.charlietap.chasm.decoder.ext.toFloatLe

internal fun ComponentValueLiteralDecoder(
    context: ComponentDecoderContext,
    type: ValueType,
    payloadSize: UInt,
): Result<ComponentValueLiteral, WasmDecodeError> = ComponentValueLiteralDecoder(
    context = context,
    type = type,
    payloadSize = payloadSize,
    nameDecoder = ::NameValueDecoder,
)

internal inline fun ComponentValueLiteralDecoder(
    context: ComponentDecoderContext,
    type: ValueType,
    payloadSize: UInt,
    crossinline nameDecoder: ReaderDecoder<NameValue>,
): Result<ComponentValueLiteral, WasmDecodeError> = binding {
    when (type) {
        ValueType.Bool -> when (val opcode = context.reader.ubyte()) {
            BOOL_FALSE -> ComponentValueLiteral.Bool(false)
            BOOL_TRUE -> ComponentValueLiteral.Bool(true)
            else -> Err(ComponentValueDecodeError.InvalidBoolean(opcode)).bind()
        }
        ValueType.S8 -> ComponentValueLiteral.S8(context.reader.byte())
        ValueType.U8 -> ComponentValueLiteral.U8(context.reader.ubyte())
        ValueType.S16 -> ComponentValueLiteral.S16(context.reader.short())
        ValueType.U16 -> ComponentValueLiteral.U16(context.reader.ushort())
        ValueType.S32 -> ComponentValueLiteral.S32(context.reader.int())
        ValueType.U32 -> ComponentValueLiteral.U32(context.reader.uint())
        ValueType.S64 -> ComponentValueLiteral.S64(context.reader.long())
        ValueType.U64 -> ComponentValueLiteral.U64(context.reader.ulong())
        ValueType.F32 -> decodeF32(context.reader.bytes(F32_BYTE_SIZE)).bind()
        ValueType.F64 -> decodeF64(context.reader.bytes(F64_BYTE_SIZE)).bind()
        ValueType.Char -> {
            val bytes = context.reader.ubytes(payloadSize)
            ComponentValueLiteral.Char(decodeUnicodeScalar(bytes).bind())
        }
        ValueType.String -> ComponentValueLiteral.String(nameDecoder(context).bind().name)
        is ValueType.TypeIndex -> {
            ComponentValueLiteral.Binary(context.reader.ubytes(payloadSize).toList())
        }
        else -> Err(ComponentValueDecodeError.UnsupportedValueType(type.toString())).bind()
    }
}

private fun decodeF32(bytes: ByteArray): Result<ComponentValueLiteral, WasmDecodeError> = binding {
    if (bytes.contentEquals(CANONICAL_F32_NAN)) {
        ComponentValueLiteral.Nan
    } else {
        val value = bytes.toFloatLe()
        if (value.isNaN()) {
            Err(ComponentValueDecodeError.NonCanonicalNan).bind<Unit>()
        }
        ComponentValueLiteral.F32(value)
    }
}

private fun decodeF64(bytes: ByteArray): Result<ComponentValueLiteral, WasmDecodeError> = binding {
    if (bytes.contentEquals(CANONICAL_F64_NAN)) {
        ComponentValueLiteral.Nan
    } else {
        val value = bytes.toDoubleLe()
        if (value.isNaN()) {
            Err(ComponentValueDecodeError.NonCanonicalNan).bind<Unit>()
        }
        ComponentValueLiteral.F64(value)
    }
}

private fun decodeUnicodeScalar(bytes: UByteArray): Result<UInt, WasmDecodeError> =
    unicodeScalar(bytes)?.let(::Ok)
        ?: Err(ComponentValueDecodeError.InvalidUnicode(bytes))

private fun unicodeScalar(bytes: UByteArray): UInt? = when (bytes.size) {
    1 -> bytes[0].toUInt().takeIf { byte -> byte <= ASCII_MAX }
    2 -> {
        val first = bytes[0].toUInt()
        val second = bytes[1].toUInt()
        if (first in UTF8_TWO_BYTE_FIRST && isContinuation(second)) {
            ((first and UTF8_TWO_BYTE_MASK) shl 6) or (second and UTF8_CONTINUATION_MASK)
        } else {
            null
        }
    }
    3 -> {
        val first = bytes[0].toUInt()
        val second = bytes[1].toUInt()
        val third = bytes[2].toUInt()
        val validSecond = when (first) {
            UTF8_THREE_BYTE_MIN -> second in UTF8_THREE_BYTE_MIN_SECOND
            UTF8_SURROGATE_PREFIX -> second in UTF8_NON_SURROGATE_SECOND
            in UTF8_THREE_BYTE_MIDDLE,
            in UTF8_THREE_BYTE_MAX,
            -> isContinuation(second)
            else -> false
        }
        if (validSecond && isContinuation(third)) {
            ((first and UTF8_THREE_BYTE_MASK) shl 12) or
                ((second and UTF8_CONTINUATION_MASK) shl 6) or
                (third and UTF8_CONTINUATION_MASK)
        } else {
            null
        }
    }
    4 -> {
        val first = bytes[0].toUInt()
        val second = bytes[1].toUInt()
        val third = bytes[2].toUInt()
        val fourth = bytes[3].toUInt()
        val validSecond = when (first) {
            UTF8_FOUR_BYTE_MIN -> second in UTF8_FOUR_BYTE_MIN_SECOND
            in UTF8_FOUR_BYTE_MIDDLE -> isContinuation(second)
            UTF8_FOUR_BYTE_MAX -> second in UTF8_FOUR_BYTE_MAX_SECOND
            else -> false
        }
        if (validSecond && isContinuation(third) && isContinuation(fourth)) {
            ((first and UTF8_FOUR_BYTE_MASK) shl 18) or
                ((second and UTF8_CONTINUATION_MASK) shl 12) or
                ((third and UTF8_CONTINUATION_MASK) shl 6) or
                (fourth and UTF8_CONTINUATION_MASK)
        } else {
            null
        }
    }
    else -> null
}

private fun isContinuation(byte: UInt): Boolean = byte in UTF8_CONTINUATION

private const val BOOL_FALSE: UByte = 0x00u
private const val BOOL_TRUE: UByte = 0x01u
private const val F32_BYTE_SIZE = 4
private const val F64_BYTE_SIZE = 8
private const val ASCII_MAX = 0x7Fu
private const val UTF8_TWO_BYTE_MASK = 0x1Fu
private const val UTF8_THREE_BYTE_MASK = 0x0Fu
private const val UTF8_FOUR_BYTE_MASK = 0x07u
private const val UTF8_CONTINUATION_MASK = 0x3Fu
private const val UTF8_THREE_BYTE_MIN = 0xE0u
private const val UTF8_SURROGATE_PREFIX = 0xEDu
private const val UTF8_FOUR_BYTE_MIN = 0xF0u
private const val UTF8_FOUR_BYTE_MAX = 0xF4u
private val UTF8_TWO_BYTE_FIRST = 0xC2u..0xDFu
private val UTF8_THREE_BYTE_MIN_SECOND = 0xA0u..0xBFu
private val UTF8_NON_SURROGATE_SECOND = 0x80u..0x9Fu
private val UTF8_THREE_BYTE_MIDDLE = 0xE1u..0xECu
private val UTF8_THREE_BYTE_MAX = 0xEEu..0xEFu
private val UTF8_FOUR_BYTE_MIN_SECOND = 0x90u..0xBFu
private val UTF8_FOUR_BYTE_MIDDLE = 0xF1u..0xF3u
private val UTF8_FOUR_BYTE_MAX_SECOND = 0x80u..0x8Fu
private val UTF8_CONTINUATION = 0x80u..0xBFu
private val CANONICAL_F32_NAN = byteArrayOf(0x00, 0x00, 0xC0.toByte(), 0x7F)
private val CANONICAL_F64_NAN = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF8.toByte(), 0x7F)
