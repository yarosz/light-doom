package com.tap.mood.doom.runtime.generated

import io.github.charlietap.chasm.vm.Import
import io.github.charlietap.chasm.vm.Instance
import io.github.charlietap.chasm.vm.Memory
import io.github.charlietap.chasm.vm.Module
import io.github.charlietap.chasm.vm.NumberType
import io.github.charlietap.chasm.vm.PreparedFunction
import io.github.charlietap.chasm.vm.Store
import io.github.charlietap.chasm.vm.SuspendInstanceFactory
import io.github.charlietap.chasm.vm.SuspendModuleFactory
import io.github.charlietap.chasm.vm.SuspendingWasmVirtualMachine
import io.github.charlietap.chasm.vm.ValueType
import io.github.charlietap.chasm.vm.WasmVirtualMachine
import io.github.charlietap.chasm.vm.`expect`
import io.github.charlietap.chasm.vm.codegen.CodegenImport
import io.github.charlietap.chasm.vm.expectFirstInt
import io.github.charlietap.chasm.vm.importFactory
import io.github.charlietap.chasm.vm.suspendingVirtualMachineFactory
import kotlin.ByteArray
import kotlin.Int
import kotlin.collections.List
import kotlin.collections.MutableList

public suspend fun doomWasmModule(
  binary: ByteArray,
  imports: List<CodegenImport> = emptyList(),
  virtualMachine: SuspendingWasmVirtualMachine = suspendingVirtualMachineFactory(),
  moduleFactory: SuspendModuleFactory? = null,
  instanceFactory: SuspendInstanceFactory? = null,
): DoomWasmModule {
  val store: Store = virtualMachine.storeInit()

  val module: Module = moduleFactory?.invoke(binary) ?: virtualMachine.moduleDecodeSuspending(binary).`expect`("Failed to decode binary")

  val allocatedImports: List<Import> = virtualMachine.importFactory(store, imports)

  val instance: Instance = instanceFactory?.invoke(store, module, allocatedImports) ?: virtualMachine.moduleInstantiateSuspending(store, module, allocatedImports).`expect`("Failed to instantiate module")

  return DoomWasmModuleImpl(
    imports = allocatedImports,
    instance = instance,
    store = store,
    virtualMachine = virtualMachine,
  )
}

private class DoomWasmModuleImpl(
  private val imports: List<Import>,
  private val instance: Instance,
  private val store: Store,
  private val virtualMachine: WasmVirtualMachine,
) : DoomWasmModule {
  private val tickGamePreparedFunction: PreparedFunction =
      virtualMachine.prepareFunction(store, instance, "tickGame", tickGameResultTypes).`expect`("Failed to prepare function tickGame")

  private val setRenderEnabledPreparedFunction: PreparedFunction =
      virtualMachine.prepareFunction(store, instance, "setRenderEnabled", emptyList()).`expect`("Failed to prepare function setRenderEnabled")

  private val configureViewPreparedFunction: PreparedFunction =
      virtualMachine.prepareFunction(store, instance, "configureView", emptyList()).`expect`("Failed to prepare function configureView")

  private val setGammaLevelPreparedFunction: PreparedFunction =
      virtualMachine.prepareFunction(store, instance, "setGammaLevel", emptyList()).`expect`("Failed to prepare function setGammaLevel")

  private val getInputModePreparedFunction: PreparedFunction =
      virtualMachine.prepareFunction(store, instance, "getInputMode", getInputModeResultTypes).`expect`("Failed to prepare function getInputMode")

  private val reportKeyDownPreparedFunction: PreparedFunction =
      virtualMachine.prepareFunction(store, instance, "reportKeyDown", emptyList()).`expect`("Failed to prepare function reportKeyDown")

  private val reportKeyUpPreparedFunction: PreparedFunction =
      virtualMachine.prepareFunction(store, instance, "reportKeyUp", emptyList()).`expect`("Failed to prepare function reportKeyUp")

  private val initGamePreparedFunction: PreparedFunction =
      virtualMachine.prepareFunction(store, instance, "initGame", emptyList()).`expect`("Failed to prepare function initGame")

  private val functionInputBuffer: MutableList<WasmVirtualMachine.Value> =
      MutableList<WasmVirtualMachine.Value>(2) { WasmVirtualMachine.Value.I32(0) }

  private val functionInputBuffer1: MutableList<WasmVirtualMachine.Value> =
      functionInputBuffer.subList(0, 1)

  private val functionInputBuffer2: MutableList<WasmVirtualMachine.Value> = functionInputBuffer

  private val _memory: Memory =
      virtualMachine.exportMemory(instance, "memory").`expect`("Failed to find memory export with name memory")

  override val memory: DoomWasmModule.Memory = MemoryImpl(store, _memory, virtualMachine)

  override fun tickGame(): Int {
    val args = emptyList<WasmVirtualMachine.Value>()
    val result = tickGamePreparedFunction(args).expectFirstInt("Failed to invoke function tickGame")
    return result
  }

  override fun setRenderEnabled(p0: Int) {
    functionInputBuffer[0] = WasmVirtualMachine.Value.I32(p0)
    val args = functionInputBuffer1
    setRenderEnabledPreparedFunction(args).`expect`("Failed to invoke function setRenderEnabled")
  }

  override fun configureView(p0: Int, p1: Int) {
    functionInputBuffer[0] = WasmVirtualMachine.Value.I32(p0)
    functionInputBuffer[1] = WasmVirtualMachine.Value.I32(p1)
    val args = functionInputBuffer2
    configureViewPreparedFunction(args).`expect`("Failed to invoke function configureView")
  }

  override fun setGammaLevel(p0: Int) {
    functionInputBuffer[0] = WasmVirtualMachine.Value.I32(p0)
    val args = functionInputBuffer1
    setGammaLevelPreparedFunction(args).`expect`("Failed to invoke function setGammaLevel")
  }

  override fun getInputMode(): Int {
    val args = emptyList<WasmVirtualMachine.Value>()
    val result = getInputModePreparedFunction(args).expectFirstInt("Failed to invoke function getInputMode")
    return result
  }

  override fun reportKeyDown(p0: Int) {
    functionInputBuffer[0] = WasmVirtualMachine.Value.I32(p0)
    val args = functionInputBuffer1
    reportKeyDownPreparedFunction(args).`expect`("Failed to invoke function reportKeyDown")
  }

  override fun reportKeyUp(p0: Int) {
    functionInputBuffer[0] = WasmVirtualMachine.Value.I32(p0)
    val args = functionInputBuffer1
    reportKeyUpPreparedFunction(args).`expect`("Failed to invoke function reportKeyUp")
  }

  override fun initGame() {
    val args = emptyList<WasmVirtualMachine.Value>()
    initGamePreparedFunction(args).`expect`("Failed to invoke function initGame")
  }

  private class MemoryImpl(
    private val store: Store,
    private val memory: Memory,
    private val virtualMachine: WasmVirtualMachine,
  ) : DoomWasmModule.Memory {
    override fun read(
      buffer: ByteArray,
      memoryPointer: Int,
      bufferPointer: Int,
      bytesToRead: Int,
    ): ByteArray = virtualMachine.memoryReadBytes(store, memory, memoryPointer, bytesToRead, buffer, bufferPointer).`expect`("Failed to read memory")

    override fun write(
      pointer: Int,
      buffer: ByteArray,
      bufferPointer: Int,
      bytesToWrite: Int,
    ) {
      virtualMachine.memoryWriteBytes(store, memory, pointer, buffer, bufferPointer, bytesToWrite).`expect`("Failed to write memory")
    }
  }

  private companion object {
    private val tickGameResultTypes: List<ValueType> = listOf(
          ValueType.Number(NumberType.I32),
        )

    private val getInputModeResultTypes: List<ValueType> = listOf(
          ValueType.Number(NumberType.I32),
        )
  }
}
