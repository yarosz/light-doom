package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.validator.ComponentValidator
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError

internal fun CanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
): Result<Unit, ComponentValidatorError> =
    CanonicalDefinitionValidator(
        context = context,
        definition = definition,
        liftValidator = ::LiftCanonicalDefinitionValidator,
        lowerValidator = ::LowerCanonicalDefinitionValidator,
        resourceValidator = ::ResourceCanonicalDefinitionValidator,
        taskReturnValidator = ::TaskReturnCanonicalDefinitionValidator,
        asyncBuiltinValidator = ::AsyncBuiltinCanonicalDefinitionValidator,
        streamFutureValidator = ::StreamFutureCanonicalDefinitionValidator,
        errorContextValidator = ::ErrorContextCanonicalDefinitionValidator,
        threadValidator = ::ThreadCanonicalDefinitionValidator,
    )

internal inline fun CanonicalDefinitionValidator(
    context: ComponentValidationContext,
    definition: CanonicalDefinition,
    crossinline liftValidator: ComponentValidator<CanonicalDefinition.Lift>,
    crossinline lowerValidator: ComponentValidator<CanonicalDefinition.Lower>,
    crossinline resourceValidator: ComponentValidator<CanonicalDefinition>,
    crossinline taskReturnValidator: ComponentValidator<CanonicalDefinition.TaskReturn>,
    crossinline asyncBuiltinValidator: ComponentValidator<CanonicalDefinition>,
    crossinline streamFutureValidator: ComponentValidator<CanonicalDefinition>,
    crossinline errorContextValidator: ComponentValidator<CanonicalDefinition>,
    crossinline threadValidator: ComponentValidator<CanonicalDefinition>,
): Result<Unit, ComponentValidatorError> = when (definition) {
    is CanonicalDefinition.Lift -> liftValidator(context, definition)
    is CanonicalDefinition.Lower -> lowerValidator(context, definition)
    is CanonicalDefinition.ResourceNew,
    is CanonicalDefinition.ResourceDrop,
    is CanonicalDefinition.ResourceRep,
    -> resourceValidator(context, definition)

    is CanonicalDefinition.TaskReturn -> taskReturnValidator(context, definition)
    CanonicalDefinition.BackpressureSet,
    CanonicalDefinition.BackpressureInc,
    CanonicalDefinition.BackpressureDec,
    CanonicalDefinition.TaskCancel,
    is CanonicalDefinition.ContextGet,
    is CanonicalDefinition.ContextSet,
    is CanonicalDefinition.SubtaskCancel,
    CanonicalDefinition.SubtaskDrop,
    CanonicalDefinition.WaitableSetNew,
    is CanonicalDefinition.WaitableSetWait,
    is CanonicalDefinition.WaitableSetPoll,
    CanonicalDefinition.WaitableSetDrop,
    CanonicalDefinition.WaitableJoin,
    is CanonicalDefinition.ThreadYield,
    -> asyncBuiltinValidator(context, definition)

    is CanonicalDefinition.StreamNew,
    is CanonicalDefinition.StreamRead,
    is CanonicalDefinition.StreamWrite,
    is CanonicalDefinition.StreamCancelRead,
    is CanonicalDefinition.StreamCancelWrite,
    is CanonicalDefinition.StreamDropReadable,
    is CanonicalDefinition.StreamDropWritable,
    is CanonicalDefinition.FutureNew,
    is CanonicalDefinition.FutureRead,
    is CanonicalDefinition.FutureWrite,
    is CanonicalDefinition.FutureCancelRead,
    is CanonicalDefinition.FutureCancelWrite,
    is CanonicalDefinition.FutureDropReadable,
    is CanonicalDefinition.FutureDropWritable,
    -> streamFutureValidator(context, definition)

    is CanonicalDefinition.ErrorContextNew,
    is CanonicalDefinition.ErrorContextDebugMessage,
    CanonicalDefinition.ErrorContextDrop,
    -> errorContextValidator(context, definition)

    CanonicalDefinition.ThreadIndex,
    is CanonicalDefinition.ThreadNewIndirect,
    CanonicalDefinition.ThreadResumeLater,
    is CanonicalDefinition.ThreadSuspend,
    is CanonicalDefinition.ThreadSuspendThenResume,
    is CanonicalDefinition.ThreadYieldThenResume,
    is CanonicalDefinition.ThreadSuspendThenPromote,
    is CanonicalDefinition.ThreadYieldThenPromote,
    is CanonicalDefinition.ThreadSpawnRef,
    is CanonicalDefinition.ThreadSpawnIndirect,
    is CanonicalDefinition.ThreadAvailableParallelism,
    -> threadValidator(context, definition)
}
