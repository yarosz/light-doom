package io.github.charlietap.chasm.validator.context

import io.github.charlietap.chasm.type.ReferenceType

internal interface ElementSegmentContext {
    var elementSegmentType: ReferenceType?
}

internal class ElementSegmentContextImpl(
    override var elementSegmentType: ReferenceType? = null,
) : ElementSegmentContext
