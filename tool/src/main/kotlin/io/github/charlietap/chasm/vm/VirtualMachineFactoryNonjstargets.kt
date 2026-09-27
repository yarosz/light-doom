package io.github.charlietap.chasm.vm

import io.github.charlietap.chasm.embedding.shapes.Function
import io.github.charlietap.chasm.embedding.shapes.Global
import io.github.charlietap.chasm.embedding.shapes.Instance
import io.github.charlietap.chasm.embedding.shapes.Memory
import io.github.charlietap.chasm.embedding.shapes.Module
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.Table

typealias StoreReference = Store
typealias ModuleReference = Module
typealias InstanceReference = Instance
typealias FunctionReference = Function
typealias GlobalReference = Global
typealias MemoryReference = Memory
typealias TableReference = Table

fun virtualMachineFactory(): WasmVirtualMachine {
    return NonJsVirtualMachine
}
