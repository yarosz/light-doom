package io.github.charlietap.chasm.decoder.decoder.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalOption
import io.github.charlietap.chasm.ast.component.ComponentStringEncoding
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.section.index.FunctionIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.section.index.MemoryIndexDecoder
import io.github.charlietap.chasm.decoder.error.ComponentCanonicalDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.ast.module.Index.FunctionIndex as ModuleFunctionIndex
import io.github.charlietap.chasm.ast.module.Index.MemoryIndex as ModuleMemoryIndex

internal fun CanonicalOptionDecoder(
    context: ComponentDecoderContext,
): Result<CanonicalOption, WasmDecodeError> = CanonicalOptionDecoder(
    context = context,
    memoryIndexDecoder = { componentContext ->
        MemoryIndexDecoder(componentContext.moduleContext)
    },
    functionIndexDecoder = { componentContext ->
        FunctionIndexDecoder(componentContext.moduleContext)
    },
)

internal inline fun CanonicalOptionDecoder(
    context: ComponentDecoderContext,
    crossinline memoryIndexDecoder: ComponentDecoder<ModuleMemoryIndex>,
    crossinline functionIndexDecoder: ComponentDecoder<ModuleFunctionIndex>,
): Result<CanonicalOption, WasmDecodeError> = binding {
    when (val opcode = context.reader.ubyte()) {
        OPTION_UTF8 -> CanonicalOption.StringEncoding(ComponentStringEncoding.Utf8)
        OPTION_UTF16 -> CanonicalOption.StringEncoding(ComponentStringEncoding.Utf16)
        OPTION_LATIN1_UTF16 -> CanonicalOption.StringEncoding(ComponentStringEncoding.Latin1Utf16)
        OPTION_MEMORY -> CanonicalOption.Memory(memoryIndexDecoder(context).bind())
        OPTION_REALLOC -> CanonicalOption.Realloc(functionIndexDecoder(context).bind())
        OPTION_POST_RETURN -> CanonicalOption.PostReturn(functionIndexDecoder(context).bind())
        OPTION_ASYNC -> CanonicalOption.Async
        OPTION_CALLBACK -> CanonicalOption.Callback(functionIndexDecoder(context).bind())
        else -> Err(ComponentCanonicalDecodeError.UnknownOption(opcode)).bind<CanonicalOption>()
    }
}

private const val OPTION_UTF8: UByte = 0x00u
private const val OPTION_UTF16: UByte = 0x01u
private const val OPTION_LATIN1_UTF16: UByte = 0x02u
private const val OPTION_MEMORY: UByte = 0x03u
private const val OPTION_REALLOC: UByte = 0x04u
private const val OPTION_POST_RETURN: UByte = 0x05u
private const val OPTION_ASYNC: UByte = 0x06u
private const val OPTION_CALLBACK: UByte = 0x07u
