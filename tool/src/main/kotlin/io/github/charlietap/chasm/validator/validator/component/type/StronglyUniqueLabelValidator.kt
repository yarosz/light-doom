package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentLabel

internal fun StronglyUniqueLabelValidator(
    labels: List<NameValue>,
): Result<Unit, ComponentValidatorError> = StronglyUniqueLabelValidator(labels, NameValue::name)

internal inline fun <T> StronglyUniqueLabelValidator(
    labels: List<T>,
    crossinline label: (T) -> String,
): Result<Unit, ComponentValidatorError> {
    val unique = HashSet<ComponentLabel>(labels.size)
    labels.forEach { value ->
        val raw = label(value)
        val parsed = ComponentLabel.parse(raw)
            ?: return Err(ComponentValidatorError.InvalidName(raw))
        if (!unique.add(parsed)) return Err(ComponentValidatorError.DuplicateName(raw))
    }
    return Ok(Unit)
}
