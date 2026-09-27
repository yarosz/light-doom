package io.github.charlietap.chasm.ast.name

import io.github.charlietap.chasm.ast.value.NameValue

data class NameAssociation(val idx: UInt, val name: NameValue)

typealias NameMap = List<NameAssociation>
