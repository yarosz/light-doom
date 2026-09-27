package io.github.charlietap.chasm.validator.validator.component.linking

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.validator.context.component.ComponentNameContext
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentTypeVisibilityMode
import io.github.charlietap.chasm.validator.type.component.isExternallyVisible
import io.github.charlietap.chasm.validator.validator.component.linking.name.ComponentExportNameValidator
import io.github.charlietap.chasm.validator.validator.component.linking.name.ComponentImportNameValidator
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentEntityOrigin
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentEntityTypeAppender

internal fun ComponentExternalRegistrar(
    context: ComponentValidationContext,
    name: NameAttributes,
    type: ComponentEntityType,
    origin: ComponentEntityOrigin,
    validateVisibility: Boolean = true,
): Result<Unit, ComponentValidatorError> = ComponentExternalRegistrar(
    context = context,
    name = name,
    type = type,
    origin = origin,
    validateVisibility = validateVisibility,
    importNameValidator = ::ComponentImportNameValidator,
    exportNameValidator = ::ComponentExportNameValidator,
    entityAppender = ::ComponentEntityTypeAppender,
)

internal inline fun ComponentExternalRegistrar(
    context: ComponentValidationContext,
    name: NameAttributes,
    type: ComponentEntityType,
    origin: ComponentEntityOrigin,
    validateVisibility: Boolean,
    crossinline importNameValidator: ComponentExternalNameValidator,
    crossinline exportNameValidator: ComponentExternalNameValidator,
    crossinline entityAppender: (
        ComponentValidationContext,
        ComponentEntityType,
        ComponentEntityOrigin,
        String?,
    ) -> Unit,
): Result<Unit, ComponentValidatorError> = binding {
    val frame = context.frame
    if (origin == ComponentEntityOrigin.Alias) {
        Err(ComponentValidatorError.InvalidComponent("aliases are not external registrations")).bind<Unit>()
    }
    val importing = origin == ComponentEntityOrigin.Import
    val names: ComponentNameContext = if (importing) frame.importNames else frame.exportNames
    val entities: MutableMap<String, ComponentEntityType> = if (importing) frame.imports else frame.exports
    val visibilityMode = if (importing) ComponentTypeVisibilityMode.Import else ComponentTypeVisibilityMode.Export
    if (importing) {
        importNameValidator(name, type, names).bind()
    } else {
        exportNameValidator(name, type, names).bind()
    }

    if (!frame.canAddExternalType(type)) {
        Err(ComponentValidatorError.InvalidType(EFFECTIVE_TYPE_SIZE_EXCEEDS_LIMIT)).bind<Unit>()
    }
    if (!type.isExternallyVisible(
            frame.visibility,
            visibilityMode,
            validateVisibility,
            frame::componentTypeInfo,
        )
    ) {
        Err(ComponentValidatorError.InvalidType("${origin.name.lowercase()} type is not externally visible"))
            .bind<Unit>()
    }

    val rawName = name.name.name
    entityAppender(context, type, origin, rawName)
    entities[rawName] = type
    frame.addExternalType(type)
}

internal typealias ComponentExternalNameValidator = (
    NameAttributes,
    ComponentEntityType,
    ComponentNameContext,
) -> Result<Unit, ComponentValidatorError>

private const val EFFECTIVE_TYPE_SIZE_EXCEEDS_LIMIT = "effective type size exceeds the limit"
