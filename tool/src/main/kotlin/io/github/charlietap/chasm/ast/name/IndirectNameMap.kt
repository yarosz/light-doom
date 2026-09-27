package io.github.charlietap.chasm.ast.name

data class IndirectNameAssociation(val idx: UInt, val nameMap: NameMap)

typealias IndirectNameMap = List<IndirectNameAssociation>
