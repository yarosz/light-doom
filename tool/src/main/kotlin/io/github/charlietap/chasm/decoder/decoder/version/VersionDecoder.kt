package io.github.charlietap.chasm.decoder.decoder.version

import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.ast.module.Version
import io.github.charlietap.chasm.decoder.context.ModuleDecoderContext
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.decoder.ext.version

internal fun VersionDecoder(
    context: ModuleDecoderContext,
): Result<Version, WasmDecodeError> = context.reader.ubytes(4u).version()
