package io.github.charlietap.chasm.decoder.error

internal class WasmDecodeException(
    val error: WasmDecodeError,
) : RuntimeException(error.toString())
