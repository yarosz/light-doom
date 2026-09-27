package io.github.charlietap.chasm.validator.validator.component.linking.value

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentPrimitiveType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun ComponentBinaryLiteralValidator(
    type: ComponentValueType,
    bytes: List<UByte>,
): Result<Unit, ComponentValidatorError> {
    val reader = ComponentLiteralReader(bytes)
    if (!reader.value(type) || !reader.exhausted) {
        return Err(ComponentValidatorError.InvalidLiteral("invalid binary literal for component value type"))
    }
    return Ok(Unit)
}

private class ComponentLiteralReader(
    private val bytes: List<UByte>,
) {
    private var position = 0

    val exhausted: Boolean
        get() = position == bytes.size

    fun value(type: ComponentValueType): Boolean = when (type) {
        is ComponentValueType.Primitive -> primitive(type.type)
        is ComponentValueType.Defined -> {
            val value = type.definition.type as? ComponentDefinedType.Value ?: return false
            defined(value.type)
        }
    }

    private fun defined(type: ComponentDefinedValueType): Boolean = when (type) {
        is ComponentDefinedValueType.Primitive -> primitive(type.type)
        is ComponentDefinedValueType.Record -> type.fields.all { field -> value(field.type) }
        is ComponentDefinedValueType.Variant -> {
            val index = unsigned(32)?.toIntOrNull() ?: return false
            val case = type.cases.getOrNull(index) ?: return false
            case.type?.let(::value) ?: true
        }
        is ComponentDefinedValueType.ListValue -> vector(type.element)
        is ComponentDefinedValueType.FixedLengthList -> repeat(type.length, type.element)
        is ComponentDefinedValueType.Map -> false
        is ComponentDefinedValueType.Tuple -> type.elements.all(::value)
        is ComponentDefinedValueType.Flags -> flags(type.labels.size)
        is ComponentDefinedValueType.Enum -> {
            val index = unsigned(32) ?: return false
            index < type.labels.size.toULong()
        }
        is ComponentDefinedValueType.Option -> when (byte()) {
            0u.toUByte() -> true
            1u.toUByte() -> value(type.value)
            else -> false
        }
        is ComponentDefinedValueType.Result -> when (byte()) {
            0u.toUByte() -> type.ok?.let(::value) ?: true
            1u.toUByte() -> type.error?.let(::value) ?: true
            else -> false
        }
        is ComponentDefinedValueType.Own,
        is ComponentDefinedValueType.Borrow,
        is ComponentDefinedValueType.Stream,
        is ComponentDefinedValueType.Future,
        -> false
    }

    private fun primitive(type: ComponentPrimitiveType): Boolean = when (type) {
        ComponentPrimitiveType.Bool -> byte()?.toInt() in 0..1
        ComponentPrimitiveType.S8, ComponentPrimitiveType.U8 -> byte() != null
        ComponentPrimitiveType.S16 -> signed(16)
        ComponentPrimitiveType.U16 -> unsigned(16) != null
        ComponentPrimitiveType.S32 -> signed(32)
        ComponentPrimitiveType.U32 -> unsigned(32) != null
        ComponentPrimitiveType.S64 -> signed(64)
        ComponentPrimitiveType.U64 -> unsigned(64) != null
        ComponentPrimitiveType.F32 -> float32()
        ComponentPrimitiveType.F64 -> float64()
        ComponentPrimitiveType.Char -> unicodeScalar()
        ComponentPrimitiveType.String -> string()
        ComponentPrimitiveType.ErrorContext -> false
    }

    private fun vector(type: ComponentValueType): Boolean {
        val length = unsigned(32) ?: return false
        return repeat(length.toUInt(), type)
    }

    private fun repeat(length: UInt, type: ComponentValueType): Boolean {
        if (length.toULong() > (bytes.size - position).toULong() + 1uL) return false
        repeat(length.toInt()) {
            if (!value(type)) return false
        }
        return true
    }

    private fun flags(count: Int): Boolean {
        val byteCount = (count + 7) / 8
        repeat(byteCount) { index ->
            val value = byte()?.toInt() ?: return false
            if (index == byteCount - 1 && count % 8 != 0) {
                val usedMask = (1 shl (count % 8)) - 1
                if (value and usedMask.inv() != 0) return false
            }
        }
        return true
    }

    private fun float32(): Boolean {
        val bits = fixedUnsigned(4) ?: return false
        val exponent = bits and 0x7F800000uL
        val mantissa = bits and 0x007FFFFFuL
        return exponent != 0x7F800000uL || mantissa == 0uL || bits == 0x7FC00000uL
    }

    private fun float64(): Boolean {
        val bits = fixedUnsigned(8) ?: return false
        val exponent = bits and 0x7FF0000000000000uL
        val mantissa = bits and 0x000FFFFFFFFFFFFFuL
        return exponent != 0x7FF0000000000000uL || mantissa == 0uL || bits == 0x7FF8000000000000uL
    }

    private fun fixedUnsigned(size: Int): ULong? {
        if (position + size > bytes.size) return null
        var result = 0uL
        repeat(size) { index ->
            result = result or (bytes[position++].toULong() shl (index * 8))
        }
        return result
    }

    private fun unsigned(bits: Int): ULong? {
        val maximumBytes = (bits + 6) / 7
        var result = 0uL
        repeat(maximumBytes) { index ->
            val byte = byte()?.toInt() ?: return null
            val payload = byte and 0x7F
            val shift = index * 7
            if (shift < 64) result = result or (payload.toULong() shl shift)
            if (byte and 0x80 == 0) {
                val remainingBits = bits - shift
                if (remainingBits < 7 && payload ushr remainingBits != 0) return null
                return result
            }
        }
        return null
    }

    private fun signed(bits: Int): Boolean {
        val maximumBytes = (bits + 6) / 7
        repeat(maximumBytes) { index ->
            val byte = byte()?.toInt() ?: return false
            if (byte and 0x80 == 0) {
                val remainingBits = bits - index * 7
                if (remainingBits < 7) {
                    val payload = byte and 0x7F
                    val valueMask = (1 shl remainingBits) - 1
                    val unused = payload and valueMask.inv() and 0x7F
                    val sign = payload and (1 shl (remainingBits - 1)) != 0
                    if ((!sign && unused != 0) || (sign && unused != (0x7F and valueMask.inv()))) return false
                }
                return true
            }
        }
        return false
    }

    private fun string(): Boolean {
        val length = unsigned(32) ?: return false
        if (length > Int.MAX_VALUE.toULong() || length.toInt() > bytes.size - position) return false
        val end = position + length.toInt()
        while (position < end) {
            if (!unicodeScalar(end)) return false
        }
        return position == end
    }

    private fun unicodeScalar(end: Int = bytes.size): Boolean {
        val first = byte()?.toInt() ?: return false
        val continuationCount = when (first) {
            in 0x00..0x7F -> 0
            in 0xC2..0xDF -> 1
            in 0xE0..0xEF -> 2
            in 0xF0..0xF4 -> 3
            else -> return false
        }
        if (position + continuationCount > end) return false
        val continuation = IntArray(continuationCount) {
            val next = byte()?.toInt() ?: return false
            if (next !in 0x80..0xBF) return false
            next
        }
        return when (first) {
            0xE0 -> continuation[0] >= 0xA0
            0xED -> continuation[0] <= 0x9F
            0xF0 -> continuation[0] >= 0x90
            0xF4 -> continuation[0] <= 0x8F
            else -> true
        }
    }

    private fun byte(): UByte? {
        val value = bytes.getOrNull(position) ?: return null
        position += 1
        return value
    }

    private fun ULong.toIntOrNull(): Int? = takeIf { it <= Int.MAX_VALUE.toULong() }?.toInt()
}
