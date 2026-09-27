package io.github.charlietap.chasm.validator.validator.component.linking.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExportTarget
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.linking.value.ComponentValueConsumer

internal fun ComponentExportTargetResolver(
    context: ComponentValidationContext,
    target: ExportTarget,
    consumeValue: Boolean = false,
): Result<ComponentEntityType, ComponentValidatorError> = ComponentExportTargetResolver(
    context = context,
    target = target,
    consumeValue = consumeValue,
    aliasType = true,
)

internal fun ComponentInlineExportTargetResolver(
    context: ComponentValidationContext,
    target: ExportTarget,
    consumeValue: Boolean = false,
): Result<ComponentEntityType, ComponentValidatorError> = ComponentExportTargetResolver(
    context = context,
    target = target,
    consumeValue = consumeValue,
    aliasType = false,
)

private fun ComponentExportTargetResolver(
    context: ComponentValidationContext,
    target: ExportTarget,
    consumeValue: Boolean,
    aliasType: Boolean,
): Result<ComponentEntityType, ComponentValidatorError> = binding {
    when (target) {
        is ExportTarget.Module -> {
            val type = context.frame.coreModules.componentIndex(target.index.idx)
                ?: Err(ComponentValidatorError.UnknownIndex("core module", target.index.idx)).bind()
            ComponentEntityType.CoreModule(type)
        }
        is ExportTarget.Function -> {
            val type = context.frame.functions.componentIndex(target.index.idx)
                ?: Err(ComponentValidatorError.UnknownIndex("function", target.index.idx)).bind()
            ComponentEntityType.Function(type)
        }
        is ExportTarget.Value -> {
            val type = if (consumeValue) {
                ComponentValueConsumer(context, target.index).bind()
            } else {
                context.frame.values.componentIndex(target.index.idx)?.type
                    ?: Err(ComponentValidatorError.UnknownIndex("value", target.index.idx)).bind()
            }
            ComponentEntityType.Value(type)
        }
        is ExportTarget.Type -> {
            val type = context.frame.types.componentIndex(target.index.idx)
                ?: Err(ComponentValidatorError.UnknownIndex("type", target.index.idx)).bind()
            ComponentEntityType.Type(
                referenced = type.definition,
                createdId = if (aliasType) context.identities.typeId() else type.id,
            )
        }
        is ExportTarget.Component -> {
            val type = context.frame.components.componentIndex(target.index.idx)
                ?: Err(ComponentValidatorError.UnknownIndex("component", target.index.idx)).bind()
            ComponentEntityType.Component(type.type)
        }
        is ExportTarget.Instance -> {
            val type = context.frame.instances.componentIndex(target.index.idx)
                ?: Err(ComponentValidatorError.UnknownIndex("instance", target.index.idx)).bind()
            ComponentEntityType.Instance(type)
        }
    }
}
