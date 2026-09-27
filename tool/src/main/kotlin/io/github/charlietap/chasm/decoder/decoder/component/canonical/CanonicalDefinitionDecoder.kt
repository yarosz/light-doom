package io.github.charlietap.chasm.decoder.decoder.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalDefinition
import io.github.charlietap.chasm.ast.component.CanonicalOption
import io.github.charlietap.chasm.ast.component.Index.ComponentFunctionIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentFunctionIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentTypeIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.component.type.ComponentResultListDecoder
import io.github.charlietap.chasm.decoder.decoder.section.index.FunctionIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.section.index.MemoryIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.section.index.TableIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.type.value.ValueTypeDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.ComponentCanonicalDecodeError
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.ast.module.Index.FunctionIndex as ModuleFunctionIndex
import io.github.charlietap.chasm.ast.module.Index.MemoryIndex as ModuleMemoryIndex
import io.github.charlietap.chasm.ast.module.Index.TableIndex as ModuleTableIndex
import io.github.charlietap.chasm.type.ValueType as CoreValueType

internal fun CanonicalDefinitionDecoder(
    context: ComponentDecoderContext,
): Result<CanonicalDefinition, WasmDecodeError> = CanonicalDefinitionDecoder(
    context = context,
    moduleFunctionIndexDecoder = { componentContext ->
        FunctionIndexDecoder(componentContext.moduleContext)
    },
    componentFunctionIndexDecoder = ::ComponentFunctionIndexDecoder,
    componentTypeIndexDecoder = ::ComponentTypeIndexDecoder,
    moduleMemoryIndexDecoder = { componentContext ->
        MemoryIndexDecoder(componentContext.moduleContext)
    },
    moduleTableIndexDecoder = { componentContext ->
        TableIndexDecoder(componentContext.moduleContext)
    },
    canonicalOptionDecoder = ::CanonicalOptionDecoder,
    optionVectorDecoder = ::ReaderVectorDecoder,
    coreValueTypeDecoder = { componentContext ->
        ValueTypeDecoder(componentContext.moduleContext)
    },
    resultListDecoder = ::ComponentResultListDecoder,
    asyncFlagDecoder = ::AsyncFlagDecoder,
    cancellableFlagDecoder = ::CancellableFlagDecoder,
    sharedFlagDecoder = ::SharedFlagDecoder,
)

@Suppress("LongMethod")
internal inline fun CanonicalDefinitionDecoder(
    context: ComponentDecoderContext,
    crossinline moduleFunctionIndexDecoder: ComponentDecoder<ModuleFunctionIndex>,
    crossinline componentFunctionIndexDecoder: ComponentDecoder<ComponentFunctionIndex>,
    crossinline componentTypeIndexDecoder: ComponentDecoder<ComponentTypeIndex>,
    crossinline moduleMemoryIndexDecoder: ComponentDecoder<ModuleMemoryIndex>,
    crossinline moduleTableIndexDecoder: ComponentDecoder<ModuleTableIndex>,
    noinline canonicalOptionDecoder: ComponentDecoder<CanonicalOption>,
    crossinline optionVectorDecoder: ComponentVectorDecoder<CanonicalOption>,
    crossinline coreValueTypeDecoder: ComponentDecoder<CoreValueType>,
    crossinline resultListDecoder: ComponentDecoder<ValueType?>,
    crossinline asyncFlagDecoder: ComponentDecoder<Boolean>,
    crossinline cancellableFlagDecoder: ComponentDecoder<Boolean>,
    crossinline sharedFlagDecoder: ComponentDecoder<Boolean>,
): Result<CanonicalDefinition, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        CANON_LIFT -> {
            val marker = context.reader.ubyte()
            if (marker != FUNCTION_SORT_MARKER) {
                Err(ComponentDecodeError.InvalidMarker(FUNCTION_SORT_MARKER, marker)).bind<Unit>()
            }
            CanonicalDefinition.Lift(
                functionIndex = moduleFunctionIndexDecoder(context).bind(),
                options = optionVectorDecoder(context, canonicalOptionDecoder).bind().vector,
                typeIndex = componentTypeIndexDecoder(context).bind(),
            )
        }
        CANON_LOWER -> {
            val marker = context.reader.ubyte()
            if (marker != FUNCTION_SORT_MARKER) {
                Err(ComponentDecodeError.InvalidMarker(FUNCTION_SORT_MARKER, marker)).bind<Unit>()
            }
            CanonicalDefinition.Lower(
                functionIndex = componentFunctionIndexDecoder(context).bind(),
                options = optionVectorDecoder(context, canonicalOptionDecoder).bind().vector,
            )
        }
        CANON_RESOURCE_NEW -> CanonicalDefinition.ResourceNew(componentTypeIndexDecoder(context).bind())
        CANON_RESOURCE_DROP -> CanonicalDefinition.ResourceDrop(componentTypeIndexDecoder(context).bind())
        CANON_RESOURCE_REP -> CanonicalDefinition.ResourceRep(componentTypeIndexDecoder(context).bind())
        CANON_TASK_CANCEL -> CanonicalDefinition.TaskCancel
        CANON_SUBTASK_CANCEL -> CanonicalDefinition.SubtaskCancel(asyncFlagDecoder(context).bind())
        CANON_BACKPRESSURE_SET -> CanonicalDefinition.BackpressureSet
        CANON_TASK_RETURN -> CanonicalDefinition.TaskReturn(
            result = resultListDecoder(context).bind(),
            options = optionVectorDecoder(context, canonicalOptionDecoder).bind().vector,
        )
        CANON_CONTEXT_GET -> CanonicalDefinition.ContextGet(
            type = coreValueTypeDecoder(context).bind(),
            index = context.reader.uint(),
        )
        CANON_CONTEXT_SET -> CanonicalDefinition.ContextSet(
            type = coreValueTypeDecoder(context).bind(),
            index = context.reader.uint(),
        )
        CANON_THREAD_YIELD -> CanonicalDefinition.ThreadYield(cancellableFlagDecoder(context).bind())
        CANON_SUBTASK_DROP -> CanonicalDefinition.SubtaskDrop
        CANON_STREAM_NEW -> CanonicalDefinition.StreamNew(componentTypeIndexDecoder(context).bind())
        CANON_STREAM_READ -> CanonicalDefinition.StreamRead(
            typeIndex = componentTypeIndexDecoder(context).bind(),
            options = optionVectorDecoder(context, canonicalOptionDecoder).bind().vector,
        )
        CANON_STREAM_WRITE -> CanonicalDefinition.StreamWrite(
            typeIndex = componentTypeIndexDecoder(context).bind(),
            options = optionVectorDecoder(context, canonicalOptionDecoder).bind().vector,
        )
        CANON_STREAM_CANCEL_READ -> CanonicalDefinition.StreamCancelRead(
            typeIndex = componentTypeIndexDecoder(context).bind(),
            async = asyncFlagDecoder(context).bind(),
        )
        CANON_STREAM_CANCEL_WRITE -> CanonicalDefinition.StreamCancelWrite(
            typeIndex = componentTypeIndexDecoder(context).bind(),
            async = asyncFlagDecoder(context).bind(),
        )
        CANON_STREAM_DROP_READABLE -> CanonicalDefinition.StreamDropReadable(componentTypeIndexDecoder(context).bind())
        CANON_STREAM_DROP_WRITABLE -> CanonicalDefinition.StreamDropWritable(componentTypeIndexDecoder(context).bind())
        CANON_FUTURE_NEW -> CanonicalDefinition.FutureNew(componentTypeIndexDecoder(context).bind())
        CANON_FUTURE_READ -> CanonicalDefinition.FutureRead(
            typeIndex = componentTypeIndexDecoder(context).bind(),
            options = optionVectorDecoder(context, canonicalOptionDecoder).bind().vector,
        )
        CANON_FUTURE_WRITE -> CanonicalDefinition.FutureWrite(
            typeIndex = componentTypeIndexDecoder(context).bind(),
            options = optionVectorDecoder(context, canonicalOptionDecoder).bind().vector,
        )
        CANON_FUTURE_CANCEL_READ -> CanonicalDefinition.FutureCancelRead(
            typeIndex = componentTypeIndexDecoder(context).bind(),
            async = asyncFlagDecoder(context).bind(),
        )
        CANON_FUTURE_CANCEL_WRITE -> CanonicalDefinition.FutureCancelWrite(
            typeIndex = componentTypeIndexDecoder(context).bind(),
            async = asyncFlagDecoder(context).bind(),
        )
        CANON_FUTURE_DROP_READABLE -> CanonicalDefinition.FutureDropReadable(componentTypeIndexDecoder(context).bind())
        CANON_FUTURE_DROP_WRITABLE -> CanonicalDefinition.FutureDropWritable(componentTypeIndexDecoder(context).bind())
        CANON_ERROR_CONTEXT_NEW -> CanonicalDefinition.ErrorContextNew(
            optionVectorDecoder(context, canonicalOptionDecoder).bind().vector,
        )
        CANON_ERROR_CONTEXT_DEBUG_MESSAGE -> CanonicalDefinition.ErrorContextDebugMessage(
            optionVectorDecoder(context, canonicalOptionDecoder).bind().vector,
        )
        CANON_ERROR_CONTEXT_DROP -> CanonicalDefinition.ErrorContextDrop
        CANON_WAITABLE_SET_NEW -> CanonicalDefinition.WaitableSetNew
        CANON_WAITABLE_SET_WAIT -> CanonicalDefinition.WaitableSetWait(
            cancellable = cancellableFlagDecoder(context).bind(),
            memoryIndex = moduleMemoryIndexDecoder(context).bind(),
        )
        CANON_WAITABLE_SET_POLL -> CanonicalDefinition.WaitableSetPoll(
            cancellable = cancellableFlagDecoder(context).bind(),
            memoryIndex = moduleMemoryIndexDecoder(context).bind(),
        )
        CANON_WAITABLE_SET_DROP -> CanonicalDefinition.WaitableSetDrop
        CANON_WAITABLE_JOIN -> CanonicalDefinition.WaitableJoin
        CANON_BACKPRESSURE_INC -> CanonicalDefinition.BackpressureInc
        CANON_BACKPRESSURE_DEC -> CanonicalDefinition.BackpressureDec
        CANON_THREAD_INDEX -> CanonicalDefinition.ThreadIndex
        CANON_THREAD_NEW_INDIRECT -> CanonicalDefinition.ThreadNewIndirect(
            typeIndex = componentTypeIndexDecoder(context).bind(),
            tableIndex = moduleTableIndexDecoder(context).bind(),
        )
        CANON_THREAD_RESUME_LATER -> CanonicalDefinition.ThreadResumeLater
        CANON_THREAD_SUSPEND -> CanonicalDefinition.ThreadSuspend(cancellableFlagDecoder(context).bind())
        CANON_THREAD_SUSPEND_THEN_RESUME -> CanonicalDefinition.ThreadSuspendThenResume(
            cancellableFlagDecoder(context).bind(),
        )
        CANON_THREAD_YIELD_THEN_RESUME -> CanonicalDefinition.ThreadYieldThenResume(
            cancellableFlagDecoder(context).bind(),
        )
        CANON_THREAD_SUSPEND_THEN_PROMOTE -> CanonicalDefinition.ThreadSuspendThenPromote(
            cancellableFlagDecoder(context).bind(),
        )
        CANON_THREAD_YIELD_THEN_PROMOTE -> CanonicalDefinition.ThreadYieldThenPromote(
            cancellableFlagDecoder(context).bind(),
        )
        CANON_THREAD_SPAWN_REF -> CanonicalDefinition.ThreadSpawnRef(
            shared = sharedFlagDecoder(context).bind(),
            typeIndex = componentTypeIndexDecoder(context).bind(),
        )
        CANON_THREAD_SPAWN_INDIRECT -> CanonicalDefinition.ThreadSpawnIndirect(
            shared = sharedFlagDecoder(context).bind(),
            typeIndex = componentTypeIndexDecoder(context).bind(),
            tableIndex = moduleTableIndexDecoder(context).bind(),
        )
        CANON_THREAD_AVAILABLE_PARALLELISM -> CanonicalDefinition.ThreadAvailableParallelism(
            sharedFlagDecoder(context).bind(),
        )
        else -> Err(ComponentCanonicalDecodeError.UnknownDefinition(opcode)).bind<CanonicalDefinition>()
    }
}

private const val FUNCTION_SORT_MARKER: UByte = 0x00u

private const val CANON_LIFT: UByte = 0x00u
private const val CANON_LOWER: UByte = 0x01u
private const val CANON_RESOURCE_NEW: UByte = 0x02u
private const val CANON_RESOURCE_DROP: UByte = 0x03u
private const val CANON_RESOURCE_REP: UByte = 0x04u
private const val CANON_TASK_CANCEL: UByte = 0x05u
private const val CANON_SUBTASK_CANCEL: UByte = 0x06u
private const val CANON_BACKPRESSURE_SET: UByte = 0x08u
private const val CANON_TASK_RETURN: UByte = 0x09u
private const val CANON_CONTEXT_GET: UByte = 0x0Au
private const val CANON_CONTEXT_SET: UByte = 0x0Bu
private const val CANON_THREAD_YIELD: UByte = 0x0Cu
private const val CANON_SUBTASK_DROP: UByte = 0x0Du
private const val CANON_STREAM_NEW: UByte = 0x0Eu
private const val CANON_STREAM_READ: UByte = 0x0Fu
private const val CANON_STREAM_WRITE: UByte = 0x10u
private const val CANON_STREAM_CANCEL_READ: UByte = 0x11u
private const val CANON_STREAM_CANCEL_WRITE: UByte = 0x12u
private const val CANON_STREAM_DROP_READABLE: UByte = 0x13u
private const val CANON_STREAM_DROP_WRITABLE: UByte = 0x14u
private const val CANON_FUTURE_NEW: UByte = 0x15u
private const val CANON_FUTURE_READ: UByte = 0x16u
private const val CANON_FUTURE_WRITE: UByte = 0x17u
private const val CANON_FUTURE_CANCEL_READ: UByte = 0x18u
private const val CANON_FUTURE_CANCEL_WRITE: UByte = 0x19u
private const val CANON_FUTURE_DROP_READABLE: UByte = 0x1Au
private const val CANON_FUTURE_DROP_WRITABLE: UByte = 0x1Bu
private const val CANON_ERROR_CONTEXT_NEW: UByte = 0x1Cu
private const val CANON_ERROR_CONTEXT_DEBUG_MESSAGE: UByte = 0x1Du
private const val CANON_ERROR_CONTEXT_DROP: UByte = 0x1Eu
private const val CANON_WAITABLE_SET_NEW: UByte = 0x1Fu
private const val CANON_WAITABLE_SET_WAIT: UByte = 0x20u
private const val CANON_WAITABLE_SET_POLL: UByte = 0x21u
private const val CANON_WAITABLE_SET_DROP: UByte = 0x22u
private const val CANON_WAITABLE_JOIN: UByte = 0x23u
private const val CANON_BACKPRESSURE_INC: UByte = 0x24u
private const val CANON_BACKPRESSURE_DEC: UByte = 0x25u
private const val CANON_THREAD_INDEX: UByte = 0x26u
private const val CANON_THREAD_NEW_INDIRECT: UByte = 0x27u
private const val CANON_THREAD_RESUME_LATER: UByte = 0x28u
private const val CANON_THREAD_SUSPEND: UByte = 0x29u
private const val CANON_THREAD_SUSPEND_THEN_RESUME: UByte = 0x2Au
private const val CANON_THREAD_YIELD_THEN_RESUME: UByte = 0x2Bu
private const val CANON_THREAD_SUSPEND_THEN_PROMOTE: UByte = 0x2Cu
private const val CANON_THREAD_YIELD_THEN_PROMOTE: UByte = 0x2Du
private const val CANON_THREAD_SPAWN_REF: UByte = 0x40u
private const val CANON_THREAD_SPAWN_INDIRECT: UByte = 0x41u
private const val CANON_THREAD_AVAILABLE_PARALLELISM: UByte = 0x42u
