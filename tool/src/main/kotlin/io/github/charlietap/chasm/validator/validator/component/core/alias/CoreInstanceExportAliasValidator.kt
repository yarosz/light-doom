package io.github.charlietap.chasm.validator.validator.component.core.alias

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.AliasDefinition
import io.github.charlietap.chasm.ast.component.CoreInstanceExportAliasTarget
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.sortName

internal fun CoreInstanceExportAliasValidator(
    context: ComponentValidationContext,
    alias: AliasDefinition.CoreInstanceExport,
): Result<Unit, ComponentValidatorError> = CoreInstanceExportAliasValidator(
    context = context,
    alias = alias,
    exportResolver = ::CoreInstanceExportResolver,
)

internal inline fun CoreInstanceExportAliasValidator(
    context: ComponentValidationContext,
    alias: AliasDefinition.CoreInstanceExport,
    crossinline exportResolver: (
        ComponentValidationContext,
        CoreInstanceExportAliasTarget,
    ) -> Result<CoreEntityType, ComponentValidatorError>,
): Result<Unit, ComponentValidatorError> = binding {
    val target = alias.target
    val entity = exportResolver(context, target).bind()

    when (target) {
        is CoreInstanceExportAliasTarget.Function -> {
            when (entity) {
                is CoreEntityType.Function -> context.frame.coreFunctions += entity.type
                else -> sortMismatch(FUNCTION_SORT, entity).bind<Unit>()
            }
        }

        is CoreInstanceExportAliasTarget.Table -> {
            when (entity) {
                is CoreEntityType.Table -> context.frame.coreTables += entity.type
                else -> sortMismatch(TABLE_SORT, entity).bind<Unit>()
            }
        }

        is CoreInstanceExportAliasTarget.Memory -> {
            when (entity) {
                is CoreEntityType.Memory -> context.frame.coreMemories += entity.type
                else -> sortMismatch(MEMORY_SORT, entity).bind<Unit>()
            }
        }

        is CoreInstanceExportAliasTarget.Global -> {
            when (entity) {
                is CoreEntityType.Global -> context.frame.coreGlobals += entity.type
                else -> sortMismatch(GLOBAL_SORT, entity).bind<Unit>()
            }
        }

        is CoreInstanceExportAliasTarget.Tag -> {
            when (entity) {
                is CoreEntityType.Tag -> context.frame.coreTags += entity.type
                else -> sortMismatch(TAG_SORT, entity).bind<Unit>()
            }
        }

        is CoreInstanceExportAliasTarget.Type -> {
            when (entity) {
                is CoreEntityType.Type -> context.frame.coreTypes += entity.type
                else -> sortMismatch(TYPE_SORT, entity).bind<Unit>()
            }
        }

        is CoreInstanceExportAliasTarget.Module -> {
            when (entity) {
                is CoreEntityType.Module -> context.frame.coreModules += entity.type
                else -> sortMismatch(MODULE_SORT, entity).bind<Unit>()
            }
        }

        is CoreInstanceExportAliasTarget.Instance -> {
            when (entity) {
                is CoreEntityType.Instance -> context.frame.coreInstances += entity.type
                else -> sortMismatch(INSTANCE_SORT, entity).bind<Unit>()
            }
        }
    }
}

internal fun CoreInstanceExportResolver(
    context: ComponentValidationContext,
    target: CoreInstanceExportAliasTarget,
): Result<CoreEntityType, ComponentValidatorError> {
    val instanceIndex = target.instanceIndex()
    val instance = context.frame.coreInstances.getOrNull(instanceIndex.toInt())
        ?: return Err(ComponentValidatorError.UnknownIndex(CORE_INSTANCE_SORT, instanceIndex))
    val name = target.name().name
    return instance.exports[name]
        ?.let(::Ok)
        ?: Err(ComponentValidatorError.UnknownName(name))
}

private fun CoreInstanceExportAliasTarget.instanceIndex(): UInt = when (this) {
    is CoreInstanceExportAliasTarget.Function -> instance.idx
    is CoreInstanceExportAliasTarget.Table -> instance.idx
    is CoreInstanceExportAliasTarget.Memory -> instance.idx
    is CoreInstanceExportAliasTarget.Global -> instance.idx
    is CoreInstanceExportAliasTarget.Tag -> instance.idx
    is CoreInstanceExportAliasTarget.Type -> instance.idx
    is CoreInstanceExportAliasTarget.Module -> instance.idx
    is CoreInstanceExportAliasTarget.Instance -> instance.idx
}

private fun CoreInstanceExportAliasTarget.name() = when (this) {
    is CoreInstanceExportAliasTarget.Function -> name
    is CoreInstanceExportAliasTarget.Table -> name
    is CoreInstanceExportAliasTarget.Memory -> name
    is CoreInstanceExportAliasTarget.Global -> name
    is CoreInstanceExportAliasTarget.Tag -> name
    is CoreInstanceExportAliasTarget.Type -> name
    is CoreInstanceExportAliasTarget.Module -> name
    is CoreInstanceExportAliasTarget.Instance -> name
}

private fun sortMismatch(
    expected: String,
    actual: CoreEntityType,
): Result<Unit, ComponentValidatorError> = Err(
    ComponentValidatorError.SortMismatch(expected, actual.sortName()),
)

private const val FUNCTION_SORT = "core function"
private const val TABLE_SORT = "core table"
private const val MEMORY_SORT = "core memory"
private const val GLOBAL_SORT = "core global"
private const val TAG_SORT = "core tag"
private const val TYPE_SORT = "core type"
private const val MODULE_SORT = "core module"
private const val INSTANCE_SORT = "core instance"
private const val CORE_INSTANCE_SORT = "core instance"
