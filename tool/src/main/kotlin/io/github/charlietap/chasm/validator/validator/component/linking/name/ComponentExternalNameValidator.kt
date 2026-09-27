package io.github.charlietap.chasm.validator.validator.component.linking.name

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import io.github.charlietap.chasm.ast.component.ExternalAttribute
import io.github.charlietap.chasm.ast.component.NameAttributes
import io.github.charlietap.chasm.type.component.ComponentDefinedType
import io.github.charlietap.chasm.type.component.ComponentDefinedValueType
import io.github.charlietap.chasm.type.component.ComponentEntityType
import io.github.charlietap.chasm.type.component.ComponentResourceTypeId
import io.github.charlietap.chasm.type.component.ComponentValueType
import io.github.charlietap.chasm.validator.context.component.ComponentNameContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.ComponentLabel

internal fun ComponentImportNameValidator(
    name: NameAttributes,
    type: ComponentEntityType,
    names: ComponentNameContext,
): Result<Unit, ComponentValidatorError> = ComponentExternalNameValidator(
    name = name,
    type = type,
    names = names,
    import = true,
)

internal fun ComponentExportNameValidator(
    name: NameAttributes,
    type: ComponentEntityType,
    names: ComponentNameContext,
): Result<Unit, ComponentValidatorError> = ComponentExternalNameValidator(
    name = name,
    type = type,
    names = names,
    import = false,
)

private fun ComponentExternalNameValidator(
    name: NameAttributes,
    type: ComponentEntityType,
    names: ComponentNameContext,
    import: Boolean,
): Result<Unit, ComponentValidatorError> {
    val rawName = name.name.name
    val parsed = parseExternalName(rawName)
        ?: return Err(ComponentValidatorError.InvalidName(rawName))
    if (!import && parsed.isImportOnly()) {
        return Err(ComponentValidatorError.InvalidName(rawName))
    }
    val key = parsed.strongKey()

    if (key in names.strongNames) {
        return Err(ComponentValidatorError.DuplicateName(rawName))
    }

    val attributeError = validateAttributes(name, parsed, type).fold(
        success = { null },
        failure = { error -> error },
    )
    if (attributeError != null) return Err(attributeError)

    val annotationError = validateAnnotatedFunction(parsed, type, names).fold(
        success = { null },
        failure = { error -> error },
    )
    if (annotationError != null) return Err(annotationError)

    names.strongNames += key
    val resource = ((type as? ComponentEntityType.Type)?.referenced?.type as? ComponentDefinedType.Resource)?.id
    if (names.resourceNamesVisible && parsed is ParsedExternalName.Label && resource != null) {
        names.resourceNames[resource] = parsed.label
        names.resourcesByName[parsed.label] = resource
    }
    return Ok(Unit)
}

private fun validateAttributes(
    name: NameAttributes,
    parsed: ParsedExternalName,
    type: ComponentEntityType,
): Result<Unit, ComponentValidatorError> {
    val kinds = mutableSetOf<String>()
    name.attributes.forEach { attribute ->
        val kind = attribute::class.simpleName.orEmpty()
        if (!kinds.add(kind)) {
            return Err(ComponentValidatorError.InvalidName("duplicate $kind attribute on ${name.name.name}"))
        }

        when (attribute) {
            is ExternalAttribute.Implements -> {
                if (parsed !is ParsedExternalName.Label || type !is ComponentEntityType.Instance) {
                    return Err(ComponentValidatorError.InvalidName("implements requires a plain instance name"))
                }
                if (parseInterfaceName(attribute.interfaceName) == null) {
                    return Err(ComponentValidatorError.InvalidName(attribute.interfaceName))
                }
            }
            is ExternalAttribute.VersionSuffix -> {
                if (!SEMVER_SUFFIX.matches(attribute.suffix)) {
                    return Err(ComponentValidatorError.InvalidName(attribute.suffix))
                }
                val interfaceName = parsed as? ParsedExternalName.Interface
                    ?: return Err(ComponentValidatorError.InvalidName("version suffix requires an interface name"))
                val version = interfaceName.version
                if (version == null || !CANON_VERSION.matches(version)) {
                    return Err(ComponentValidatorError.InvalidName("version suffix requires a canonical version"))
                }
                if (!SEMVER.matches(version + attribute.suffix)) {
                    return Err(ComponentValidatorError.InvalidName(version + attribute.suffix))
                }
            }
            is ExternalAttribute.ExternalId -> Unit
        }
    }
    return Ok(Unit)
}

private fun validateAnnotatedFunction(
    name: ParsedExternalName,
    type: ComponentEntityType,
    names: ComponentNameContext,
): Result<Unit, ComponentValidatorError> {
    val function = when (name) {
        is ParsedExternalName.Constructor,
        is ParsedExternalName.Method,
        is ParsedExternalName.Static,
        -> (type as? ComponentEntityType.Function)?.type
            ?: return Err(ComponentValidatorError.TypeMismatch("function", type::class.simpleName))
        else -> return Ok(Unit)
    }

    val resourceName = when (name) {
        is ParsedExternalName.Constructor -> name.resource
        is ParsedExternalName.Method -> name.resource
        is ParsedExternalName.Static -> name.resource
    }
    val resource = names.resourcesByName[resourceName]
        ?: return Err(ComponentValidatorError.InvalidName("unknown resource $resourceName"))

    return when (name) {
        is ParsedExternalName.Constructor -> {
            if (function.result.owns(resource)) {
                Ok(Unit)
            } else {
                Err(ComponentValidatorError.TypeMismatch("constructor result owning $resourceName", null))
            }
        }
        is ParsedExternalName.Method -> {
            val self = function.params.firstOrNull()
            if (self?.label == "self" && self.type.borrows(resource)) {
                Ok(Unit)
            } else {
                Err(ComponentValidatorError.TypeMismatch("self borrowing $resourceName", null))
            }
        }
        is ParsedExternalName.Static -> Ok(Unit)
    }
}

private fun ComponentValueType?.owns(resource: ComponentResourceTypeId): Boolean = when (this) {
    is ComponentValueType.Defined -> (definition.type as? ComponentDefinedType.Value)?.type.owns(resource)
    else -> false
}

private fun ComponentValueType.borrows(resource: ComponentResourceTypeId): Boolean = when (this) {
    is ComponentValueType.Defined -> (definition.type as? ComponentDefinedType.Value)?.type.borrows(resource)
    else -> false
}

private fun ComponentDefinedValueType?.owns(resource: ComponentResourceTypeId): Boolean = when (this) {
    is ComponentDefinedValueType.Own -> this.resource == resource
    is ComponentDefinedValueType.Result -> ok.owns(resource)
    else -> false
}

private fun ComponentDefinedValueType?.borrows(resource: ComponentResourceTypeId): Boolean =
    this is ComponentDefinedValueType.Borrow && this.resource == resource

private fun ParsedExternalName.isImportOnly(): Boolean = when (this) {
    is ParsedExternalName.Dependency,
    is ParsedExternalName.Url,
    is ParsedExternalName.Hash,
    -> true
    else -> false
}

private sealed interface ParsedExternalName {
    data class Label(val label: ComponentLabel) : ParsedExternalName

    data class Constructor(val resource: ComponentLabel) : ParsedExternalName

    data class Method(val resource: ComponentLabel, val method: ComponentLabel) : ParsedExternalName

    data class Static(val resource: ComponentLabel, val method: ComponentLabel) : ParsedExternalName

    data class Interface(val name: String, val version: String?) : ParsedExternalName

    data class Dependency(val name: String) : ParsedExternalName

    data class Url(val name: String) : ParsedExternalName

    data class Hash(val name: String) : ParsedExternalName
}

private fun parseExternalName(name: String): ParsedExternalName? = when {
    name.startsWith(CONSTRUCTOR) -> ComponentLabel.parse(name.removePrefix(CONSTRUCTOR))
        ?.let(ParsedExternalName::Constructor)
    name.startsWith(METHOD) -> parseResourceFunction(name.removePrefix(METHOD), ParsedExternalName::Method)
    name.startsWith(STATIC) -> parseResourceFunction(name.removePrefix(STATIC), ParsedExternalName::Static)
    name.startsWith(UNLOCKED_DEPENDENCY) -> parseUnlockedDependency(name)
    name.startsWith(LOCKED_DEPENDENCY) -> parseLockedDependency(name)
    name.startsWith(URL) -> parseUrl(name)
    name.startsWith(INTEGRITY) -> parseIntegrity(name)
    else -> ComponentLabel.parse(name)?.let(ParsedExternalName::Label) ?: parseInterfaceName(name)
}

private inline fun <T : ParsedExternalName> parseResourceFunction(
    name: String,
    constructor: (ComponentLabel, ComponentLabel) -> T,
): T? {
    val separator = name.indexOf('.')
    if (separator <= 0 || separator == name.lastIndex) return null
    val resource = ComponentLabel.parse(name.substring(0, separator)) ?: return null
    val method = ComponentLabel.parse(name.substring(separator + 1)) ?: return null
    return constructor(resource, method)
}

private fun parseInterfaceName(name: String): ParsedExternalName.Interface? {
    val at = name.indexOf('@')
    if (at != name.lastIndexOf('@')) return null
    val base = if (at == -1) name else name.substring(0, at)
    val version = if (at == -1) null else name.substring(at + 1)
    if (version != null && !SEMVER.matches(version) && !CANON_VERSION.matches(version)) return null

    val projections = base.split('/')
    if (projections.size != 2 || ComponentLabel.parse(projections[1]) == null) return null
    val namespacesAndWords = projections.first().split(':')
    if (namespacesAndWords.size != 2 || namespacesAndWords.any { !WORDS.matches(it) }) return null
    return ParsedExternalName.Interface(name, version)
}

private fun parseUnlockedDependency(name: String): ParsedExternalName.Dependency? {
    val (query, trailing) = parseBracketed(name.removePrefix(UNLOCKED_DEPENDENCY)) ?: return null
    if (trailing.isNotEmpty()) return null

    val at = query.indexOf('@')
    if (at != query.lastIndexOf('@')) return null
    val packagePath = if (at == -1) query else query.substring(0, at)
    if (!isPackagePath(packagePath, requireProjection = false)) return null
    if (at == -1) return ParsedExternalName.Dependency(name)

    val versionQuery = query.substring(at + 1)
    val validVersionQuery = versionQuery == "*" ||
        versionQuery.startsWith('{') && versionQuery.endsWith('}') &&
        isVersionRange(versionQuery.substring(1, versionQuery.lastIndex))
    return if (validVersionQuery) ParsedExternalName.Dependency(name) else null
}

private fun parseLockedDependency(name: String): ParsedExternalName.Dependency? {
    val (packageName, trailing) = parseBracketed(name.removePrefix(LOCKED_DEPENDENCY)) ?: return null
    val at = packageName.indexOf('@')
    if (at != packageName.lastIndexOf('@')) return null
    val packagePath = if (at == -1) packageName else packageName.substring(0, at)
    val version = if (at == -1) null else packageName.substring(at + 1)
    if (!isPackagePath(packagePath, requireProjection = false)) return null
    if (version != null && !SEMVER.matches(version)) return null
    if (!isOptionalIntegrity(trailing)) return null
    return ParsedExternalName.Dependency(name)
}

private fun parseUrl(name: String): ParsedExternalName.Url? {
    val (url, trailing) = parseBracketed(name.removePrefix(URL)) ?: return null
    if ('<' in url || '>' in url || !isOptionalIntegrity(trailing)) return null
    return ParsedExternalName.Url(name)
}

private fun parseIntegrity(name: String): ParsedExternalName.Hash? {
    val (hash, trailing) = parseBracketed(name.removePrefix(INTEGRITY)) ?: return null
    return if (trailing.isEmpty() && isIntegrityHash(hash)) ParsedExternalName.Hash(name) else null
}

private fun parseBracketed(value: String): Pair<String, String>? {
    if (!value.startsWith('<')) return null
    val closingBracket = (1..value.lastIndex).firstOrNull { index ->
        value[index] == '>' && value.getOrNull(index + 1) != '='
    } ?: -1
    if (closingBracket == -1) return null
    return value.substring(1, closingBracket) to value.substring(closingBracket + 1)
}

private fun isOptionalIntegrity(value: String): Boolean {
    if (value.isEmpty()) return true
    if (!value.startsWith(OPTIONAL_INTEGRITY)) return false
    val (hash, trailing) = parseBracketed(value.removePrefix(",integrity=")) ?: return false
    return trailing.isEmpty() && isIntegrityHash(hash)
}

private fun isIntegrityHash(value: String): Boolean {
    val hashes = value.trim().split(WHITESPACE).filter(String::isNotEmpty)
    if (hashes.isEmpty()) return false

    return hashes.all { hash ->
        val encoded = HASH_ALGORITHMS.firstNotNullOfOrNull { algorithm ->
            hash.removePrefix("$algorithm-").takeIf { it != hash }
        } ?: return@all false
        isBase64(encoded.substringBefore('?'))
    }
}

private fun isBase64(value: String): Boolean {
    if (value.isEmpty()) return false
    var padding = 0
    value.forEachIndexed { index, character ->
        when {
            character.isLetterOrDigit() || character == '+' || character == '/' -> {
                if (padding != 0) return false
            }
            character == '=' && index > 0 && padding < 2 -> padding += 1
            else -> return false
        }
    }
    return true
}

private fun isVersionRange(value: String): Boolean = when {
    value.startsWith(">=") -> {
        val bounds = value.removePrefix(">=").split(' ', limit = 2)
        SEMVER.matches(bounds.first()) &&
            (bounds.size == 1 || bounds[1].startsWith('<') && SEMVER.matches(bounds[1].removePrefix("<")))
    }
    value.startsWith('<') -> SEMVER.matches(value.removePrefix("<"))
    else -> false
}

private fun isPackagePath(
    value: String,
    requireProjection: Boolean,
): Boolean {
    val projections = value.split('/')
    if (projections.size > 2 || requireProjection && projections.size != 2) return false
    if (projections.size == 2 && ComponentLabel.parse(projections[1]) == null) return false

    val namespaceAndPackage = projections.first().split(':')
    return namespaceAndPackage.size == 2 && namespaceAndPackage.all(WORDS::matches)
}

private fun ParsedExternalName.strongKey(): String = when (this) {
    is ParsedExternalName.Label -> "label:${label.value.lowercase()}"
    is ParsedExternalName.Constructor -> "constructor:${resource.value.lowercase()}"
    is ParsedExternalName.Method -> resourceFunctionKey(resource, method)
    is ParsedExternalName.Static -> resourceFunctionKey(resource, method)
    is ParsedExternalName.Interface -> "interface:${name.lowercase()}"
    is ParsedExternalName.Dependency -> "dependency:$name"
    is ParsedExternalName.Url -> "url:$name"
    is ParsedExternalName.Hash -> "hash:$name"
}

private fun resourceFunctionKey(resource: ComponentLabel, method: ComponentLabel): String =
    if (resource == method) {
        "label:${resource.value.lowercase()}"
    } else {
        "resource-function:${resource.value.lowercase()}.${method.value.lowercase()}"
    }

private const val CONSTRUCTOR = "[constructor]"
private const val METHOD = "[method]"
private const val STATIC = "[static]"
private const val UNLOCKED_DEPENDENCY = "unlocked-dep="
private const val LOCKED_DEPENDENCY = "locked-dep="
private const val URL = "url="
private const val INTEGRITY = "integrity="
private const val OPTIONAL_INTEGRITY = ",integrity=<"
private val WORDS = Regex("[a-z][0-9a-z]*(?:-[0-9a-z]+)*")
private val WHITESPACE = Regex("\\s+")
private val HASH_ALGORITHMS = listOf("sha256", "sha384", "sha512")
private val CANON_VERSION = Regex("""(?:[1-9][0-9]*|0\.[1-9][0-9]*|0\.0\.[1-9][0-9]*|0\.0\.0)""")
private val SEMVER_SUFFIX = Regex("[0-9A-Za-z.+-]*")
private val SEMVER = Regex(
    """(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)""" +
        """(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?""",
)
