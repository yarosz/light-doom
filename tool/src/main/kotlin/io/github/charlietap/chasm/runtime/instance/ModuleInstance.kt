package io.github.charlietap.chasm.runtime.instance

import io.github.charlietap.chasm.host.HostModuleInstance
import io.github.charlietap.chasm.runtime.address.Address
import io.github.charlietap.chasm.runtime.type.RuntimeTypeMap

data class ModuleInstance(
    val runtimeTypes: RuntimeTypeMap,
    val functionAddresses: MutableList<Address.Function> = mutableListOf(),
    val tableAddresses: MutableList<Address.Table> = mutableListOf(),
    val memAddresses: MutableList<Address.Memory> = mutableListOf(),
    val tagAddresses: MutableList<Address.Tag> = mutableListOf(),
    val globalAddresses: MutableList<Address.Global> = mutableListOf(),
    val elemAddresses: MutableList<Address.Element> = mutableListOf(),
    val dataAddresses: MutableList<Address.Data> = mutableListOf(),
    val exports: MutableList<ExportInstance> = mutableListOf(),
    var deallocated: Boolean = false,
) : HostModuleInstance
