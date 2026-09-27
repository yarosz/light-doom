package io.github.charlietap.chasm.decoder.context

import io.github.charlietap.chasm.decoder.section.SectionSize
import io.github.charlietap.chasm.decoder.section.SectionType

internal interface SectionContext {
    var sectionSize: SectionSize
    var sectionType: SectionType
}
