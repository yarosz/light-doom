package io.github.charlietap.chasm.validator.validator.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import io.github.charlietap.chasm.type.AbstractHeapType
import io.github.charlietap.chasm.type.ConcreteHeapType
import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.type.HeapType
import io.github.charlietap.chasm.validator.CoreTypeValidator
import io.github.charlietap.chasm.validator.context.CoreTypeValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError

internal fun HeapTypeValidator(
    context: CoreTypeValidationContext,
    type: HeapType,
): Result<Unit, ModuleValidatorError> =
    HeapTypeValidator(
        context = context,
        type = type,
        definedTypeValidator = ::DefinedTypeValidator,
    )

internal inline fun HeapTypeValidator(
    context: CoreTypeValidationContext,
    type: HeapType,
    crossinline definedTypeValidator: CoreTypeValidator<DefinedType>,
): Result<Unit, ModuleValidatorError> = binding {
    when (type) {
        is AbstractHeapType -> Unit
        is ConcreteHeapType.Defined -> definedTypeValidator(context, type.definedType).bind()
        is ConcreteHeapType.RecursiveTypeIndex -> Unit
        is ConcreteHeapType.TypeIndex -> context.definedType(type.index).map { }.bind()
    }
}
