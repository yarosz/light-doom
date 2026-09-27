package io.github.charlietap.chasm.embedding.shapes

import io.github.charlietap.chasm.config.ComponentConfig
import io.github.charlietap.chasm.type.component.ComponentTypes
import io.github.charlietap.chasm.ast.component.Component as InternalComponent

class Component internal constructor(
    internal val config: ComponentConfig,
    internal val component: InternalComponent,
    internal val types: ComponentTypes? = null,
)
