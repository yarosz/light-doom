package io.github.charlietap.chasm.config

data class ComponentConfig(
    val moduleConfig: ModuleConfig = ModuleConfig(),
    val features: Set<ComponentFeature> = ComponentFeature.entries.toSet(),
    val decodeNameSection: Boolean = false,
)
