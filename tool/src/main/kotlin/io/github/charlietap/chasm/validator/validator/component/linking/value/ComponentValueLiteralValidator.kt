package io.github.charlietap.chasm.validator.validator.component.linking.value

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ComponentValueLiteral
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentPrimitiveType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun ComponentValueLiteralValidator(
    type: ComponentValueType,
    literal: ComponentValueLiteral,
): Result<Unit, ComponentValidatorError> = ComponentValueLiteralValidator(
    type = type,
    literal = literal,
    binaryLiteralValidator = ::ComponentBinaryLiteralValidator,
)

internal inline fun ComponentValueLiteralValidator(
    type: ComponentValueType,
    literal: ComponentValueLiteral,
    crossinline binaryLiteralValidator: (ComponentValueType, List<UByte>) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = ComponentValueLiteralValidatorRecursive(
    type = type,
    literal = literal,
    binaryLiteralValidator = { binaryType, bytes -> binaryLiteralValidator(binaryType, bytes) },
)

private fun ComponentValueLiteralValidatorRecursive(
    type: ComponentValueType,
    literal: ComponentValueLiteral,
    binaryLiteralValidator: (ComponentValueType, List<UByte>) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    if (literal is ComponentValueLiteral.Binary) {
        binaryLiteralValidator(type, literal.bytes).bind()
        return@binding
    }

    when (type) {
        is ComponentValueType.Primitive -> validatePrimitive(type.type, literal).bind()
        is ComponentValueType.Defined -> {
            val value = type.definition.type as? ComponentDefinedType.Value
                ?: invalidLiteral("component value type index does not refer to a value type").bind()
            validateDefined(value.type, literal, binaryLiteralValidator).bind()
        }
    }
}

private fun validateDefined(
    type: ComponentDefinedValueType,
    literal: ComponentValueLiteral,
    binaryLiteralValidator: (ComponentValueType, List<UByte>) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    when (type) {
        is ComponentDefinedValueType.Primitive -> validatePrimitive(type.type, literal).bind()
        is ComponentDefinedValueType.Record -> {
            val record = literal as? ComponentValueLiteral.Record
                ?: invalidLiteral("expected a record").bind()
            if (record.fields.size != type.fields.size) invalidLiteral("record arity mismatch").bind<Unit>()
            for (index in type.fields.indices) {
                ComponentValueLiteralValidatorRecursive(
                    type.fields[index].type,
                    record.fields[index],
                    binaryLiteralValidator,
                ).bind()
            }
        }
        is ComponentDefinedValueType.Variant -> {
            val variant = literal as? ComponentValueLiteral.Variant
                ?: invalidLiteral("expected a variant").bind()
            val variantValue = variant.value
            val case = type.cases.firstOrNull { case -> case.label == variant.label.name }
                ?: invalidLiteral("unknown variant case ${variant.label.name}").bind()
            val caseType = case.type
            when {
                caseType == null && variantValue != null -> invalidLiteral("variant case has no payload").bind<Unit>()
                caseType != null && variantValue == null -> invalidLiteral("variant case requires a payload").bind<Unit>()
                caseType != null && variantValue != null ->
                    ComponentValueLiteralValidatorRecursive(caseType, variantValue, binaryLiteralValidator).bind()
            }
        }
        is ComponentDefinedValueType.ListValue -> {
            val list = literal as? ComponentValueLiteral.ListValue
                ?: invalidLiteral("expected a list").bind()
            list.elements.forEach { element ->
                ComponentValueLiteralValidatorRecursive(type.element, element, binaryLiteralValidator).bind()
            }
        }
        is ComponentDefinedValueType.FixedLengthList -> {
            val list = literal as? ComponentValueLiteral.ListValue
                ?: invalidLiteral("expected a fixed-length list").bind()
            if (list.elements.size.toUInt() != type.length) {
                invalidLiteral("fixed-length list arity mismatch").bind<Unit>()
            }
            list.elements.forEach { element ->
                ComponentValueLiteralValidatorRecursive(type.element, element, binaryLiteralValidator).bind()
            }
        }
        is ComponentDefinedValueType.Map ->
            invalidLiteral("map literals are not defined by the current binary grammar").bind<Unit>()
        is ComponentDefinedValueType.Tuple -> {
            val tuple = literal as? ComponentValueLiteral.Tuple
                ?: invalidLiteral("expected a tuple").bind()
            if (tuple.elements.size != type.elements.size) invalidLiteral("tuple arity mismatch").bind<Unit>()
            for (index in type.elements.indices) {
                ComponentValueLiteralValidatorRecursive(
                    type.elements[index],
                    tuple.elements[index],
                    binaryLiteralValidator,
                ).bind()
            }
        }
        is ComponentDefinedValueType.Flags -> {
            val flags = literal as? ComponentValueLiteral.Flags
                ?: invalidLiteral("expected flags").bind()
            val labels = linkedSetOf<String>()
            flags.labels.forEach { label ->
                if (!labels.add(label.name) || label.name !in type.labels) {
                    invalidLiteral("invalid flag label").bind<Unit>()
                }
            }
        }
        is ComponentDefinedValueType.Enum -> {
            val enum = literal as? ComponentValueLiteral.Enum
                ?: invalidLiteral("expected an enum").bind()
            if (enum.label.name !in type.labels) invalidLiteral("unknown enum label ${enum.label.name}").bind<Unit>()
        }
        is ComponentDefinedValueType.Option -> when (literal) {
            ComponentValueLiteral.None -> Unit
            is ComponentValueLiteral.Some ->
                ComponentValueLiteralValidatorRecursive(type.value, literal.value, binaryLiteralValidator).bind()
            else -> invalidLiteral("expected an option").bind<Unit>()
        }
        is ComponentDefinedValueType.Result -> when (literal) {
            ComponentValueLiteral.Ok -> if (type.ok != null) invalidLiteral("ok requires a payload").bind<Unit>()
            is ComponentValueLiteral.OkValue -> {
                val ok = type.ok ?: invalidLiteral("ok has no payload type").bind()
                ComponentValueLiteralValidatorRecursive(ok, literal.value, binaryLiteralValidator).bind()
            }
            ComponentValueLiteral.Error -> if (type.error != null) {
                invalidLiteral("error requires a payload").bind<Unit>()
            }
            is ComponentValueLiteral.ErrorValue -> {
                val error = type.error ?: invalidLiteral("error has no payload type").bind()
                ComponentValueLiteralValidatorRecursive(error, literal.value, binaryLiteralValidator).bind()
            }
            else -> invalidLiteral("expected a result").bind<Unit>()
        }
        is ComponentDefinedValueType.Own,
        is ComponentDefinedValueType.Borrow,
        is ComponentDefinedValueType.Stream,
        is ComponentDefinedValueType.Future,
        -> invalidLiteral("this type cannot be represented by a component value definition").bind<Unit>()
    }
}

private fun validatePrimitive(
    type: ComponentPrimitiveType,
    literal: ComponentValueLiteral,
): Result<Unit, ComponentValidatorError> {
    val valid = when (type) {
        ComponentPrimitiveType.Bool -> literal is ComponentValueLiteral.Bool
        ComponentPrimitiveType.S8 -> literal is ComponentValueLiteral.S8
        ComponentPrimitiveType.U8 -> literal is ComponentValueLiteral.U8
        ComponentPrimitiveType.S16 -> literal is ComponentValueLiteral.S16
        ComponentPrimitiveType.U16 -> literal is ComponentValueLiteral.U16
        ComponentPrimitiveType.S32 -> literal is ComponentValueLiteral.S32
        ComponentPrimitiveType.U32 -> literal is ComponentValueLiteral.U32
        ComponentPrimitiveType.S64 -> literal is ComponentValueLiteral.S64
        ComponentPrimitiveType.U64 -> literal is ComponentValueLiteral.U64
        ComponentPrimitiveType.F32 -> literal is ComponentValueLiteral.Nan || literal is ComponentValueLiteral.F32 && !literal.value.isNaN()
        ComponentPrimitiveType.F64 -> literal is ComponentValueLiteral.Nan || literal is ComponentValueLiteral.F64 && !literal.value.isNaN()
        ComponentPrimitiveType.Char -> literal is ComponentValueLiteral.Char && literal.codePoint.isUnicodeScalar()
        ComponentPrimitiveType.String -> literal is ComponentValueLiteral.String && literal.value.isUnicodeScalarString()
        ComponentPrimitiveType.ErrorContext -> false
    }
    return if (valid) Ok(Unit) else invalidLiteral("literal does not match $type")
}

private fun UInt.isUnicodeScalar(): Boolean = this <= MAX_UNICODE && this !in SURROGATES

private fun String.isUnicodeScalarString(): Boolean {
    var index = 0
    while (index < length) {
        val current = this[index]
        when {
            current.isHighSurrogate() -> {
                if (index + 1 >= length || !this[index + 1].isLowSurrogate()) return false
                index += 2
            }
            current.isLowSurrogate() -> return false
            else -> index += 1
        }
    }
    return true
}

private fun invalidLiteral(reason: String): Result<Nothing, ComponentValidatorError> =
    Err(ComponentValidatorError.InvalidLiteral(reason))

private const val MAX_UNICODE = 0x10FFFFu
private val SURROGATES = 0xD800u..0xDFFFu
