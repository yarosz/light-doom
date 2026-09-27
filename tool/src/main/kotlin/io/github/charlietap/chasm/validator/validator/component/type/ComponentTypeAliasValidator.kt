package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.AliasDefinition
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.linking.alias.ComponentInstanceExportAliasValidator
import io.github.charlietap.chasm.validator.validator.component.linking.alias.ComponentOuterAliasValidator

internal fun ComponentTypeAliasValidator(
    context: ComponentValidationContext,
    alias: AliasDefinition,
): Result<Unit, ComponentValidatorError> = ComponentTypeAliasValidator(
    context = context,
    alias = alias,
    instanceExportAliasValidator = ::ComponentInstanceExportAliasValidator,
    outerAliasValidator = ::ComponentOuterAliasValidator,
)

internal inline fun ComponentTypeAliasValidator(
    context: ComponentValidationContext,
    alias: AliasDefinition,
    crossinline instanceExportAliasValidator: ComponentTypeResolver<io.github.charlietap.chasm.ast.component.InstanceExportAliasTarget, Unit>,
    crossinline outerAliasValidator: ComponentTypeResolver<io.github.charlietap.chasm.ast.component.OuterAliasTarget, Unit>,
): Result<Unit, ComponentValidatorError> = when (alias) {
    is AliasDefinition.InstanceExport -> instanceExportAliasValidator(context, alias.target)
    is AliasDefinition.Outer -> outerAliasValidator(context, alias.target)
    is AliasDefinition.CoreInstanceExport -> Err(
        ComponentValidatorError.InvalidAlias("core instance export aliases are not allowed in component type declarations"),
    )
}
