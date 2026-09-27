package io.github.charlietap.chasm.validator.validator.component.core.instance

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreExportTarget
import io.github.charlietap.chasm.ast.component.CoreInstance
import io.github.charlietap.chasm.ast.component.CoreInstanceDefinition
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.type.component.CoreInstanceType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.core.type.CoreEntityTypeSubtypeValidator

internal fun CoreInstanceValidator(
    context: ComponentValidationContext,
    instance: CoreInstance,
): Result<Unit, ComponentValidatorError> = CoreInstanceValidator(
    context = context,
    instance = instance,
    instantiateValidator = ::CoreInstanceInstantiateValidator,
    inlineExportsValidator = ::CoreInstanceInlineExportsValidator,
)

internal inline fun CoreInstanceValidator(
    context: ComponentValidationContext,
    instance: CoreInstance,
    crossinline instantiateValidator: (
        ComponentValidationContext,
        CoreInstanceDefinition.Instantiate,
    ) -> Result<CoreInstanceType, ComponentValidatorError>,
    crossinline inlineExportsValidator: (
        ComponentValidationContext,
        CoreInstanceDefinition.InlineExports,
    ) -> Result<CoreInstanceType, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    val instanceType = when (val definition = instance.instance) {
        is CoreInstanceDefinition.Instantiate -> instantiateValidator(context, definition).bind()
        is CoreInstanceDefinition.InlineExports -> inlineExportsValidator(context, definition).bind()
    }
    context.frame.coreInstances += instanceType
}

internal fun CoreInstanceInstantiateValidator(
    context: ComponentValidationContext,
    definition: CoreInstanceDefinition.Instantiate,
): Result<CoreInstanceType, ComponentValidatorError> = CoreInstanceInstantiateValidator(
    context = context,
    definition = definition,
    entityTypeValidator = ::CoreEntityTypeSubtypeValidator,
)

internal inline fun CoreInstanceInstantiateValidator(
    context: ComponentValidationContext,
    definition: CoreInstanceDefinition.Instantiate,
    crossinline entityTypeValidator: (
        ComponentValidationContext,
        CoreEntityType,
        CoreEntityType,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<CoreInstanceType, ComponentValidatorError> = binding {
    val moduleIndex = definition.moduleIndex.idx
    val moduleType = context.frame.coreModules.getOrNull(moduleIndex.toInt())
        ?: Err(ComponentValidatorError.UnknownIndex(CORE_MODULE_SORT, moduleIndex)).bind()
    val arguments = linkedMapOf<String, CoreInstanceType>()

    definition.args.forEach { argument ->
        val name = argument.name.name
        if (arguments.containsKey(name)) {
            Err(ComponentValidatorError.DuplicateName(name)).bind<Unit>()
        }

        val instanceIndex = argument.instanceIndex.idx
        val instanceType = context.frame.coreInstances.getOrNull(instanceIndex.toInt())
            ?: Err(ComponentValidatorError.UnknownIndex(CORE_INSTANCE_SORT, instanceIndex)).bind()
        arguments[name] = instanceType
    }

    moduleType.imports.forEach { (name, expectedType) ->
        val argument = arguments[name.module]
            ?: Err(
                ComponentValidatorError.InvalidInstantiation(
                    "missing core module argument ${name.module}",
                ),
            ).bind()
        val actualType = argument.exports[name.entity]
            ?: Err(
                ComponentValidatorError.InvalidInstantiation(
                    "core module argument ${name.module} has no export ${name.entity}",
                ),
            ).bind()
        entityTypeValidator(context, actualType, expectedType).bind()
    }

    CoreInstanceType(LinkedHashMap(moduleType.exports))
}

internal fun CoreInstanceInlineExportsValidator(
    context: ComponentValidationContext,
    definition: CoreInstanceDefinition.InlineExports,
): Result<CoreInstanceType, ComponentValidatorError> = CoreInstanceInlineExportsValidator(
    context = context,
    definition = definition,
    targetResolver = ::CoreExportTargetResolver,
)

internal inline fun CoreInstanceInlineExportsValidator(
    context: ComponentValidationContext,
    definition: CoreInstanceDefinition.InlineExports,
    crossinline targetResolver: (
        ComponentValidationContext,
        CoreExportTarget,
    ) -> Result<CoreEntityType, ComponentValidatorError>,
): Result<CoreInstanceType, ComponentValidatorError> = binding {
    val exports = linkedMapOf<String, CoreEntityType>()

    definition.exports.forEach { export ->
        val name = export.name.name
        if (exports.containsKey(name)) {
            Err(ComponentValidatorError.DuplicateName(name)).bind<Unit>()
        }
        exports[name] = targetResolver(context, export.target).bind()
    }

    CoreInstanceType(exports)
}

private const val CORE_MODULE_SORT = "core module"
private const val CORE_INSTANCE_SORT = "core instance"
