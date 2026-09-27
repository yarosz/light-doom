package io.github.charlietap.chasm.validator.validator.component.linking.instance

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExportTarget
import io.github.charlietap.chasm.ast.component.InstanceDefinition
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentInstanceType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentRemapping
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentEntityTypesInstantiator
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentImportsMatcher
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentInlineExportTargetResolver
import io.github.charlietap.chasm.validator.validator.component.linking.type.componentIndex
import io.github.charlietap.chasm.validator.type.component.ComponentEntityTypesInstantiator as InstantiateComponentEntityTypes
import io.github.charlietap.chasm.validator.type.component.ComponentImportsMatcher as MatchComponentImports

internal fun ComponentInstantiationValidator(
    context: ComponentValidationContext,
    instance: InstanceDefinition.Instantiate,
): Result<Unit, ComponentValidatorError> = ComponentInstantiationValidator(
    context = context,
    instance = instance,
    targetResolver = ::ComponentInlineExportTargetResolver,
    importsMatcher = ::MatchComponentImports,
    entityTypesInstantiator = ::InstantiateComponentEntityTypes,
)

internal inline fun ComponentInstantiationValidator(
    context: ComponentValidationContext,
    instance: InstanceDefinition.Instantiate,
    crossinline targetResolver: (
        ComponentValidationContext,
        ExportTarget,
        Boolean,
    ) -> Result<ComponentEntityType, ComponentValidatorError>,
    crossinline importsMatcher: ComponentImportsMatcher,
    crossinline entityTypesInstantiator: ComponentEntityTypesInstantiator,
): Result<Unit, ComponentValidatorError> = binding {
    val component = context.frame.components.componentIndex(instance.componentIndex.idx)?.type
        ?: Err(ComponentValidatorError.UnknownIndex("component", instance.componentIndex.idx)).bind()
    val arguments = linkedMapOf<String, ComponentEntityType>()
    val valueTargets = linkedMapOf<String, ExportTarget.Value>()
    instance.args.forEach { argument ->
        val name = argument.name.name
        val target = argument.target
        if (name in arguments) {
            Err(ComponentValidatorError.InvalidInstantiation("duplicate argument $name")).bind<Unit>()
        }
        arguments[name] = targetResolver(context, target, false).bind()
        if (target is ExportTarget.Value) valueTargets[name] = target
    }

    val remapping = ComponentRemapping()
    importsMatcher(context, arguments, component, remapping).bind()
    component.imports.keys.forEach { name ->
        valueTargets[name]?.let { target -> targetResolver(context, target, true).bind() }
    }

    component.definedResources.keys.forEach { resource ->
        val fresh = context.identities.resourceId()
        remapping.addResource(resource, fresh)
        context.frame.definedResources += fresh
    }

    val exports = entityTypesInstantiator(component.exports, remapping)
    val explicitResources = component.explicitResources.entries.associateTo(linkedMapOf()) { (resource, path) ->
        remapping.resource(resource) to path
    }
    context.frame.instances += ComponentInstanceType(
        exports = exports,
        explicitResources = explicitResources,
    )
}
