package io.github.charlietap.chasm.decoder.decoder.component.alias

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.AliasDefinition
import io.github.charlietap.chasm.ast.component.CoreInstanceExportAliasTarget
import io.github.charlietap.chasm.ast.component.Index.ComponentIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentInstanceIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentModuleIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentModuleInstanceIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.ast.component.InstanceExportAliasTarget
import io.github.charlietap.chasm.ast.component.OuterAliasTarget
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.ReaderDecoder
import io.github.charlietap.chasm.decoder.decoder.component.sort.ComponentSort
import io.github.charlietap.chasm.decoder.decoder.component.sort.ComponentSortDecoder
import io.github.charlietap.chasm.decoder.decoder.component.sort.CoreSort
import io.github.charlietap.chasm.decoder.decoder.name.NameValueDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError
import io.github.charlietap.chasm.ast.module.Index.TypeIndex as ModuleTypeIndex

internal fun AliasDefinitionDecoder(
    context: ComponentDecoderContext,
): Result<AliasDefinition, WasmDecodeError> = AliasDefinitionDecoder(
    context = context,
    sortDecoder = ::ComponentSortDecoder,
    nameDecoder = ::NameValueDecoder,
)

internal inline fun AliasDefinitionDecoder(
    context: ComponentDecoderContext,
    crossinline sortDecoder: ComponentDecoder<ComponentSort>,
    crossinline nameDecoder: ReaderDecoder<NameValue>,
): Result<AliasDefinition, WasmDecodeError> = binding {
    val sort = sortDecoder(context).bind()
    when (val opcode = context.reader.ubyte()) {
        ALIAS_INSTANCE_EXPORT -> {
            val instance = ComponentInstanceIndex(context.reader.uint())
            val name = nameDecoder(context).bind()
            AliasDefinition.InstanceExport(instanceExportTarget(sort, instance, name).bind())
        }
        ALIAS_CORE_INSTANCE_EXPORT -> {
            val instance = ComponentModuleInstanceIndex(context.reader.uint())
            val name = nameDecoder(context).bind()
            AliasDefinition.CoreInstanceExport(coreInstanceExportTarget(sort, instance, name).bind())
        }
        ALIAS_OUTER -> {
            val count = context.reader.uint()
            val index = context.reader.uint()
            AliasDefinition.Outer(outerAliasTarget(sort, count, index).bind())
        }
        else -> Err(ComponentDecodeError.UnknownAlias(opcode)).bind<AliasDefinition>()
    }
}

private fun instanceExportTarget(
    sort: ComponentSort,
    instance: ComponentInstanceIndex,
    name: NameValue,
): Result<InstanceExportAliasTarget, WasmDecodeError> = when (sort) {
    is ComponentSort.Core -> when (sort.sort) {
        CoreSort.Module -> Ok(InstanceExportAliasTarget.Module(instance, name))
        else -> Err(ComponentDecodeError.InvalidInstanceAliasSort(sort.sort.opcode))
    }
    ComponentSort.Function -> Ok(InstanceExportAliasTarget.Function(instance, name))
    ComponentSort.Value -> Ok(InstanceExportAliasTarget.Value(instance, name))
    ComponentSort.Type -> Ok(InstanceExportAliasTarget.Type(instance, name))
    ComponentSort.Component -> Ok(InstanceExportAliasTarget.Component(instance, name))
    ComponentSort.Instance -> Ok(InstanceExportAliasTarget.Instance(instance, name))
}

private fun coreInstanceExportTarget(
    sort: ComponentSort,
    instance: ComponentModuleInstanceIndex,
    name: NameValue,
): Result<CoreInstanceExportAliasTarget, WasmDecodeError> = when (sort) {
    is ComponentSort.Core -> Ok(
        when (sort.sort) {
            CoreSort.Function -> CoreInstanceExportAliasTarget.Function(instance, name)
            CoreSort.Table -> CoreInstanceExportAliasTarget.Table(instance, name)
            CoreSort.Memory -> CoreInstanceExportAliasTarget.Memory(instance, name)
            CoreSort.Global -> CoreInstanceExportAliasTarget.Global(instance, name)
            CoreSort.Tag -> CoreInstanceExportAliasTarget.Tag(instance, name)
            CoreSort.Type -> CoreInstanceExportAliasTarget.Type(instance, name)
            CoreSort.Module -> CoreInstanceExportAliasTarget.Module(instance, name)
            CoreSort.Instance -> CoreInstanceExportAliasTarget.Instance(instance, name)
        },
    )
    else -> Err(ComponentDecodeError.InvalidCoreInstanceAliasSort(sort.opcode))
}

private fun outerAliasTarget(
    sort: ComponentSort,
    count: UInt,
    index: UInt,
): Result<OuterAliasTarget, WasmDecodeError> = when (sort) {
    is ComponentSort.Core -> when (sort.sort) {
        CoreSort.Module -> Ok(OuterAliasTarget.Module(count, ComponentModuleIndex(index)))
        CoreSort.Type -> Ok(OuterAliasTarget.CoreType(count, ModuleTypeIndex(index)))
        else -> Err(ComponentDecodeError.InvalidOuterAliasSort(sort.sort.opcode))
    }
    ComponentSort.Type -> Ok(OuterAliasTarget.Type(count, ComponentTypeIndex(index)))
    ComponentSort.Component -> Ok(OuterAliasTarget.Component(count, ComponentIndex(index)))
    else -> Err(ComponentDecodeError.InvalidOuterAliasSort(sort.opcode))
}

private const val ALIAS_INSTANCE_EXPORT: UByte = 0x00u
private const val ALIAS_CORE_INSTANCE_EXPORT: UByte = 0x01u
private const val ALIAS_OUTER: UByte = 0x02u
