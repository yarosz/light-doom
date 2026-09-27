package io.github.charlietap.chasm.decoder.decoder.component.sort

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.ExportTarget
import io.github.charlietap.chasm.ast.component.Index.ComponentFunctionIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentInstanceIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentModuleIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentValueIndex
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.ComponentDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ExportTargetDecoder(
    context: ComponentDecoderContext,
): Result<ExportTarget, WasmDecodeError> = ExportTargetDecoder(
    context = context,
    sortDecoder = ::ComponentSortDecoder,
)

internal inline fun ExportTargetDecoder(
    context: ComponentDecoderContext,
    crossinline sortDecoder: ComponentDecoder<ComponentSort>,
): Result<ExportTarget, WasmDecodeError> = binding {
    val sort = sortDecoder(context).bind()
    val index = context.reader.uint()
    when (sort) {
        is ComponentSort.Core -> when (sort.sort) {
            CoreSort.Module -> ExportTarget.Module(ComponentModuleIndex(index))
            else -> Err(ComponentDecodeError.InvalidSortTarget(sort.sort.opcode)).bind()
        }
        ComponentSort.Function -> ExportTarget.Function(ComponentFunctionIndex(index))
        ComponentSort.Value -> ExportTarget.Value(ComponentValueIndex(index))
        ComponentSort.Type -> ExportTarget.Type(ComponentTypeIndex(index))
        ComponentSort.Component -> ExportTarget.Component(ComponentIndex(index))
        ComponentSort.Instance -> ExportTarget.Instance(ComponentInstanceIndex(index))
    }
}
