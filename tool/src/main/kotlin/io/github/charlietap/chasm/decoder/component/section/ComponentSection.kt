package io.github.charlietap.chasm.decoder.component.section

import io.github.charlietap.chasm.ast.component.Definition
import io.github.charlietap.chasm.ast.component.Custom as AstCustom

internal sealed interface ComponentSection {

    data class Definitions(val definitions: List<Definition>) : ComponentSection

    data class Custom(val custom: AstCustom) : ComponentSection
}
