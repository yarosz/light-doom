package io.github.charlietap.chasm.validator.type.component

import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.type.component.LabeledComponentValueType

internal fun ComponentDefinedType.typeInfo(
    typeInfo: ComponentTypeInfoLookup,
): ComponentTypeInfo = ComponentTypeInfo(
    effectiveSize = effectiveTypeSize(typeInfo),
    containsBorrow = containsBorrow(typeInfo),
    nestingDepth = nestingDepth(typeInfo),
)

internal fun ComponentDefinedType.effectiveTypeSize(typeInfo: ComponentTypeInfoLookup): Int = when (this) {
    is ComponentDefinedType.Value -> type.effectiveTypeSize(typeInfo)
    is ComponentDefinedType.Function -> componentFunctionEffectiveTypeSize(type.params, type.result, typeInfo)
    is ComponentDefinedType.Component -> componentEffectiveTypeSize(type.imports.values, type.exports.values, typeInfo)
    is ComponentDefinedType.Instance -> componentEffectiveTypeSize(type.exports.values, typeInfo = typeInfo)
    is ComponentDefinedType.Resource -> LEAF_TYPE_SIZE
}

internal fun ComponentEntityType.effectiveTypeSize(typeInfo: ComponentTypeInfoLookup): Int = when (this) {
    is ComponentEntityType.CoreModule -> LEAF_TYPE_SIZE
    is ComponentEntityType.Function -> componentFunctionEffectiveTypeSize(type.params, type.result, typeInfo)
    is ComponentEntityType.Value -> type.effectiveTypeSize(typeInfo)
    is ComponentEntityType.Type -> typeInfo(referenced.id)?.effectiveSize ?: referenced.type.effectiveTypeSize(typeInfo)
    is ComponentEntityType.Component -> componentEffectiveTypeSize(type.imports.values, type.exports.values, typeInfo)
    is ComponentEntityType.Instance -> componentEffectiveTypeSize(type.exports.values, typeInfo = typeInfo)
}

internal fun ComponentValueType.effectiveTypeSize(typeInfo: ComponentTypeInfoLookup): Int = when (this) {
    is ComponentValueType.Primitive -> LEAF_TYPE_SIZE
    is ComponentValueType.Defined ->
        typeInfo(definition.id)?.effectiveSize ?: definition.type.effectiveTypeSize(typeInfo)
}

internal fun ComponentDefinedValueType.effectiveTypeSize(typeInfo: ComponentTypeInfoLookup): Int = when (this) {
    is ComponentDefinedValueType.Primitive,
    is ComponentDefinedValueType.Flags,
    is ComponentDefinedValueType.Enum,
    is ComponentDefinedValueType.Own,
    is ComponentDefinedValueType.Borrow,
    is ComponentDefinedValueType.Stream,
    is ComponentDefinedValueType.Future,
    -> LEAF_TYPE_SIZE
    is ComponentDefinedValueType.Record -> fields.foldTypeSize { field -> field.type.effectiveTypeSize(typeInfo) }
    is ComponentDefinedValueType.Variant -> cases.foldTypeSize { case ->
        case.type?.effectiveTypeSize(typeInfo) ?: 0
    }
    is ComponentDefinedValueType.ListValue -> element.effectiveTypeSize(typeInfo)
    is ComponentDefinedValueType.FixedLengthList -> element.effectiveTypeSize(typeInfo)
    is ComponentDefinedValueType.Map -> combineEffectiveTypeSizes(LEAF_TYPE_SIZE, value.effectiveTypeSize(typeInfo))
    is ComponentDefinedValueType.Tuple -> elements.foldTypeSize { type -> type.effectiveTypeSize(typeInfo) }
    is ComponentDefinedValueType.Option -> value.effectiveTypeSize(typeInfo)
    is ComponentDefinedValueType.Result -> combineEffectiveTypeSizes(
        ok?.effectiveTypeSize(typeInfo) ?: LEAF_TYPE_SIZE,
        error?.effectiveTypeSize(typeInfo) ?: LEAF_TYPE_SIZE,
    )
}

internal fun componentFunctionEffectiveTypeSize(
    params: List<LabeledComponentValueType>,
    result: ComponentValueType?,
    typeInfo: ComponentTypeInfoLookup,
): Int {
    val parameterSize = params.foldTypeSize { parameter -> parameter.type.effectiveTypeSize(typeInfo) }
    return result?.let { type -> combineEffectiveTypeSizes(parameterSize, type.effectiveTypeSize(typeInfo)) }
        ?: parameterSize
}

internal fun componentEffectiveTypeSize(
    entities: Iterable<ComponentEntityType>,
    additionalEntities: Iterable<ComponentEntityType> = emptyList(),
    typeInfo: ComponentTypeInfoLookup,
): Int {
    var size = LEAF_TYPE_SIZE
    entities.forEach { entity -> size = combineEffectiveTypeSizes(size, entity.effectiveTypeSize(typeInfo)) }
    additionalEntities.forEach { entity -> size = combineEffectiveTypeSizes(size, entity.effectiveTypeSize(typeInfo)) }
    return size
}

internal fun ComponentDefinedType.nestingDepth(typeInfo: ComponentTypeInfoLookup): Int = when (this) {
    is ComponentDefinedType.Value -> type.nestingDepth(typeInfo)
    is ComponentDefinedType.Function -> componentFunctionNestingDepth(type.params, type.result, typeInfo)
    is ComponentDefinedType.Component -> componentNestingDepth(type.imports.values, type.exports.values, typeInfo)
    is ComponentDefinedType.Instance -> componentNestingDepth(type.exports.values, typeInfo = typeInfo)
    is ComponentDefinedType.Resource -> LEAF_NESTING_DEPTH
}

internal fun ComponentEntityType.nestingDepth(typeInfo: ComponentTypeInfoLookup): Int = when (this) {
    is ComponentEntityType.CoreModule -> 0
    is ComponentEntityType.Function -> componentFunctionNestingDepth(type.params, type.result, typeInfo)
    is ComponentEntityType.Value -> type.nestingDepth(typeInfo)
    is ComponentEntityType.Type -> typeInfo(referenced.id)?.nestingDepth ?: referenced.type.nestingDepth(typeInfo)
    is ComponentEntityType.Component -> componentNestingDepth(type.imports.values, type.exports.values, typeInfo)
    is ComponentEntityType.Instance -> componentNestingDepth(type.exports.values, typeInfo = typeInfo)
}

internal fun ComponentValueType.nestingDepth(typeInfo: ComponentTypeInfoLookup): Int = when (this) {
    is ComponentValueType.Primitive -> 0
    is ComponentValueType.Defined -> typeInfo(definition.id)?.nestingDepth ?: definition.type.nestingDepth(typeInfo)
}

internal fun ComponentDefinedValueType.nestingDepth(typeInfo: ComponentTypeInfoLookup): Int = when (this) {
    is ComponentDefinedValueType.Primitive,
    is ComponentDefinedValueType.Flags,
    is ComponentDefinedValueType.Enum,
    is ComponentDefinedValueType.Own,
    is ComponentDefinedValueType.Borrow,
    -> LEAF_NESTING_DEPTH
    is ComponentDefinedValueType.Record -> nestedTypeDepth(fields) { field -> field.type.nestingDepth(typeInfo) }
    is ComponentDefinedValueType.Variant -> nestedTypeDepth(cases) { case -> case.type?.nestingDepth(typeInfo) ?: 0 }
    is ComponentDefinedValueType.ListValue -> nestedTypeDepth(element.nestingDepth(typeInfo))
    is ComponentDefinedValueType.FixedLengthList -> nestedTypeDepth(element.nestingDepth(typeInfo))
    is ComponentDefinedValueType.Map -> nestedTypeDepth(value.nestingDepth(typeInfo))
    is ComponentDefinedValueType.Tuple -> nestedTypeDepth(elements) { type -> type.nestingDepth(typeInfo) }
    is ComponentDefinedValueType.Option -> nestedTypeDepth(value.nestingDepth(typeInfo))
    is ComponentDefinedValueType.Result -> nestedTypeDepth(
        maxOf(ok?.nestingDepth(typeInfo) ?: 0, error?.nestingDepth(typeInfo) ?: 0),
    )
    is ComponentDefinedValueType.Stream -> nestedTypeDepth(element?.nestingDepth(typeInfo) ?: 0)
    is ComponentDefinedValueType.Future -> nestedTypeDepth(value?.nestingDepth(typeInfo) ?: 0)
}

internal fun componentFunctionNestingDepth(
    params: List<LabeledComponentValueType>,
    result: ComponentValueType?,
    typeInfo: ComponentTypeInfoLookup,
): Int {
    var depth = result?.nestingDepth(typeInfo) ?: 0
    params.forEach { parameter -> depth = maxOf(depth, parameter.type.nestingDepth(typeInfo)) }
    return nestedTypeDepth(depth)
}

internal fun componentNestingDepth(
    entities: Iterable<ComponentEntityType>,
    additionalEntities: Iterable<ComponentEntityType> = emptyList(),
    typeInfo: ComponentTypeInfoLookup,
): Int = nestedTypeDepth(
    maxOf(
        entities.maximumDepth { entity -> entity.nestingDepth(typeInfo) },
        additionalEntities.maximumDepth { entity -> entity.nestingDepth(typeInfo) },
    ),
)

internal fun combineEffectiveTypeSizes(
    first: Int,
    second: Int,
): Int = minOf(first.toLong() + second, MAX_COMPONENT_TYPE_SIZE.toLong()).toInt()

internal fun Int.canCombineWith(
    type: ComponentEntityType,
    typeInfo: ComponentTypeInfoLookup,
): Boolean = combineEffectiveTypeSizes(this, type.effectiveTypeSize(typeInfo)) < MAX_COMPONENT_TYPE_SIZE

internal const val MAX_COMPONENT_TYPE_SIZE = 1_000_000
internal const val MAX_COMPONENT_TYPE_NESTING_DEPTH = 100

private const val LEAF_TYPE_SIZE = 1
private const val LEAF_NESTING_DEPTH = 1

private inline fun <T> Iterable<T>.foldTypeSize(
    typeSize: (T) -> Int,
): Int = fold(LEAF_TYPE_SIZE) { size, type ->
    combineEffectiveTypeSizes(size, typeSize(type))
}

private fun nestedTypeDepth(depth: Int): Int =
    minOf(depth + 1, MAX_COMPONENT_TYPE_NESTING_DEPTH + 1)

private inline fun <T> Iterable<T>.maximumDepth(
    depth: (T) -> Int,
): Int {
    var maximum = 0
    forEach { type -> maximum = maxOf(maximum, depth(type)) }
    return maximum
}

private inline fun <T> nestedTypeDepth(
    types: Iterable<T>,
    depth: (T) -> Int,
): Int = nestedTypeDepth(types.maximumDepth(depth))
