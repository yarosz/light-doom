package io.github.charlietap.chasm.decoder.decoder.component.sort

internal enum class CoreSort(val opcode: UByte) {
    Function(0x00u),
    Table(0x01u),
    Memory(0x02u),
    Global(0x03u),
    Tag(0x04u),
    Type(0x10u),
    Module(0x11u),
    Instance(0x12u),
}

internal sealed interface ComponentSort {
    val opcode: UByte

    data class Core(val sort: CoreSort) : ComponentSort {
        override val opcode: UByte = CORE_SORT_PREFIX
    }

    data object Function : ComponentSort {
        override val opcode: UByte = 0x01u
    }

    data object Value : ComponentSort {
        override val opcode: UByte = 0x02u
    }

    data object Type : ComponentSort {
        override val opcode: UByte = 0x03u
    }

    data object Component : ComponentSort {
        override val opcode: UByte = 0x04u
    }

    data object Instance : ComponentSort {
        override val opcode: UByte = 0x05u
    }
}

private const val CORE_SORT_PREFIX: UByte = 0x00u
