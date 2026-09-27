package io.github.charlietap.chasm.validator.validator.component.core.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreExternalType
import io.github.charlietap.chasm.ast.component.CoreModuleDeclaration
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.type.component.CoreEntityType
import io.github.charlietap.chasm.type.component.CoreImportName
import io.github.charlietap.chasm.type.component.CoreModuleType
import io.github.charlietap.chasm.type.component.CoreType
import io.github.charlietap.chasm.validator.ComponentValidator
import io.github.charlietap.chasm.validator.context.component.ComponentScopeKind
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun CoreModuleTypeValidator(
    context: ComponentValidationContext,
    type: CoreTypeDefinition.ModuleType,
): Result<CoreModuleType, ComponentValidatorError> = CoreModuleTypeValidator(
    context = context,
    type = type,
    definedTypeValidator = ::CoreDefinedTypeValidator,
    externalTypeValidator = ::CoreExternalTypeValidator,
)

internal inline fun CoreModuleTypeValidator(
    context: ComponentValidationContext,
    type: CoreTypeDefinition.ModuleType,
    crossinline definedTypeValidator: ComponentValidator<CoreTypeDefinition.DefinedType>,
    crossinline externalTypeValidator: (
        ComponentValidationContext,
        CoreExternalType,
    ) -> Result<CoreEntityType, ComponentValidatorError>,
): Result<CoreModuleType, ComponentValidatorError> {
    if (!context.canPush()) {
        return Err(ComponentValidatorError.InvalidType(TYPE_NESTING_TOO_DEEP))
    }
    context.push(ComponentScopeKind.ComponentType)

    val result: Result<CoreModuleType, ComponentValidatorError> = binding {
        val imports = linkedMapOf<CoreImportName, CoreEntityType>()
        val exports = linkedMapOf<String, CoreEntityType>()

        type.declarations.forEach { declaration ->
            when (declaration) {
                is CoreModuleDeclaration.Type -> when (val definition = declaration.type) {
                    is CoreTypeDefinition.DefinedType -> definedTypeValidator(context, definition).bind()
                    is CoreTypeDefinition.ModuleType -> Err(
                        ComponentValidatorError.InvalidType(NESTED_MODULE_TYPE),
                    ).bind<Unit>()
                }

                is CoreModuleDeclaration.Import -> {
                    val name = CoreImportName(
                        module = declaration.moduleName.name,
                        entity = declaration.entityName.name,
                    )
                    if (imports.containsKey(name)) {
                        Err(ComponentValidatorError.DuplicateName("${name.module}:${name.entity}"))
                            .bind<Unit>()
                    }
                    imports[name] = externalTypeValidator(context, declaration.descriptor).bind()
                }

                is CoreModuleDeclaration.Export -> {
                    val name = declaration.name.name
                    if (exports.containsKey(name)) {
                        Err(ComponentValidatorError.DuplicateName(name)).bind<Unit>()
                    }
                    exports[name] = externalTypeValidator(context, declaration.descriptor).bind()
                }

                is CoreModuleDeclaration.OuterAlias -> {
                    val outer = context.outer(declaration.count)
                        ?: Err(
                            ComponentValidatorError.InvalidAlias(
                                "invalid outer alias count ${declaration.count}",
                            ),
                        ).bind()
                    val index = declaration.typeIndex.idx
                    val aliasedType = outer.coreTypes.getOrNull(index.toInt())
                        ?: Err(ComponentValidatorError.UnknownIndex(CORE_TYPE_SORT, index)).bind()
                    if (aliasedType !is CoreType.Defined) {
                        Err(ComponentValidatorError.InvalidAlias(MODULE_TYPE_ALIAS)).bind<Unit>()
                    }
                    context.frame.coreTypes += aliasedType
                }
            }
        }

        CoreModuleType(imports, exports)
    }

    context.pop()
    return result
}

private const val CORE_TYPE_SORT = "core type"
private const val TYPE_NESTING_TOO_DEEP = "component type nesting is too deep"
private const val NESTED_MODULE_TYPE = "core module types cannot define core module types"
private const val MODULE_TYPE_ALIAS = "core module types cannot alias core module types"
