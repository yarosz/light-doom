package io.github.charlietap.chasm.validator.validator.component.linking.export

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.Export
import io.github.charlietap.chasm.ast.component.ExportTarget
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentEntityTypeMatcher
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentExternalRegistrar
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentExternalTypeResolver
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentEntityOrigin
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentExportTargetResolver
import io.github.charlietap.chasm.validator.validator.component.linking.type.ComponentExportedEntityType
import io.github.charlietap.chasm.validator.type.component.ComponentEntityTypeMatcher as MatchComponentEntityType
import io.github.charlietap.chasm.validator.validator.component.linking.ComponentExternalRegistrar as RegisterComponentExternal
import io.github.charlietap.chasm.validator.validator.component.type.ComponentExternalTypeResolver as ResolveComponentExternalType

internal fun ComponentExportValidator(
    context: ComponentValidationContext,
    export: Export,
): Result<Unit, ComponentValidatorError> = ComponentExportValidator(
    context = context,
    export = export,
    targetResolver = ::ComponentExportTargetResolver,
    externalTypeResolver = ::ResolveComponentExternalType,
    entityTypeMatcher = ::MatchComponentEntityType,
    exportedEntityType = ::ComponentExportedEntityType,
    externalRegistrar = ::RegisterComponentExternal,
)

internal inline fun ComponentExportValidator(
    context: ComponentValidationContext,
    export: Export,
    crossinline targetResolver: (
        ComponentValidationContext,
        ExportTarget,
        Boolean,
    ) -> Result<ComponentEntityType, ComponentValidatorError>,
    crossinline externalTypeResolver: ComponentExternalTypeResolver,
    crossinline entityTypeMatcher: ComponentEntityTypeMatcher,
    crossinline exportedEntityType: (
        ComponentValidationContext,
        ComponentEntityType,
    ) -> ComponentEntityType,
    crossinline externalRegistrar: ComponentExternalRegistrar,
): Result<Unit, ComponentValidatorError> = binding {
    val target = export.target
    if (context.depth == 0 && target is ExportTarget.Component) {
        Err(ComponentValidatorError.InvalidType("root component cannot export a component")).bind<Unit>()
    }
    if (
        context.depth == 0 &&
        target is ExportTarget.Function &&
        target.index.idx in context.frame.importedFunctions
    ) {
        Err(ComponentValidatorError.InvalidType("root component cannot re-export an imported function")).bind<Unit>()
    }

    val actualType = targetResolver(context, target, false).bind()
    val matchedType = export.type?.let { ascription ->
        val ascribedType = externalTypeResolver(context, ascription).bind()
        entityTypeMatcher(context, actualType, ascribedType).bind()
        ascribedType
    } ?: actualType
    val exportType = exportedEntityType(context, matchedType)

    if (target is ExportTarget.Value) {
        targetResolver(context, target, true).bind()
    }
    externalRegistrar(context, export.name, exportType, ComponentEntityOrigin.Export, true).bind()
}
