package io.github.charlietap.chasm.validator.validator.component.core.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.type.component.CoreInstanceType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun CoreInstanceTypeSubtypeValidator(
    context: ComponentValidationContext,
    actual: CoreInstanceType,
    expected: CoreInstanceType,
): Result<Unit, ComponentValidatorError> = CoreInstanceTypeSubtypeValidator(
    context = context,
    actual = actual,
    expected = expected,
    entityTypeValidator = ::CoreEntityTypeSubtypeValidator,
)

internal inline fun CoreInstanceTypeSubtypeValidator(
    context: ComponentValidationContext,
    actual: CoreInstanceType,
    expected: CoreInstanceType,
    crossinline entityTypeValidator: (
        ComponentValidationContext,
        CoreEntityType,
        CoreEntityType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    expected.exports.forEach { (name, expectedExport) ->
        val actualExport = actual.exports[name]
            ?: Err(
                ComponentValidatorError.TypeMismatch(
                    expected = "core instance export $name",
                    actual = null,
                ),
            ).bind()
        entityTypeValidator(context, actualExport, expectedExport).bind()
    }
}
