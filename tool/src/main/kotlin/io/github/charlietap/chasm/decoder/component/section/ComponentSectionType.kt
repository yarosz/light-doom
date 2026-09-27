package io.github.charlietap.chasm.decoder.component.section

internal enum class ComponentSectionType(val id: UByte) {
    Custom(0x00u),
    CoreModule(0x01u),
    CoreInstance(0x02u),
    CoreType(0x03u),
    Component(0x04u),
    Instance(0x05u),
    Alias(0x06u),
    Type(0x07u),
    Canonical(0x08u),
    Start(0x09u),
    Import(0x0Au),
    Export(0x0Bu),
    Value(0x0Cu),
}
