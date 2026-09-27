package io.github.charlietap.chasm.validator.validator.component.linking.type

import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentItemType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.type.component.ComponentInstanceTypeFreshener
import io.github.charlietap.chasm.validator.type.component.ComponentValueEntry

internal enum class ComponentEntityOrigin {
    Alias,
    Import,
    Export,
}

internal fun ComponentEntityTypeAppender(
    context: ComponentValidationContext,
    type: ComponentEntityType,
    origin: ComponentEntityOrigin = ComponentEntityOrigin.Alias,
    name: String? = null,
) {
    val frame = context.frame
    when (type) {
        is ComponentEntityType.CoreModule -> frame.coreModules += type.type
        is ComponentEntityType.Function -> {
            if (origin == ComponentEntityOrigin.Import) {
                frame.importedFunctions += frame.functions.size.toUInt()
            }
            frame.functions += type.type
        }
        is ComponentEntityType.Value -> frame.values += ComponentValueEntry(
            type = type.type,
            consumed = origin == ComponentEntityOrigin.Export,
        )
        is ComponentEntityType.Type -> {
            frame.addType(frame.typeEntry(type.referenced.copy(id = type.createdId)))
            val resource = type.referenced.type as? ComponentDefinedType.Resource ?: return
            when (origin) {
                ComponentEntityOrigin.Alias -> Unit
                ComponentEntityOrigin.Import -> if (type.createdId == type.referenced.id && name != null) {
                    frame.importedResources[resource.id] = listOf(name)
                }
                ComponentEntityOrigin.Export -> {
                    if (type.createdId == type.referenced.id) {
                        frame.definedResources += resource.id
                    }
                    if (name != null) frame.explicitResources[resource.id] = listOf(name)
                }
            }
        }
        is ComponentEntityType.Component -> frame.components += ComponentItemType(type.type)
        is ComponentEntityType.Instance -> {
            frame.instances += type.type
            when (origin) {
                ComponentEntityOrigin.Alias -> Unit
                ComponentEntityOrigin.Import -> if (name != null) {
                    type.type.explicitResources.forEach { (resource, path) ->
                        if (frame.importedResources[resource]?.isEmpty() == true) {
                            frame.importedResources[resource] = listOf(name) + path
                        }
                    }
                }
                ComponentEntityOrigin.Export -> if (name != null) {
                    type.type.explicitResources.forEach { (resource, path) ->
                        frame.explicitResources[resource] = listOf(name) + path
                    }
                }
            }
        }
    }
}

internal fun ComponentAliasEntityTypeAppender(
    context: ComponentValidationContext,
    type: ComponentEntityType,
) = ComponentEntityTypeAppender(context, type)

internal fun ComponentImportedEntityType(
    context: ComponentValidationContext,
    type: ComponentEntityType,
): ComponentEntityType = when (type) {
    is ComponentEntityType.Instance -> {
        val substitution = ComponentInstanceTypeFreshener(context, type.type)
        substitution.resources.values.forEach { resource ->
            context.frame.importedResources[resource] = emptyList()
        }
        ComponentEntityType.Instance(substitution.type)
    }
    else -> type
}

internal fun ComponentExportedEntityType(
    context: ComponentValidationContext,
    type: ComponentEntityType,
): ComponentEntityType = when (type) {
    is ComponentEntityType.Instance -> {
        val substitution = ComponentInstanceTypeFreshener(context, type.type)
        val entity = ComponentEntityType.Instance(substitution.type)
        context.frame.definedResources += substitution.resources.values
        entity
    }
    else -> type
}
