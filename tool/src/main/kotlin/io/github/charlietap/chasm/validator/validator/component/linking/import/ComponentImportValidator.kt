package io.github.charlietap.chasm.validator.validator.component.linking.import

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Import
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentExternalRegistrar
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentExternalTypeResolver
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentEntityOrigin
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentImportedEntityType
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentExternalRegistrar as RegisterComponentExternal
import io.github.charlietap.chasm.validator.validator.component.type.ComponentExternalTypeResolver as ResolveComponentExternalType

internal fun ComponentImportValidator(
    context: ComponentValidationContext,
    import: Import,
): Result<Unit, ComponentValidatorError> = ComponentImportValidator(
    context = context,
    import = import,
    externalTypeResolver = ::ResolveComponentExternalType,
    importedEntityType = ::ComponentImportedEntityType,
    externalRegistrar = ::RegisterComponentExternal,
)

internal inline fun ComponentImportValidator(
    context: ComponentValidationContext,
    import: Import,
    crossinline externalTypeResolver: ComponentExternalTypeResolver,
    crossinline importedEntityType: (
        ComponentValidationContext,
        ComponentEntityType,
    ) -> ComponentEntityType,
    crossinline externalRegistrar: ComponentExternalRegistrar,
): Result<Unit, ComponentValidatorError> = binding {
    val resolvedType = externalTypeResolver(context, import.type).bind()
    val type = importedEntityType(context, resolvedType)
    if (context.depth == 0 && type is ComponentEntityType.Component) {
        Err(ComponentValidatorError.InvalidType("root component cannot import a component")).bind<Unit>()
    }
    externalRegistrar(context, import.name, type, ComponentEntityOrigin.Import, true).bind()
}
