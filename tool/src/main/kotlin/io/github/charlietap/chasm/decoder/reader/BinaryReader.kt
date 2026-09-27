package io.github.charlietap.chasm.decoder.reader

import io.github.charlietap.chasm.stream.SourceReader

internal fun BinaryReader(bytes: ByteArray): WasmBinaryReader =
    BufferedWasmBinaryReader(bytes)

internal fun BinaryReader(source: SourceReader): WasmBinaryReader =
    BufferedWasmBinaryReader(SourceReaderAdapter(source))
