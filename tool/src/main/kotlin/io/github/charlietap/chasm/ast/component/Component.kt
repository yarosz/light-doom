package io.github.charlietap.chasm.ast.component

import io.github.charlietap.chasm.ast.module.Module

data class Component(
    val version: Version,
    val definitions: List<Definition>,
    val customs: List<Custom>,
)

sealed interface Definition

data class CoreModule(val module: Module) : Definition

data class CoreInstance(val instance: CoreInstanceDefinition) : Definition

data class CoreType(val type: CoreTypeDefinition) : Definition

data class NestedComponent(val component: Component) : Definition

data class Instance(val instance: InstanceDefinition) : Definition

data class Alias(val alias: AliasDefinition) : Definition

data class Type(val type: TypeDefinition) : Definition

data class Canon(val canon: CanonicalDefinition) : Definition

data class Start(val start: StartDefinition) : Definition

data class Import(
    val name: NameAttributes,
    val type: ExternalType,
) : Definition

data class Export(
    val name: NameAttributes,
    val target: ExportTarget,
    val type: ExternalType?,
) : Definition

data class Value(val value: ComponentValue) : Definition
