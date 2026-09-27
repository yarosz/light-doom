package io.github.charlietap.chasm.validator.context.scope

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.type.AddressType
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun MemoryTypeScope(
    context: CoreTypeValidationContext,
    type: MemoryType,
    block: (CoreTypeValidationContext) -> Result<Unit, ModuleValidatorError>,
): Result<Unit, ModuleValidatorError> {
    val previousMaximum = context.limitsMaximum
    context.limitsMaximum = when (type.addressType) {
        AddressType.I32 -> MAX_MEMORY32_PAGES
        AddressType.I64 -> MAX_MEMORY64_PAGES
    }

    val result = block(context)
    context.limitsMaximum = previousMaximum
    return result
}

private const val MAX_MEMORY32_PAGES = 65_536uL
private const val MAX_MEMORY64_PAGES = 281_474_976_710_656uL
