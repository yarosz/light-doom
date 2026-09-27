package io.github.charlietap.chasm.validator.validator.component.core.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.type.FunctionType
import io.github.charlietap.chasm.type.GlobalType
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.type.TableType
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.type.component.CoreInstanceType
import io.github.charlietap.chasm.type.component.CoreModuleType
import io.github.charlietap.chasm.type.component.CoreType
import io.github.charlietap.chasm.type.matching.DefinedTypeMatcher
import io.github.charlietap.chasm.type.matching.FunctionTypeMatcher
import io.github.charlietap.chasm.type.matching.GlobalTypeMatcher
import io.github.charlietap.chasm.type.matching.MemoryTypeMatcher
import io.github.charlietap.chasm.type.matching.TableTypeMatcher
import io.github.charlietap.chasm.type.matching.TypeMatcher
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.sortName

internal fun CoreEntityTypeSubtypeValidator(
    context: ComponentValidationContext,
    actual: CoreEntityType,
    expected: CoreEntityType,
): Result<Unit, ComponentValidatorError> = CoreEntityTypeSubtypeValidator(
    context = context,
    actual = actual,
    expected = expected,
    definedTypeMatcher = ::DefinedTypeMatcher,
    tableTypeMatcher = ::TableTypeMatcher,
    memoryTypeMatcher = ::MemoryTypeMatcher,
    globalTypeMatcher = ::GlobalTypeMatcher,
    functionTypeMatcher = ::FunctionTypeMatcher,
    moduleTypeValidator = ::CoreModuleTypeSubtypeValidator,
    instanceTypeValidator = ::CoreInstanceTypeSubtypeValidator,
)

internal inline fun CoreEntityTypeSubtypeValidator(
    context: ComponentValidationContext,
    actual: CoreEntityType,
    expected: CoreEntityType,
    crossinline definedTypeMatcher: TypeMatcher<DefinedType>,
    crossinline tableTypeMatcher: TypeMatcher<TableType>,
    crossinline memoryTypeMatcher: TypeMatcher<MemoryType>,
    crossinline globalTypeMatcher: TypeMatcher<GlobalType>,
    crossinline functionTypeMatcher: TypeMatcher<FunctionType>,
    crossinline moduleTypeValidator: (
        ComponentValidationContext,
        CoreModuleType,
        CoreModuleType,
    ) -> Result<Unit, ComponentValidatorError>,
    crossinline instanceTypeValidator: (
        ComponentValidationContext,
        CoreInstanceType,
        CoreInstanceType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> {
    val matches = when {
        actual is CoreEntityType.Function && expected is CoreEntityType.Function ->
            definedTypeMatcher(actual.type, expected.type, context.frame)

        actual is CoreEntityType.Table && expected is CoreEntityType.Table ->
            actual.type.addressType == expected.type.addressType &&
                tableTypeMatcher(actual.type, expected.type, context.frame)

        actual is CoreEntityType.Memory && expected is CoreEntityType.Memory ->
            actual.type.addressType == expected.type.addressType &&
                actual.type.shared == expected.type.shared &&
                memoryTypeMatcher(actual.type, expected.type, context.frame)

        actual is CoreEntityType.Global && expected is CoreEntityType.Global ->
            globalTypeMatcher(actual.type, expected.type, context.frame)

        actual is CoreEntityType.Tag && expected is CoreEntityType.Tag ->
            actual.type.attribute == expected.type.attribute &&
                functionTypeMatcher(actual.type.functionType, expected.type.functionType, context.frame)

        actual is CoreEntityType.Type && expected is CoreEntityType.Type -> {
            val actualType = actual.type
            val expectedType = expected.type
            return when {
                actualType is CoreType.Defined && expectedType is CoreType.Defined -> {
                    if (definedTypeMatcher(actualType.type, expectedType.type, context.frame)) {
                        Ok(Unit)
                    } else {
                        typeMismatch(actual, expected)
                    }
                }

                actualType is CoreType.Module && expectedType is CoreType.Module ->
                    moduleTypeValidator(context, actualType.type, expectedType.type)

                else -> typeMismatch(actual, expected)
            }
        }

        actual is CoreEntityType.Module && expected is CoreEntityType.Module ->
            return moduleTypeValidator(context, actual.type, expected.type)

        actual is CoreEntityType.Instance && expected is CoreEntityType.Instance ->
            return instanceTypeValidator(context, actual.type, expected.type)

        else -> false
    }

    return if (matches) Ok(Unit) else typeMismatch(actual, expected)
}

private fun typeMismatch(
    actual: CoreEntityType,
    expected: CoreEntityType,
): Result<Unit, ComponentValidatorError> = Err(
    ComponentValidatorError.TypeMismatch(
        expected = expected.sortName(),
        actual = actual.sortName(),
    ),
)
