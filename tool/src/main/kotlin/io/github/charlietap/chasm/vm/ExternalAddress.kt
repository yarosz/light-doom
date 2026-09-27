package io.github.charlietap.chasm.vm











sealed interface ExternalAddress {
    class Function internal constructor(internal val address: FunctionExternalAddress) : ExternalAddress

    class Global internal constructor(internal val address: GlobalExternalAddress) : ExternalAddress

    class Memory internal constructor(internal val address: MemoryExternalAddress) : ExternalAddress

    class Table internal constructor(internal val address: TableExternalAddress) : ExternalAddress

    class Tag internal constructor(internal val address: TagExternalAddress) : ExternalAddress
}
