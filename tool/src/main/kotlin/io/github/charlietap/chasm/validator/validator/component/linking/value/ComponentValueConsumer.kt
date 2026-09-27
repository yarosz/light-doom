package io.github.charlietap.chasm.validator.validator.component.linking.value

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.Index.ComponentValueIndex
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.linking.type.componentIndex

internal fun ComponentValueConsumer(
    context: ComponentValidationContext,
    index: ComponentValueIndex,
): Result<ComponentValueType, ComponentValidatorError> {
    val entry = context.frame.values.componentIndex(index.idx)
        ?: return Err(ComponentValidatorError.UnknownIndex("value", index.idx))

    if (entry.consumed) {
        return Err(ComponentValidatorError.InvalidValueUse(index.idx))
    }

    entry.consumed = true
    return Ok(entry.type)
}
