package io.github.charlietap.chasm.validator.validator.component.core.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import io.github.charlietap.chasm.ast.component.CoreExternalType
import io.github.charlietap.chasm.type.GlobalType
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.type.TableType
import io.github.charlietap.chasm.type.TagType
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.type.component.CoreType
import io.github.charlietap.chasm.type.ext.functionType
import io.github.charlietap.chasm.validator.CoreTypeValidator
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.type.GlobalTypeValidator
import io.github.charlietap.chasm.validator.validator.type.MemoryTypeValidator
import io.github.charlietap.chasm.validator.validator.type.TableTypeValidator
import io.github.charlietap.chasm.validator.validator.type.TagTypeValidator

internal fun CoreExternalTypeValidator(
    context: ComponentValidationContext,
    type: CoreExternalType,
): Result<CoreEntityType, ComponentValidatorError> = CoreExternalTypeValidator(
    context = context,
    type = type,
    tableTypeValidator = ::TableTypeValidator,
    memoryTypeValidator = ::MemoryTypeValidator,
    globalTypeValidator = ::GlobalTypeValidator,
    tagTypeValidator = ::TagTypeValidator,
)

internal inline fun CoreExternalTypeValidator(
    context: ComponentValidationContext,
    type: CoreExternalType,
    crossinline tableTypeValidator: CoreTypeValidator<TableType>,
    crossinline memoryTypeValidator: CoreTypeValidator<MemoryType>,
    crossinline globalTypeValidator: CoreTypeValidator<GlobalType>,
    crossinline tagTypeValidator: CoreTypeValidator<TagType>,
): Result<CoreEntityType, ComponentValidatorError> = binding {
    when (type) {
        is CoreExternalType.Function -> {
            val index = type.typeIndex.idx
            val coreType = context.frame.coreTypes.getOrNull(index.toInt())
                ?: Err(ComponentValidatorError.UnknownIndex(CORE_TYPE_SORT, index)).bind()
            val definedType = (coreType as? CoreType.Defined)?.type
                ?: Err(ComponentValidatorError.SortMismatch(FUNCTION_SORT, MODULE_TYPE_SORT)).bind()
            if (definedType.functionType() == null) {
                Err(ComponentValidatorError.SortMismatch(FUNCTION_SORT, DEFINED_TYPE_SORT)).bind<Unit>()
            }
            CoreEntityType.Function(definedType)
        }

        is CoreExternalType.Table -> {
            tableTypeValidator(context.frame, type.type)
                .mapError(ComponentValidatorError::EmbeddedModule)
                .bind()
            CoreEntityType.Table(type.type)
        }

        is CoreExternalType.Memory -> {
            memoryTypeValidator(context.frame, type.type)
                .mapError(ComponentValidatorError::EmbeddedModule)
                .bind()
            CoreEntityType.Memory(type.type)
        }

        is CoreExternalType.Global -> {
            globalTypeValidator(context.frame, type.type)
                .mapError(ComponentValidatorError::EmbeddedModule)
                .bind()
            CoreEntityType.Global(type.type)
        }

        is CoreExternalType.Tag -> {
            val index = type.typeIndex.idx
            val coreType = context.frame.coreTypes.getOrNull(index.toInt())
                ?: Err(ComponentValidatorError.UnknownIndex(CORE_TYPE_SORT, index)).bind()
            val definedType = (coreType as? CoreType.Defined)?.type
                ?: Err(ComponentValidatorError.SortMismatch(FUNCTION_SORT, MODULE_TYPE_SORT)).bind()
            val functionType = definedType.functionType()
                ?: Err(ComponentValidatorError.SortMismatch(FUNCTION_SORT, DEFINED_TYPE_SORT)).bind()
            val tagType = TagType(type.attribute, index.toInt(), functionType)
            tagTypeValidator(context.frame, tagType)
                .mapError(ComponentValidatorError::EmbeddedModule)
                .bind()
            CoreEntityType.Tag(tagType)
        }
    }
}

private const val CORE_TYPE_SORT = "core type"
private const val FUNCTION_SORT = "core function type"
private const val DEFINED_TYPE_SORT = "core defined type"
private const val MODULE_TYPE_SORT = "core module type"
