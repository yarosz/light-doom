package io.github.charlietap.chasm.memory.write

import io.github.charlietap.chasm.runtime.memory.LinearMemory

typealias BytesWriter = (LinearMemory, Int, ByteArray, Int, Int, Int) -> Unit


