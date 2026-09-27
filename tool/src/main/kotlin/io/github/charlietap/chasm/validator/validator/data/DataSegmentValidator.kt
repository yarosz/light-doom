package io.github.charlietap.chasm.validator.validator.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.module.DataSegment
import io.github.charlietap.chasm.validator.ModuleValidator
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun DataSegmentValidator(
    context: ModuleValidationContext,
    segment: DataSegment,
): Result<Unit, ModuleValidatorError> =
    DataSegmentValidator(
        context = context,
        segment = segment,
        segmentModeValidator = ::DataSegmentModeValidator,
    )

internal inline fun DataSegmentValidator(
    context: ModuleValidationContext,
    segment: DataSegment,
    crossinline segmentModeValidator: ModuleValidator<DataSegment.Mode>,
): Result<Unit, ModuleValidatorError> = binding {
    segmentModeValidator(context, segment.mode).bind()
}
