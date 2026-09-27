package io.github.charlietap.chasm.validator.type.component

import io.github.charlietap.chasm.ast.component.ComponentStringEncoding
import io.github.charlietap.chasm.type.AddressType
import io.github.charlietap.chasm.type.DefinedType
import io.github.charlietap.chasm.type.MemoryType
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiContext
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiLowering
import io.github.charlietap.chasm.type.component.canonical.CanonicalAbiSignatureOptions
import io.github.charlietap.chasm.type.component.canonical.CanonicalCoreFunctionType
import io.github.charlietap.chasm.type.component.canonical.CanonicalFunctionType
import io.github.charlietap.chasm.type.component.canonical.CanonicalFunctionTypeLowering

internal data class CanonicalAbiOptions(
    val stringEncoding: ComponentStringEncoding = ComponentStringEncoding.Utf8,
    val addressType: AddressType = AddressType.I32,
    val memory: MemoryType? = null,
    val realloc: DefinedType? = null,
    val postReturn: DefinedType? = null,
    val callback: DefinedType? = null,
    val async: Boolean = false,
)

internal fun CanonicalFunctionType(
    type: ComponentFunctionType,
    options: CanonicalAbiOptions,
    context: CanonicalAbiContext,
): DefinedType? = CanonicalFunctionType(type, options.signatureOptions(), context)

internal fun CanonicalFunctionTypeLowering(
    type: ComponentFunctionType,
    options: CanonicalAbiOptions,
    context: CanonicalAbiContext,
): CanonicalAbiLowering? = CanonicalFunctionTypeLowering(type, options.signatureOptions(), context)

private fun CanonicalAbiOptions.signatureOptions(): CanonicalAbiSignatureOptions = CanonicalAbiSignatureOptions(
    addressType = addressType,
    async = async,
    hasCallback = callback != null,
)
