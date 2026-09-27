package io.github.charlietap.chasm.ast.component

import io.github.charlietap.chasm.ast.value.NameValue

data class NameAttributes(
    val name: NameValue,
    val attributes: List<ExternalAttribute> = emptyList(),
)

sealed interface ExternalAttribute {

    data class Implements(val interfaceName: String) : ExternalAttribute

    data class VersionSuffix(val suffix: String) : ExternalAttribute

    data class ExternalId(val id: NameValue) : ExternalAttribute
}
