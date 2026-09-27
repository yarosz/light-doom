package io.github.charlietap.chasm.validator.validator.component.linking.alias

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.InstanceExportAliasTarget
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.validator.context.component.ComponentScopeKind
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentAliasEntityTypeAppender
import io.github.charlietap.chasm.validator.validator.component.linking.type.componentIndex
import io.github.charlietap.chasm.validator.validator.component.linking.type.sort

internal fun ComponentInstanceExportAliasValidator(
    context: ComponentValidationContext,
    target: InstanceExportAliasTarget,
): Result<Unit, ComponentValidatorError> = ComponentInstanceExportAliasValidator(
    context = context,
    target = target,
    entityAppender = ::ComponentAliasEntityTypeAppender,
)

internal inline fun ComponentInstanceExportAliasValidator(
    context: ComponentValidationContext,
    target: InstanceExportAliasTarget,
    crossinline entityAppender: (ComponentValidationContext, ComponentEntityType) -> Unit,
): Result<Unit, ComponentValidatorError> {
    if (context.frame.kind != ComponentScopeKind.Component &&
        target !is InstanceExportAliasTarget.Type &&
        target !is InstanceExportAliasTarget.Instance
    ) {
        return Err(ComponentValidatorError.InvalidAlias("component type aliases may only select types or instances"))
    }

    val instanceIndex = when (target) {
        is InstanceExportAliasTarget.Module -> target.instance
        is InstanceExportAliasTarget.Function -> target.instance
        is InstanceExportAliasTarget.Value -> target.instance
        is InstanceExportAliasTarget.Type -> target.instance
        is InstanceExportAliasTarget.Component -> target.instance
        is InstanceExportAliasTarget.Instance -> target.instance
    }
    val name = when (target) {
        is InstanceExportAliasTarget.Module -> target.name.name
        is InstanceExportAliasTarget.Function -> target.name.name
        is InstanceExportAliasTarget.Value -> target.name.name
        is InstanceExportAliasTarget.Type -> target.name.name
        is InstanceExportAliasTarget.Component -> target.name.name
        is InstanceExportAliasTarget.Instance -> target.name.name
    }
    val instance = context.frame.instances.componentIndex(instanceIndex.idx)
        ?: return Err(ComponentValidatorError.UnknownIndex("instance", instanceIndex.idx))
    val type = instance.exports[name]
        ?: return Err(ComponentValidatorError.UnknownName(name))
    val expectedSort = when (target) {
        is InstanceExportAliasTarget.Module -> "core module"
        is InstanceExportAliasTarget.Function -> "function"
        is InstanceExportAliasTarget.Value -> "value"
        is InstanceExportAliasTarget.Type -> "type"
        is InstanceExportAliasTarget.Component -> "component"
        is InstanceExportAliasTarget.Instance -> "instance"
    }

    if (type.sort != expectedSort) {
        return Err(ComponentValidatorError.SortMismatch(expectedSort, type.sort))
    }

    entityAppender(context, type)
    return Ok(Unit)
}
