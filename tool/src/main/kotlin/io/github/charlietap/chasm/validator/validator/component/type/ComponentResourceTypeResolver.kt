package io.github.charlietap.chasm.validator.validator.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.TypeDefinition
import io.github.charlietap.chasm.type.FunctionType
import io.github.charlietap.chasm.type.NumberType
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.ext.functionType
import io.github.charlietap.chasm.validator.context.component.ComponentScopeKind
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.ast.module.Index.FunctionIndex as ModuleFunctionIndex
import io.github.charlietap.chasm.type.ValueType as CoreValueType

internal fun ComponentResourceTypeResolver(
    context: ComponentValidationContext,
    type: TypeDefinition.Resource,
): Result<ComponentDefinedType.Resource, ComponentValidatorError> = ComponentResourceTypeResolver(
    context = context,
    type = type,
    destructorTypeResolver = ::ComponentResourceDestructorTypeResolver,
)

internal inline fun ComponentResourceTypeResolver(
    context: ComponentValidationContext,
    type: TypeDefinition.Resource,
    crossinline destructorTypeResolver: ComponentTypeResolver<ModuleFunctionIndex, FunctionType>,
): Result<ComponentDefinedType.Resource, ComponentValidatorError> = binding {
    if (context.frame.kind != ComponentScopeKind.Component) {
        Err(ComponentValidatorError.InvalidType("resource definitions are not allowed in component or instance types"))
            .bind<Unit>()
    }

    val representation = type.representation
    if (representation != RESOURCE_REPRESENTATION) {
        Err(ComponentValidatorError.InvalidType("resource representation must be i32")).bind<Unit>()
    }
    type.destructor?.let { destructor ->
        val destructorType = destructorTypeResolver(context, destructor).bind()
        if (destructorType.params.types != listOf(representation) || destructorType.results.types.isNotEmpty()) {
            Err(ComponentValidatorError.InvalidType("resource destructor has an incorrect signature")).bind<Unit>()
        }
    }

    val resource = ComponentDefinedType.Resource(context.identities.resourceId())
    context.frame.localResourceRepresentations[resource.id] = representation
    resource
}

private val RESOURCE_REPRESENTATION = CoreValueType.Number(NumberType.I32)

internal fun ComponentResourceDestructorTypeResolver(
    context: ComponentValidationContext,
    index: ModuleFunctionIndex,
): Result<FunctionType, ComponentValidatorError> {
    val function = context.frame.coreFunctions.getOrNull(index.idx.toInt())
        ?: return Err(ComponentValidatorError.UnknownIndex(CORE_FUNCTION_SORT, index.idx))
    val functionType = function.functionType()
        ?: return Err(ComponentValidatorError.SortMismatch(CORE_FUNCTION_TYPE, CORE_DEFINED_TYPE))
    return Ok(functionType)
}

private const val CORE_FUNCTION_SORT = "core function"
private const val CORE_FUNCTION_TYPE = "core function type"
private const val CORE_DEFINED_TYPE = "core defined type"
