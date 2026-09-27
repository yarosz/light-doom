package io.github.charlietap.chasm.decoder.reader

import io.github.charlietap.chasm.stream.SourceReader

internal class SourceReaderAdapter(
    private val reader: SourceReader,
) : ByteSource {

    override fun readAtMostTo(
        destination: ByteArray,
        startIndex: Int,
        endIndex: Int,
    ): Int {
        if (reader.exhausted()) return -1

        val requested = endIndex - startIndex
        val bytes = reader.bytes(requested)
        check(bytes.size <= requested) {
            "SourceReader returned ${bytes.size} bytes for a $requested byte request"
        }

        bytes.copyInto(destination, startIndex, 0, bytes.size)
        return bytes.size
    }
}
