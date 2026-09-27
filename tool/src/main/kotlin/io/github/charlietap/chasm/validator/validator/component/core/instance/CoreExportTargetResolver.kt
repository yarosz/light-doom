package io.github.charlietap.chasm.validator.validator.component.core.instance

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.CoreExportTarget
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun CoreExportTargetResolver(
    context: ComponentValidationContext,
    target: CoreExportTarget,
): Result<CoreEntityType, ComponentValidatorError> {
    val frame = context.frame
    return when (target) {
        is CoreExportTarget.Function -> frame.coreFunctions.getOrNull(target.index.idx.toInt())
            ?.let { type -> Ok(CoreEntityType.Function(type)) }
            ?: Err(ComponentValidatorError.UnknownIndex(FUNCTION_SORT, target.index.idx))

        is CoreExportTarget.Table -> frame.coreTables.getOrNull(target.index.idx.toInt())
            ?.let { type -> Ok(CoreEntityType.Table(type)) }
            ?: Err(ComponentValidatorError.UnknownIndex(TABLE_SORT, target.index.idx))

        is CoreExportTarget.Memory -> frame.coreMemories.getOrNull(target.index.idx.toInt())
            ?.let { type -> Ok(CoreEntityType.Memory(type)) }
            ?: Err(ComponentValidatorError.UnknownIndex(MEMORY_SORT, target.index.idx))

        is CoreExportTarget.Global -> frame.coreGlobals.getOrNull(target.index.idx.toInt())
            ?.let { type -> Ok(CoreEntityType.Global(type)) }
            ?: Err(ComponentValidatorError.UnknownIndex(GLOBAL_SORT, target.index.idx))

        is CoreExportTarget.Tag -> frame.coreTags.getOrNull(target.index.idx.toInt())
            ?.let { type -> Ok(CoreEntityType.Tag(type)) }
            ?: Err(ComponentValidatorError.UnknownIndex(TAG_SORT, target.index.idx))

        is CoreExportTarget.Type -> frame.coreTypes.getOrNull(target.index.idx.toInt())
            ?.let { type -> Ok(CoreEntityType.Type(type)) }
            ?: Err(ComponentValidatorError.UnknownIndex(TYPE_SORT, target.index.idx))

        is CoreExportTarget.Module -> frame.coreModules.getOrNull(target.index.idx.toInt())
            ?.let { type -> Ok(CoreEntityType.Module(type)) }
            ?: Err(ComponentValidatorError.UnknownIndex(MODULE_SORT, target.index.idx))

        is CoreExportTarget.Instance -> frame.coreInstances.getOrNull(target.index.idx.toInt())
            ?.let { type -> Ok(CoreEntityType.Instance(type)) }
            ?: Err(ComponentValidatorError.UnknownIndex(INSTANCE_SORT, target.index.idx))
    }
}

private const val FUNCTION_SORT = "core function"
private const val TABLE_SORT = "core table"
private const val MEMORY_SORT = "core memory"
private const val GLOBAL_SORT = "core global"
private const val TAG_SORT = "core tag"
private const val TYPE_SORT = "core type"
private const val MODULE_SORT = "core module"
private const val INSTANCE_SORT = "core instance"
