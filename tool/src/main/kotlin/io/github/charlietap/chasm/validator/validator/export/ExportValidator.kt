package io.github.charlietap.chasm.validator.validator.export

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.Export
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ExportValidatorError
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun ExportValidator(
    context: ModuleValidationContext,
    export: Export,
): Result<Unit, ModuleValidatorError> =
    ExportValidator(
        context = context,
        export = export,
        functionExportValidator = ::FunctionExportValidator,
        globalExportValidator = ::GlobalExportValidator,
        memoryExportValidator = ::MemoryExportValidator,
        tableExportValidator = ::TableExportValidator,
        tagExportValidator = ::TagExportValidator,
    )

internal inline fun ExportValidator(
    context: ModuleValidationContext,
    export: Export,
    crossinline functionExportValidator: ModuleValidator<Export.Descriptor.Function>,
    crossinline globalExportValidator: ModuleValidator<Export.Descriptor.Global>,
    crossinline memoryExportValidator: ModuleValidator<Export.Descriptor.Memory>,
    crossinline tableExportValidator: ModuleValidator<Export.Descriptor.Table>,
    crossinline tagExportValidator: ModuleValidator<Export.Descriptor.Tag>,
): Result<Unit, ModuleValidatorError> = binding {
    when (val descriptor = export.descriptor) {
        is Export.Descriptor.Function -> functionExportValidator(context, descriptor).bind()
        is Export.Descriptor.Global -> globalExportValidator(context, descriptor).bind()
        is Export.Descriptor.Memory -> memoryExportValidator(context, descriptor).bind()
        is Export.Descriptor.Table -> tableExportValidator(context, descriptor).bind()
        is Export.Descriptor.Tag -> tagExportValidator(context, descriptor).bind()
    }

    if (!context.exportNames.add(export.name.name)) {
        Err(ExportValidatorError.DuplicateExportNames).bind<Unit>()
    }
}
