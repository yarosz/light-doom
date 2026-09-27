package io.github.charlietap.chasm.validator.validator.component.core.type

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import io.github.charlietap.chasm.ast.component.CoreTypeDefinition
import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.type.RecursiveType
import io.github.charlietap.chasm.type.component.CoreType
import io.github.charlietap.chasm.type.copy.RecursiveTypeDeepCopier
import io.github.charlietap.chasm.type.rolling.DefinedTypeRoller
import io.github.charlietap.chasm.type.rolling.substitution.RecursiveTypeSubstitutor
import io.github.charlietap.chasm.type.rolling.substitution.Substitution
import io.github.charlietap.chasm.validator.CoreTypeValidator
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.validator.type.RecursiveTypeValidator

internal fun CoreDefinedTypeValidator(
    context: ComponentValidationContext,
    definition: CoreTypeDefinition.DefinedType,
): Result<Unit, ComponentValidatorError> = CoreDefinedTypeValidator(
    context = context,
    definition = definition,
    recursiveTypeValidator = ::RecursiveTypeValidator,
    definedTypeDeriver = ::CoreDefinedTypeDeriver,
)

internal inline fun CoreDefinedTypeValidator(
    context: ComponentValidationContext,
    definition: CoreTypeDefinition.DefinedType,
    crossinline recursiveTypeValidator: CoreTypeValidator<RecursiveType>,
    crossinline definedTypeDeriver: (List<CoreType>, RecursiveType) -> List<DefinedType>,
): Result<Unit, ComponentValidatorError> {
    val recursiveType = definition.type
    if (recursiveType.subTypes.isEmpty()) {
        return com.github.michaelbull.result.Err(
            ComponentValidatorError.InvalidType(EMPTY_RECURSIVE_TYPE),
        )
    }

    val coreTypes = context.frame.coreTypes
    val originalSize = coreTypes.size
    definedTypeDeriver(coreTypes, recursiveType).mapTo(coreTypes, CoreType::Defined)

    return recursiveTypeValidator(context.frame, recursiveType).mapError { error ->
        coreTypes.subList(originalSize, coreTypes.size).clear()
        ComponentValidatorError.EmbeddedModule(error)
    }
}

internal fun CoreDefinedTypeDeriver(
    coreTypes: List<CoreType>,
    recursiveType: RecursiveType,
): List<DefinedType> {
    val unresolvedModuleType = DefinedType(recursiveType, 0)
    val precedingTypes = coreTypes.map { coreType ->
        when (coreType) {
            is CoreType.Defined -> coreType.type
            is CoreType.Module -> unresolvedModuleType
        }
    }
    val copiedType = RecursiveTypeDeepCopier(recursiveType)
    val externallySubstitutedType = RecursiveTypeSubstitutor(
        copiedType,
        Substitution.TypeIndexToDefinedType(precedingTypes),
    )

    return DefinedTypeRoller(coreTypes.size, externallySubstitutedType)
}

private const val EMPTY_RECURSIVE_TYPE = "core recursive type must define at least one subtype"
