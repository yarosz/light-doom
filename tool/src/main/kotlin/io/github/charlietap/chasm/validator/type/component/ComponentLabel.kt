package io.github.charlietap.chasm.validator.type.component

internal class ComponentLabel private constructor(
    val value: String,
    private val foldedHash: Int,
) {

    override fun equals(other: Any?): Boolean =
        other is ComponentLabel && value.equals(other.value, ignoreCase = true)

    override fun hashCode(): Int = foldedHash

    override fun toString(): String = value

    companion object {
        fun parse(value: String): ComponentLabel? {
            if (value.isEmpty()) return null

            var segmentStart = true
            var segment = 0
            var lower = false
            var upper = false
            var hash = 0
            value.forEach { character ->
                if (character == '-') {
                    if (segmentStart) return null
                    segmentStart = true
                    segment += 1
                    lower = false
                    upper = false
                    hash = 31 * hash + character.code
                    return@forEach
                }
                if (character !in '0'..'9' && character !in 'a'..'z' && character !in 'A'..'Z') return null
                if (segmentStart && segment == 0 && character in '0'..'9') return null

                when (character) {
                    in 'a'..'z' -> lower = true
                    in 'A'..'Z' -> upper = true
                }
                if (lower && upper) return null
                segmentStart = false
                hash = 31 * hash + character.asciiLowercase().code
            }
            if (segmentStart) return null
            return ComponentLabel(value, hash)
        }
    }
}

private fun Char.asciiLowercase(): Char = if (this in 'A'..'Z') this + ('a' - 'A') else this
