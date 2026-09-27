package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.DefinedValueType
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.ast.component.KeyType
import io.github.charlietap.chasm.ast.component.PrimitiveValueType
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentPrimitiveType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.type.component.ComponentVariantCase
import io.github.charlietap.chasm.type.component.LabeledComponentValueType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentTypeEntry
import io.github.charlietap.chasm.validator.type.component.containsBorrow

internal fun ComponentValueTypeResolver(
    context: ComponentValidationContext,
    type: ValueType,
): Result<ComponentValueType, ComponentValidatorError> = ComponentValueTypeResolver(
    context = context,
    type = type,
    indexedValueTypeResolver = ::ComponentValueTypeIndexResolver,
)

internal inline fun ComponentValueTypeResolver(
    context: ComponentValidationContext,
    type: ValueType,
    crossinline indexedValueTypeResolver: ComponentTypeResolver<ComponentTypeIndex, ComponentValueType>,
): Result<ComponentValueType, ComponentValidatorError> = binding {
    when (type) {
        is ValueType.TypeIndex -> indexedValueTypeResolver(context, type.index).bind()
        is PrimitiveValueType -> {
            if (type == ValueType.ErrorContext) requireFeature(context, ComponentFeature.Async).bind()
            ComponentValueType.Primitive(type.semanticType())
        }
    }
}

internal fun ComponentDefinedValueTypeResolver(
    context: ComponentValidationContext,
    type: DefinedValueType,
): Result<ComponentDefinedValueType, ComponentValidatorError> = ComponentDefinedValueTypeResolver(
    context = context,
    type = type,
    valueTypeResolver = ::ComponentValueTypeResolver,
    resourceTypeResolver = ::ComponentResourceTypeIndexResolver,
)

internal inline fun ComponentDefinedValueTypeResolver(
    context: ComponentValidationContext,
    type: DefinedValueType,
    crossinline valueTypeResolver: ComponentTypeResolver<ValueType, ComponentValueType>,
    crossinline resourceTypeResolver: ComponentTypeResolver<ComponentTypeIndex, ComponentTypeEntry>,
): Result<ComponentDefinedValueType, ComponentValidatorError> = binding {
    when (type) {
        is PrimitiveValueType -> {
            if (type == ValueType.ErrorContext) requireFeature(context, ComponentFeature.Async).bind()
            ComponentDefinedValueType.Primitive(type.semanticType())
        }
        is DefinedValueType.Record -> {
            requireNonEmpty(type.fields, RECORD_TYPE).bind()
            StronglyUniqueLabelValidator(type.fields) { field -> field.label.name }.bind()
            ComponentDefinedValueType.Record(
                fields = type.fields.map { field ->
                    LabeledComponentValueType(
                        label = field.label.name,
                        type = valueTypeResolver(context, field.type).bind(),
                    )
                },
            )
        }
        is DefinedValueType.Variant -> {
            requireNonEmpty(type.cases, VARIANT_TYPE).bind()
            StronglyUniqueLabelValidator(type.cases) { case -> case.label.name }.bind()
            ComponentDefinedValueType.Variant(
                cases = type.cases.map { case ->
                    ComponentVariantCase(
                        label = case.label.name,
                        type = case.type?.let { caseType -> valueTypeResolver(context, caseType).bind() },
                    )
                },
            )
        }
        is DefinedValueType.List -> ComponentDefinedValueType.ListValue(
            element = valueTypeResolver(context, type.element).bind(),
        )
        is DefinedValueType.FixedLengthList -> {
            requireFeature(context, ComponentFeature.FixedLengthLists).bind()
            if (type.length == 0u) {
                Err(ComponentValidatorError.InvalidType("fixed-length list length must be greater than zero")).bind()
            }
            ComponentDefinedValueType.FixedLengthList(
                element = valueTypeResolver(context, type.element).bind(),
                length = type.length,
            )
        }
        is DefinedValueType.Map -> {
            requireFeature(context, ComponentFeature.Maps).bind()
            ComponentDefinedValueType.Map(
                key = type.key.valueType(),
                value = valueTypeResolver(context, type.value).bind(),
            )
        }
        is DefinedValueType.Tuple -> {
            requireNonEmpty(type.elements, TUPLE_TYPE).bind()
            ComponentDefinedValueType.Tuple(
                elements = type.elements.map { element -> valueTypeResolver(context, element).bind() },
            )
        }
        is DefinedValueType.Flags -> {
            if (type.labels.isEmpty() || type.labels.size > MAX_FLAGS) {
                Err(ComponentValidatorError.InvalidType("flags must contain between 1 and $MAX_FLAGS labels")).bind()
            }
            StronglyUniqueLabelValidator(type.labels).bind()
            ComponentDefinedValueType.Flags(type.labels.map { label -> label.name })
        }
        is DefinedValueType.Enum -> {
            requireNonEmpty(type.labels, ENUM_TYPE).bind()
            StronglyUniqueLabelValidator(type.labels).bind()
            ComponentDefinedValueType.Enum(type.labels.map { label -> label.name })
        }
        is DefinedValueType.Option -> ComponentDefinedValueType.Option(
            value = valueTypeResolver(context, type.value).bind(),
        )
        is DefinedValueType.Result -> ComponentDefinedValueType.Result(
            ok = type.ok?.let { ok -> valueTypeResolver(context, ok).bind() },
            error = type.error?.let { error -> valueTypeResolver(context, error).bind() },
        )
        is DefinedValueType.Own -> {
            val resource = resourceTypeResolver(context, type.resource).bind()
            ComponentDefinedValueType.Own(
                id = resource.id,
                resource = (resource.type as ComponentDefinedType.Resource).id,
            )
        }
        is DefinedValueType.Borrow -> {
            val resource = resourceTypeResolver(context, type.resource).bind()
            ComponentDefinedValueType.Borrow(
                id = resource.id,
                resource = (resource.type as ComponentDefinedType.Resource).id,
            )
        }
        is DefinedValueType.Stream -> {
            requireFeature(context, ComponentFeature.Async).bind()
            val element = type.element?.let { value -> valueTypeResolver(context, value).bind() }
            if (element?.containsBorrow(context.frame::componentTypeInfo) == true) {
                Err(ComponentValidatorError.InvalidType("stream element cannot contain a borrow type")).bind()
            }
            if (element == ComponentValueType.Primitive(ComponentPrimitiveType.Char)) {
                Err(ComponentValidatorError.InvalidType("stream element cannot be char")).bind()
            }
            ComponentDefinedValueType.Stream(element)
        }
        is DefinedValueType.Future -> {
            requireFeature(context, ComponentFeature.Async).bind()
            val value = type.value?.let { element -> valueTypeResolver(context, element).bind() }
            if (value?.containsBorrow(context.frame::componentTypeInfo) == true) {
                Err(ComponentValidatorError.InvalidType("future value cannot contain a borrow type")).bind()
            }
            ComponentDefinedValueType.Future(value)
        }
    }
}

private fun requireFeature(
    context: ComponentValidationContext,
    feature: ComponentFeature,
): Result<Unit, ComponentValidatorError> = if (feature in context.config.features) {
    com.github.michaelbull.result.Ok(Unit)
} else {
    Err(ComponentValidatorError.FeatureDisabled(feature))
}

private fun requireNonEmpty(
    values: Collection<*>,
    description: String,
): Result<Unit, ComponentValidatorError> = if (values.isNotEmpty()) {
    com.github.michaelbull.result.Ok(Unit)
} else {
    Err(ComponentValidatorError.InvalidType("$description must not be empty"))
}

private fun KeyType.valueType(): ComponentPrimitiveType = when (this) {
    KeyType.Bool -> ComponentPrimitiveType.Bool
    KeyType.S8 -> ComponentPrimitiveType.S8
    KeyType.U8 -> ComponentPrimitiveType.U8
    KeyType.S16 -> ComponentPrimitiveType.S16
    KeyType.U16 -> ComponentPrimitiveType.U16
    KeyType.S32 -> ComponentPrimitiveType.S32
    KeyType.U32 -> ComponentPrimitiveType.U32
    KeyType.S64 -> ComponentPrimitiveType.S64
    KeyType.U64 -> ComponentPrimitiveType.U64
    KeyType.Char -> ComponentPrimitiveType.Char
    KeyType.String -> ComponentPrimitiveType.String
}

private fun PrimitiveValueType.semanticType(): ComponentPrimitiveType = when (this) {
    ValueType.Bool -> ComponentPrimitiveType.Bool
    ValueType.S8 -> ComponentPrimitiveType.S8
    ValueType.U8 -> ComponentPrimitiveType.U8
    ValueType.S16 -> ComponentPrimitiveType.S16
    ValueType.U16 -> ComponentPrimitiveType.U16
    ValueType.S32 -> ComponentPrimitiveType.S32
    ValueType.U32 -> ComponentPrimitiveType.U32
    ValueType.S64 -> ComponentPrimitiveType.S64
    ValueType.U64 -> ComponentPrimitiveType.U64
    ValueType.F32 -> ComponentPrimitiveType.F32
    ValueType.F64 -> ComponentPrimitiveType.F64
    ValueType.Char -> ComponentPrimitiveType.Char
    ValueType.String -> ComponentPrimitiveType.String
    ValueType.ErrorContext -> ComponentPrimitiveType.ErrorContext
}

private const val MAX_FLAGS = 32
private const val RECORD_TYPE = "record"
private const val VARIANT_TYPE = "variant"
private const val TUPLE_TYPE = "tuple"
private const val ENUM_TYPE = "enum"
