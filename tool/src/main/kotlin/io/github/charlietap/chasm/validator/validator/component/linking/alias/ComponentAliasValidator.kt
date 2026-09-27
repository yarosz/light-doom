package io.github.charlietap.chasm.validator.validator.component.linking.alias

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.AliasDefinition
import io.github.charlietap.chasm.ast.component.InstanceExportAliasTarget
import io.github.charlietap.chasm.ast.component.OuterAliasTarget
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.core.alias.CoreInstanceExportAliasValidator

internal fun ComponentAliasValidator(
    context: ComponentValidationContext,
    alias: AliasDefinition,
): Result<Unit, ComponentValidatorError> = ComponentAliasValidator(
    context = context,
    alias = alias,
    instanceExportAliasValidator = ::ComponentInstanceExportAliasValidator,
    coreInstanceExportAliasValidator = ::CoreInstanceExportAliasValidator,
    outerAliasValidator = ::ComponentOuterAliasValidator,
)

internal inline fun ComponentAliasValidator(
    context: ComponentValidationContext,
    alias: AliasDefinition,
    crossinline instanceExportAliasValidator: (
        ComponentValidationContext,
        InstanceExportAliasTarget,
    ) -> Result<Unit, ComponentValidatorError>,
    crossinline coreInstanceExportAliasValidator: (
        ComponentValidationContext,
        AliasDefinition.CoreInstanceExport,
    ) -> Result<Unit, ComponentValidatorError>,
    crossinline outerAliasValidator: (
        ComponentValidationContext,
        OuterAliasTarget,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = when (alias) {
    is AliasDefinition.InstanceExport -> instanceExportAliasValidator(context, alias.target)
    is AliasDefinition.CoreInstanceExport -> coreInstanceExportAliasValidator(context, alias)
    is AliasDefinition.Outer -> outerAliasValidator(context, alias.target)
}
