package io.github.charlietap.chasm.decoder.reader

internal fun interface ByteSource {

    fun readAtMostTo(
        destination: ByteArray,
        startIndex: Int,
        endIndex: Int,
    ): Int
}
