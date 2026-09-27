package io.github.charlietap.chasm.decoder.decoder.component.sort

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CoreSortDecoder(
    context: ComponentDecoderContext,
): Result<CoreSort, WasmDecodeError> = binding {
    val opcode = context.reader.ubyte()
    CoreSort.entries.firstOrNull { sort -> sort.opcode == opcode }
        ?: Err(ComponentDecodeError.UnknownCoreSort(opcode)).bind()
}
