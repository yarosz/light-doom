package io.github.charlietap.chasm.validator.validator.component.core.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.type.component.CoreModuleType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun CoreModuleTypeSubtypeValidator(
    context: ComponentValidationContext,
    actual: CoreModuleType,
    expected: CoreModuleType,
): Result<Unit, ComponentValidatorError> = CoreModuleTypeSubtypeValidator(
    context = context,
    actual = actual,
    expected = expected,
    entityTypeValidator = ::CoreEntityTypeSubtypeValidator,
)

internal inline fun CoreModuleTypeSubtypeValidator(
    context: ComponentValidationContext,
    actual: CoreModuleType,
    expected: CoreModuleType,
    crossinline entityTypeValidator: (
        ComponentValidationContext,
        CoreEntityType,
        CoreEntityType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    actual.imports.forEach { (name, actualImport) ->
        val expectedImport = expected.imports[name]
            ?: Err(
                ComponentValidatorError.TypeMismatch(
                    expected = "core module import ${name.module}:${name.entity}",
                    actual = null,
                ),
            ).bind()
        entityTypeValidator(context, expectedImport, actualImport).bind()
    }

    expected.exports.forEach { (name, expectedExport) ->
        val actualExport = actual.exports[name]
            ?: Err(
                ComponentValidatorError.TypeMismatch(
                    expected = "core module export $name",
                    actual = null,
                ),
            ).bind()
        entityTypeValidator(context, actualExport, expectedExport).bind()
    }
}
