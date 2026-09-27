package io.github.charlietap.chasm.validator.validator.component.linking.instance

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExportTarget
import io.github.charlietap.chasm.ast.component.InstanceDefinition
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentInstanceType
import io.github.charlietap.chasm.type.component.ComponentResourceTypeId
import io.github.charlietap.chasm.validator.context.component.ComponentNameContext
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.canCombineWith
import io.github.charlietap.chasm.validator.type.component.combineEffectiveTypeSizes
import io.github.charlietap.chasm.validator.type.component.effectiveTypeSize
import io.github.charlietap.chasm.validator.validator.component.linking.name.ComponentExportNameValidator
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentInlineExportTargetResolver

internal fun ComponentInlineInstanceValidator(
    context: ComponentValidationContext,
    instance: InstanceDefinition.InlineExports,
): Result<Unit, ComponentValidatorError> = ComponentInlineInstanceValidator(
    context = context,
    instance = instance,
    targetResolver = ::ComponentInlineExportTargetResolver,
    nameValidator = ::ComponentExportNameValidator,
)

internal inline fun ComponentInlineInstanceValidator(
    context: ComponentValidationContext,
    instance: InstanceDefinition.InlineExports,
    crossinline targetResolver: (
        ComponentValidationContext,
        ExportTarget,
        Boolean,
    ) -> Result<ComponentEntityType, ComponentValidatorError>,
    crossinline nameValidator: (
        NameAttributes,
        ComponentEntityType,
        ComponentNameContext,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    val exports = linkedMapOf<String, ComponentEntityType>()
    val names = ComponentNameContext(resourceNamesVisible = false)
    val explicitResources = linkedMapOf<ComponentResourceTypeId, List<String>>()
    var effectiveTypeSize = 1

    instance.exports.forEach { export ->
        val type = targetResolver(context, export.target, false).bind()
        nameValidator(export.name, type, names).bind()
        if (!effectiveTypeSize.canCombineWith(type, context.frame::componentTypeInfo)) {
            com.github.michaelbull.result.Err(
                ComponentValidatorError.InvalidType(EFFECTIVE_TYPE_SIZE_EXCEEDS_LIMIT),
            ).bind<Unit>()
        }
        val target = export.target
        if (target is ExportTarget.Value) {
            targetResolver(context, target, true).bind()
        }

        val name = export.name.name.name
        exports[name] = type
        when (type) {
            is ComponentEntityType.Type -> {
                val resource = type.referenced.type as? ComponentDefinedType.Resource
                if (resource != null) explicitResources[resource.id] = listOf(name)
            }
            is ComponentEntityType.Instance -> type.type.explicitResources.forEach { (resource, path) ->
                explicitResources[resource] = listOf(name) + path
            }
            else -> Unit
        }
        effectiveTypeSize = combineEffectiveTypeSizes(
            effectiveTypeSize,
            type.effectiveTypeSize(context.frame::componentTypeInfo),
        )
    }

    context.frame.instances += ComponentInstanceType(
        exports = exports,
        explicitResources = explicitResources,
    )
}

private const val EFFECTIVE_TYPE_SIZE_EXCEEDS_LIMIT = "effective type size exceeds the limit"
