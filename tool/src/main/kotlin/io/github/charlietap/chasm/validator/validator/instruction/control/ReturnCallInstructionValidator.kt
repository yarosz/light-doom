package io.github.charlietap.chasm.validator.validator.instruction.control

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.instruction.ControlInstruction
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.matching.ResultTypeMatcher
import io.github.charlietap.chasm.type.matching.TypeMatcher
import io.github.charlietap.chasm.validator.context.ModuleValidationContext
import io.github.charlietap.chasm.validator.error.ModuleValidatorError
import io.github.charlietap.chasm.validator.error.TypeValidatorError
import io.github.charlietap.chasm.validator.ext.functionType
import io.github.charlietap.chasm.validator.ext.popValues
import io.github.charlietap.chasm.validator.ext.pushValues
import io.github.charlietap.chasm.validator.ext.resultType
import io.github.charlietap.chasm.validator.ext.unreachable

internal fun ReturnCallInstructionValidator(
    context: ModuleValidationContext,
    instruction: ControlInstruction.ReturnCall,
): Result<Unit, ModuleValidatorError> =
    ReturnCallInstructionValidator(
        context = context,
        instruction = instruction,
        resultTypeMatcher = ::ResultTypeMatcher,
    )

internal inline fun ReturnCallInstructionValidator(
    context: ModuleValidationContext,
    instruction: ControlInstruction.ReturnCall,
    crossinline resultTypeMatcher: TypeMatcher<ResultType>,
): Result<Unit, ModuleValidatorError> = binding {

    val functionType = context.functionType(instruction.functionIndex).bind()
    val resultType = context.resultType().bind()

    if (!resultTypeMatcher(functionType.results, resultType, context)) {
        Err(TypeValidatorError.TypeMismatch).bind<Unit>()
    }

    context.popValues(functionType.params.types).bind()
    context.pushValues(functionType.results.types)

    context.popValues(context.result?.types ?: emptyList()).bind()
    context.unreachable().bind()
}
