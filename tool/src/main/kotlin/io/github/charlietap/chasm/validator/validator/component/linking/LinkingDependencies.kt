package io.github.charlietap.chasm.validator.validator.component.linking

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.ExternalType
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentRemapping
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentEntityOrigin

internal typealias ComponentExternalTypeResolver = (
    ComponentValidationContext,
    ExternalType,
) -> Result<ComponentEntityType, ComponentValidatorError>

internal typealias ComponentEntityTypeMatcher = (
    ComponentValidationContext,
    ComponentEntityType,
    ComponentEntityType,
) -> Result<Unit, ComponentValidatorError>

internal typealias ComponentExternalRegistrar = (
    ComponentValidationContext,
    NameAttributes,
    ComponentEntityType,
    ComponentEntityOrigin,
    Boolean,
) -> Result<Unit, ComponentValidatorError>

internal typealias ComponentImportsMatcher = (
    ComponentValidationContext,
    Map<String, ComponentEntityType>,
    ComponentType,
    ComponentRemapping,
) -> Result<Unit, ComponentValidatorError>

internal typealias ComponentEntityTypesInstantiator = (
    Map<String, ComponentEntityType>,
    ComponentRemapping,
) -> LinkedHashMap<String, ComponentEntityType>
