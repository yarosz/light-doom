package io.github.charlietap.chasm.validator.type.component

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.ComponentInstanceType
import io.github.charlietap.chasm.type.component.ComponentResourceTypeId
import io.github.charlietap.chasm.type.component.ComponentType
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.type.component.CoreModuleType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun ComponentSubtypeValidator(
    context: ComponentValidationContext,
    actual: ComponentEntityType,
    expected: ComponentEntityType,
    remapping: ComponentRemapping = ComponentRemapping(),
    coreModuleTypeMatcher: (
        ComponentValidationContext,
        CoreModuleType,
        CoreModuleType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = ComponentSubtypeContext(
    context = context,
    remapping = remapping,
    coreModuleTypeMatcher = coreModuleTypeMatcher,
).entity(actual, expected)

internal fun ComponentImportsSubtypeValidator(
    context: ComponentValidationContext,
    arguments: Map<String, ComponentEntityType>,
    component: ComponentType,
    remapping: ComponentRemapping,
    coreModuleTypeMatcher: (
        ComponentValidationContext,
        CoreModuleType,
        CoreModuleType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = ComponentSubtypeContext(
    context = context,
    remapping = remapping,
    coreModuleTypeMatcher = coreModuleTypeMatcher,
).open(
    actual = arguments,
    expected = component.imports,
    resources = component.importedResources,
    kind = IMPORT,
)

private class ComponentSubtypeContext(
    private val context: ComponentValidationContext,
    private val remapping: ComponentRemapping,
    private val coreModuleTypeMatcher: (
        ComponentValidationContext,
        CoreModuleType,
        CoreModuleType,
    ) -> Result<Unit, ComponentValidatorError>,
) {

    fun entity(
        actual: ComponentEntityType,
        expected: ComponentEntityType,
    ): Result<Unit, ComponentValidatorError> = when {
        actual is ComponentEntityType.CoreModule && expected is ComponentEntityType.CoreModule ->
            coreModuleTypeMatcher(context, actual.type, expected.type)
        actual is ComponentEntityType.Function && expected is ComponentEntityType.Function ->
            function(actual.type, expected.type)
        actual is ComponentEntityType.Value && expected is ComponentEntityType.Value ->
            value(actual.type, expected.type)
        actual is ComponentEntityType.Type && expected is ComponentEntityType.Type -> binding {
            defined(actual.referenced.type, expected.referenced.type).bind()
            if (!remapping.addType(expected.createdId, actual.createdId)) mismatch(actual, expected).bind<Unit>()
        }
        actual is ComponentEntityType.Component && expected is ComponentEntityType.Component ->
            component(actual.type, expected.type)
        actual is ComponentEntityType.Instance && expected is ComponentEntityType.Instance ->
            instance(actual.type, expected.type)
        else -> mismatch(actual, expected)
    }

    private fun defined(
        actual: ComponentDefinedType,
        expected: ComponentDefinedType,
    ): Result<Unit, ComponentValidatorError> = when {
        actual is ComponentDefinedType.Value && expected is ComponentDefinedType.Value ->
            definedValue(actual.type, expected.type)
        actual is ComponentDefinedType.Function && expected is ComponentDefinedType.Function ->
            function(actual.type, expected.type)
        actual is ComponentDefinedType.Component && expected is ComponentDefinedType.Component ->
            component(actual.type, expected.type)
        actual is ComponentDefinedType.Instance && expected is ComponentDefinedType.Instance ->
            instance(actual.type, expected.type)
        actual is ComponentDefinedType.Resource && expected is ComponentDefinedType.Resource ->
            equal(resourcesEqual(actual.id, expected.id))
        else -> typeMismatch()
    }

    private fun value(
        actual: ComponentValueType,
        expected: ComponentValueType,
    ): Result<Unit, ComponentValidatorError> = when {
        actual is ComponentValueType.Primitive && expected is ComponentValueType.Primitive ->
            equal(actual.type == expected.type)
        actual is ComponentValueType.Defined && expected is ComponentValueType.Defined ->
            defined(actual.definition.type, expected.definition.type)
        actual is ComponentValueType.Primitive && expected is ComponentValueType.Defined -> {
            val expectedType = expected.definition.type as? ComponentDefinedType.Value
            val primitive = expectedType?.type as? ComponentDefinedValueType.Primitive
            equal(actual.type == primitive?.type)
        }
        actual is ComponentValueType.Defined && expected is ComponentValueType.Primitive -> {
            val actualType = actual.definition.type as? ComponentDefinedType.Value
            val primitive = actualType?.type as? ComponentDefinedValueType.Primitive
            equal(primitive?.type == expected.type)
        }
        else -> typeMismatch()
    }

    private fun definedValue(
        actual: ComponentDefinedValueType,
        expected: ComponentDefinedValueType,
    ): Result<Unit, ComponentValidatorError> = binding {
        when {
            actual is ComponentDefinedValueType.Primitive && expected is ComponentDefinedValueType.Primitive ->
                equal(actual.type == expected.type).bind()
            actual is ComponentDefinedValueType.Record && expected is ComponentDefinedValueType.Record -> {
                equal(actual.fields.size == expected.fields.size).bind()
                for (index in actual.fields.indices) {
                    val actualField = actual.fields[index]
                    val expectedField = expected.fields[index]
                    equal(actualField.label == expectedField.label).bind()
                    value(actualField.type, expectedField.type).bind()
                }
            }
            actual is ComponentDefinedValueType.Variant && expected is ComponentDefinedValueType.Variant -> {
                equal(actual.cases.size == expected.cases.size).bind()
                for (index in actual.cases.indices) {
                    val actualCase = actual.cases[index]
                    val expectedCase = expected.cases[index]
                    equal(actualCase.label == expectedCase.label).bind()
                    optionalValue(actualCase.type, expectedCase.type).bind()
                }
            }
            actual is ComponentDefinedValueType.ListValue && expected is ComponentDefinedValueType.ListValue ->
                value(actual.element, expected.element).bind()
            actual is ComponentDefinedValueType.FixedLengthList &&
                expected is ComponentDefinedValueType.FixedLengthList -> {
                equal(actual.length == expected.length).bind()
                value(actual.element, expected.element).bind()
            }
            actual is ComponentDefinedValueType.Map && expected is ComponentDefinedValueType.Map -> {
                equal(actual.key == expected.key).bind()
                value(actual.value, expected.value).bind()
            }
            actual is ComponentDefinedValueType.Tuple && expected is ComponentDefinedValueType.Tuple -> {
                equal(actual.elements.size == expected.elements.size).bind()
                for (index in actual.elements.indices) {
                    value(actual.elements[index], expected.elements[index]).bind()
                }
            }
            actual is ComponentDefinedValueType.Flags && expected is ComponentDefinedValueType.Flags ->
                equal(actual.labels == expected.labels).bind()
            actual is ComponentDefinedValueType.Enum && expected is ComponentDefinedValueType.Enum ->
                equal(actual.labels == expected.labels).bind()
            actual is ComponentDefinedValueType.Option && expected is ComponentDefinedValueType.Option ->
                value(actual.value, expected.value).bind()
            actual is ComponentDefinedValueType.Result && expected is ComponentDefinedValueType.Result -> {
                optionalValue(actual.ok, expected.ok).bind()
                optionalValue(actual.error, expected.error).bind()
            }
            actual is ComponentDefinedValueType.Own && expected is ComponentDefinedValueType.Own ->
                equal(resourcesEqual(actual.resource, expected.resource)).bind()
            actual is ComponentDefinedValueType.Borrow && expected is ComponentDefinedValueType.Borrow ->
                equal(resourcesEqual(actual.resource, expected.resource)).bind()
            actual is ComponentDefinedValueType.Stream && expected is ComponentDefinedValueType.Stream ->
                optionalValue(actual.element, expected.element).bind()
            actual is ComponentDefinedValueType.Future && expected is ComponentDefinedValueType.Future ->
                optionalValue(actual.value, expected.value).bind()
            else -> typeMismatch().bind<Unit>()
        }
    }

    private fun component(
        actual: ComponentType,
        expected: ComponentType,
    ): Result<Unit, ComponentValidatorError> = binding {
        open(
            actual = expected.imports,
            expected = actual.imports,
            resources = actual.importedResources,
            kind = IMPORT,
        ).bind()
        open(
            actual = actual.exports,
            expected = expected.exports,
            resources = expected.definedResources,
            kind = EXPORT,
        ).bind()
    }

    private fun instance(
        actual: ComponentInstanceType,
        expected: ComponentInstanceType,
    ): Result<Unit, ComponentValidatorError> = binding {
        expected.definedResources.forEach { resource ->
            val path = expected.explicitResources[resource]
            if (path == null) {
                typeMismatch().bind<Unit>()
            } else {
                openResource(actual.exports, resource, path).bind()
            }
        }
        entities(actual.exports, expected.exports, EXPORT).bind()
    }

    fun open(
        actual: Map<String, ComponentEntityType>,
        expected: Map<String, ComponentEntityType>,
        resources: Map<ComponentResourceTypeId, List<String>>,
        kind: String,
    ): Result<Unit, ComponentValidatorError> = binding {
        resources.forEach { (expectedResource, path) ->
            openResource(actual, expectedResource, path).bind()
        }
        entities(actual, expected, kind).bind()
    }

    private fun openResource(
        actual: Map<String, ComponentEntityType>,
        expectedResource: ComponentResourceTypeId,
        path: List<String>,
    ): Result<Unit, ComponentValidatorError> {
        val actualResource = resourceAtPath(actual, path) ?: return Ok(Unit)
        return equal(remapping.addResource(expectedResource, actualResource))
    }

    private fun entities(
        actual: Map<String, ComponentEntityType>,
        expected: Map<String, ComponentEntityType>,
        kind: String,
    ): Result<Unit, ComponentValidatorError> = binding {
        expected.forEach { (name, expectedType) ->
            val actualType = actual[name]
                ?: Err(ComponentValidatorError.TypeMismatch(expected = "$kind `$name`", actual = "missing")).bind()
            entity(actualType, expectedType).bind()
        }
    }

    private fun function(
        actual: ComponentFunctionType,
        expected: ComponentFunctionType,
    ): Result<Unit, ComponentValidatorError> = binding {
        equal(actual.async == expected.async && actual.params.size == expected.params.size).bind()
        for (index in actual.params.indices) {
            val actualParameter = actual.params[index]
            val expectedParameter = expected.params[index]
            equal(actualParameter.label == expectedParameter.label).bind()
            value(actualParameter.type, expectedParameter.type).bind()
        }
        optionalValue(actual.result, expected.result).bind()
    }

    private fun optionalValue(
        actual: ComponentValueType?,
        expected: ComponentValueType?,
    ): Result<Unit, ComponentValidatorError> = when {
        actual == null && expected == null -> Ok(Unit)
        actual != null && expected != null -> value(actual, expected)
        else -> typeMismatch()
    }

    private fun resourcesEqual(
        actual: ComponentResourceTypeId,
        expected: ComponentResourceTypeId,
    ): Boolean = remapping.resource(actual) == remapping.resource(expected)

    private fun mismatch(
        actual: ComponentEntityType,
        expected: ComponentEntityType,
    ): Result<Unit, ComponentValidatorError> = Err(
        ComponentValidatorError.TypeMismatch(
            expected = expected.sortName(),
            actual = actual.sortName(),
        ),
    )

    private fun equal(value: Boolean): Result<Unit, ComponentValidatorError> =
        if (value) Ok(Unit) else typeMismatch()

    private fun typeMismatch(): Result<Unit, ComponentValidatorError> =
        Err(ComponentValidatorError.TypeMismatch())
}

private fun resourceAtPath(
    entities: Map<String, ComponentEntityType>,
    path: List<String>,
): ComponentResourceTypeId? {
    val first = path.firstOrNull() ?: return null
    var entity = entities[first] ?: return null
    for (index in 1 until path.size) {
        val instance = entity as? ComponentEntityType.Instance ?: return null
        entity = instance.type.exports[path[index]] ?: return null
    }
    val type = entity as? ComponentEntityType.Type ?: return null
    return (type.referenced.type as? ComponentDefinedType.Resource)?.id
}

private const val IMPORT = "import"
private const val EXPORT = "export"
