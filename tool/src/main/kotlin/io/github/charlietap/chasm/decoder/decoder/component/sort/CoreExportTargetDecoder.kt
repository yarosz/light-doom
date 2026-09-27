package io.github.charlietap.chasm.decoder.decoder.component.sort

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CoreExportTarget
import io.github.charlietap.chasm.ast.component.Index.ComponentModuleIndex
import io.github.charlietap.chasm.ast.component.Index.ComponentModuleInstanceIndex
import io.github.charlietap.chasm.ast.module.Index.FunctionIndex
import io.github.charlietap.chasm.ast.module.Index.GlobalIndex
import io.github.charlietap.chasm.ast.module.Index.MemoryIndex
import io.github.charlietap.chasm.ast.module.Index.TableIndex
import io.github.charlietap.chasm.ast.module.Index.TagIndex
import io.github.charlietap.chasm.ast.module.Index.TypeIndex
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun CoreExportTargetDecoder(
    context: ComponentDecoderContext,
): Result<CoreExportTarget, WasmDecodeError> = CoreExportTargetDecoder(
    context = context,
    sortDecoder = ::CoreSortDecoder,
)

internal inline fun CoreExportTargetDecoder(
    context: ComponentDecoderContext,
    crossinline sortDecoder: ComponentDecoder<CoreSort>,
): Result<CoreExportTarget, WasmDecodeError> = binding {
    val sort = sortDecoder(context).bind()
    val index = context.reader.uint()
    when (sort) {
        CoreSort.Function -> CoreExportTarget.Function(FunctionIndex(index))
        CoreSort.Table -> CoreExportTarget.Table(TableIndex(index))
        CoreSort.Memory -> CoreExportTarget.Memory(MemoryIndex(index))
        CoreSort.Global -> CoreExportTarget.Global(GlobalIndex(index))
        CoreSort.Tag -> CoreExportTarget.Tag(TagIndex(index))
        CoreSort.Type -> CoreExportTarget.Type(TypeIndex(index))
        CoreSort.Module -> CoreExportTarget.Module(ComponentModuleIndex(index))
        CoreSort.Instance -> CoreExportTarget.Instance(ComponentModuleInstanceIndex(index))
    }
}
