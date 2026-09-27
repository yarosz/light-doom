package io.github.charlietap.chasm.validator.validator.import

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.Import
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.ext.functionType

internal fun FunctionImportValidator(
    context: ModuleValidationContext,
    descriptor: Import.Descriptor.Function,
): Result<Unit, ModuleValidatorError> = binding {
    context.functionType(descriptor.typeIndex).bind()
}
