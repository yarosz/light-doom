package io.github.charlietap.chasm.ast.component

import io.github.charlietap.chasm.ast.name.NameMap
import io.github.charlietap.chasm.ast.value.NameValue

sealed interface Custom

data class Uninterpreted(val name: NameValue, val data: UByteArray) : Custom {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (other !is Uninterpreted) return false
        if (name != other.name) return false
        return true
    }

    override fun hashCode(): Int = name.hashCode()
}

sealed interface NameSubsection

data class ComponentNameSubsection(val name: NameValue) : NameSubsection

data class SortNameSubsection(
    val sort: NameSort,
    val nameMap: NameMap,
) : NameSubsection

sealed interface NameSort {

    data object CoreFunction : NameSort

    data object CoreTable : NameSort

    data object CoreMemory : NameSort

    data object CoreGlobal : NameSort

    data object CoreTag : NameSort

    data object CoreType : NameSort

    data object CoreModule : NameSort

    data object CoreInstance : NameSort

    data object Function : NameSort

    data object Value : NameSort

    data object Type : NameSort

    data object Component : NameSort

    data object Instance : NameSort
}

data class NameData(
    val subsections: List<NameSubsection>,
) : Custom
