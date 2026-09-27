package io.github.charlietap.chasm.validator.validator.component.linking.alias

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.OuterAliasTarget
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.validator.context.component.ComponentScopeKind
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentAliasEntityTypeAppender
import io.github.charlietap.chasm.validator.validator.component.linking.type.componentIndex
import io.github.charlietap.chasm.validator.validator.component.linking.type.containsResource

internal fun ComponentOuterAliasValidator(
    context: ComponentValidationContext,
    target: OuterAliasTarget,
): Result<Unit, ComponentValidatorError> = ComponentOuterAliasValidator(
    context = context,
    target = target,
    entityAppender = ::ComponentAliasEntityTypeAppender,
)

internal inline fun ComponentOuterAliasValidator(
    context: ComponentValidationContext,
    target: OuterAliasTarget,
    crossinline entityAppender: (ComponentValidationContext, ComponentEntityType) -> Unit,
): Result<Unit, ComponentValidatorError> {
    if (context.frame.kind != ComponentScopeKind.Component &&
        target !is OuterAliasTarget.CoreType &&
        target !is OuterAliasTarget.Type
    ) {
        return Err(ComponentValidatorError.InvalidAlias("component type outer aliases may only select core or component types"))
    }

    val count = when (target) {
        is OuterAliasTarget.Module -> target.count
        is OuterAliasTarget.CoreType -> target.count
        is OuterAliasTarget.Type -> target.count
        is OuterAliasTarget.Component -> target.count
    }
    val outer = context.outer(count)
        ?: return Err(ComponentValidatorError.InvalidAlias("invalid outer alias count $count"))

    return when (target) {
        is OuterAliasTarget.Module -> {
            val type = outer.coreModules.componentIndex(target.index.idx)
                ?: return Err(ComponentValidatorError.UnknownIndex("core module", target.index.idx))
            entityAppender(context, ComponentEntityType.CoreModule(type))
            Ok(Unit)
        }
        is OuterAliasTarget.CoreType -> {
            val type = outer.coreTypes.componentIndex(target.index.idx)
                ?: return Err(ComponentValidatorError.UnknownIndex("core type", target.index.idx))
            context.frame.coreTypes += type
            Ok(Unit)
        }
        is OuterAliasTarget.Type -> {
            val type = outer.types.componentIndex(target.index.idx)
                ?: return Err(ComponentValidatorError.UnknownIndex("type", target.index.idx))
            val entity = ComponentEntityType.Type(type.definition, type.id)
            if (crossesComponentBoundary(context, count) && entity.containsResource()) {
                return Err(ComponentValidatorError.InvalidAlias("outer type transitively refers to a resource"))
            }
            entityAppender(context, entity)
            Ok(Unit)
        }
        is OuterAliasTarget.Component -> {
            val type = outer.components.componentIndex(target.index.idx)
                ?: return Err(ComponentValidatorError.UnknownIndex("component", target.index.idx))
            entityAppender(context, ComponentEntityType.Component(type.type))
            Ok(Unit)
        }
    }
}

private fun crossesComponentBoundary(context: ComponentValidationContext, count: UInt): Boolean {
    if (count == 0u) return false
    return context.outer(count - 1u)?.kind == ComponentScopeKind.Component
}
