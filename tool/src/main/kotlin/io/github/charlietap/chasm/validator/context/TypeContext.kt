package io.github.charlietap.chasm.validator.context

internal interface TypeContext {
    var limitsMaximum: ULong
}

internal class TypeContextImpl(
    override var limitsMaximum: ULong = ULong.MAX_VALUE,
) : TypeContext
