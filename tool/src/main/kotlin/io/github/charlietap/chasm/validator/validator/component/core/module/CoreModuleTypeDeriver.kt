package io.github.charlietap.chasm.validator.validator.component.core.module

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.Export
import io.github.charlietap.chasm.ast.module.Import
import io.github.charlietap.chasm.ast.module.Module
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.type.component.CoreImportName
import io.github.charlietap.chasm.type.component.CoreModuleType
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun CoreModuleTypeDeriver(
    context: ModuleValidationContext,
    module: Module,
): Result<CoreModuleType, ComponentValidatorError> = CoreModuleTypeDeriver(
    context = context,
    module = module,
    importTypeResolver = ::CoreModuleImportTypeResolver,
    exportTypeResolver = ::CoreModuleExportTypeResolver,
)

internal inline fun CoreModuleTypeDeriver(
    context: ModuleValidationContext,
    module: Module,
    crossinline importTypeResolver: (
        ModuleValidationContext,
        Import,
    ) -> Result<CoreEntityType, ComponentValidatorError>,
    crossinline exportTypeResolver: (
        ModuleValidationContext,
        Export,
    ) -> Result<CoreEntityType, ComponentValidatorError>,
): Result<CoreModuleType, ComponentValidatorError> = binding {
    val imports = linkedMapOf<CoreImportName, CoreEntityType>()
    val exports = linkedMapOf<String, CoreEntityType>()

    module.imports.forEach { import ->
        val name = CoreImportName(import.moduleName.name, import.entityName.name)
        if (imports.containsKey(name)) {
            Err(ComponentValidatorError.DuplicateName("${name.module}:${name.entity}"))
                .bind<Unit>()
        }
        imports[name] = importTypeResolver(context, import).bind()
    }

    module.exports.forEach { export ->
        val name = export.name.name
        if (exports.containsKey(name)) {
            Err(ComponentValidatorError.DuplicateName(name)).bind<Unit>()
        }
        exports[name] = exportTypeResolver(context, export).bind()
    }

    CoreModuleType(imports, exports)
}

internal fun CoreModuleImportTypeResolver(
    context: ModuleValidationContext,
    import: Import,
): Result<CoreEntityType, ComponentValidatorError> = binding {
    when (val descriptor = import.descriptor) {
        is Import.Descriptor.Function -> {
            val index = descriptor.typeIndex.idx
            val type = context.types.getOrNull(index.toInt())
                ?: Err(ComponentValidatorError.UnknownIndex(CORE_TYPE_SORT, index)).bind()
            CoreEntityType.Function(type)
        }

        is Import.Descriptor.Table -> CoreEntityType.Table(descriptor.type)
        is Import.Descriptor.Memory -> CoreEntityType.Memory(descriptor.type)
        is Import.Descriptor.Global -> CoreEntityType.Global(descriptor.type)
        is Import.Descriptor.Tag -> CoreEntityType.Tag(descriptor.type)
    }
}

internal fun CoreModuleExportTypeResolver(
    context: ModuleValidationContext,
    export: Export,
): Result<CoreEntityType, ComponentValidatorError> = binding {
    when (val descriptor = export.descriptor) {
        is Export.Descriptor.Function -> {
            val index = descriptor.functionIndex.idx
            val type = context.functions.getOrNull(index.toInt())
                ?: Err(ComponentValidatorError.UnknownIndex(FUNCTION_SORT, index)).bind()
            CoreEntityType.Function(type)
        }

        is Export.Descriptor.Table -> {
            val index = descriptor.tableIndex.idx
            val type = context.tables.getOrNull(index.toInt())
                ?: Err(ComponentValidatorError.UnknownIndex(TABLE_SORT, index)).bind()
            CoreEntityType.Table(type)
        }

        is Export.Descriptor.Memory -> {
            val index = descriptor.memoryIndex.idx
            val type = context.memories.getOrNull(index.toInt())
                ?: Err(ComponentValidatorError.UnknownIndex(MEMORY_SORT, index)).bind()
            CoreEntityType.Memory(type)
        }

        is Export.Descriptor.Global -> {
            val index = descriptor.globalIndex.idx
            val type = context.globals.getOrNull(index.toInt())
                ?: Err(ComponentValidatorError.UnknownIndex(GLOBAL_SORT, index)).bind()
            CoreEntityType.Global(type)
        }

        is Export.Descriptor.Tag -> {
            val index = descriptor.tagIndex.idx
            val type = context.tags.getOrNull(index.toInt())
                ?: Err(ComponentValidatorError.UnknownIndex(TAG_SORT, index)).bind()
            CoreEntityType.Tag(type)
        }
    }
}

private const val CORE_TYPE_SORT = "core type"
private const val FUNCTION_SORT = "core function"
private const val TABLE_SORT = "core table"
private const val MEMORY_SORT = "core memory"
private const val GLOBAL_SORT = "core global"
private const val TAG_SORT = "core tag"
