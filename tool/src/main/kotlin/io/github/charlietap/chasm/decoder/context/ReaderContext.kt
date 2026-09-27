package io.github.charlietap.chasm.decoder.context

import io.github.charlietap.chasm.decoder.reader.WasmBinaryReader

internal interface ReaderContext {
    var reader: WasmBinaryReader
}
