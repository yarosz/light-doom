package io.github.charlietap.chasm.validator.validator.component.canonical

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.CanonicalOption
import io.github.charlietap.chasm.ast.component.ComponentStringEncoding
import io.github.charlietap.chasm.config.ComponentFeature
import io.github.charlietap.chasm.type.component.ComponentFunctionType
import io.github.charlietap.chasm.type.component.canonical.CanonicalCoreFunctionType
import io.github.charlietap.chasm.type.component.canonical.coreValueType
import io.github.charlietap.chasm.validator.context.component.ComponentValidationContext
import io.github.charlietap.chasm.validator.error.ComponentValidatorError
import io.github.charlietap.chasm.validator.type.component.CanonicalAbiOptions

internal enum class CanonicalOptionUse {
    Lift,
    Lower,
    TaskReturn,
    StreamRead,
    StreamWrite,
    FutureRead,
    FutureWrite,
    ErrorContextNew,
    ErrorContextDebugMessage,
}

internal typealias CanonicalOptionsValidator = (
    ComponentValidationContext,
    List<CanonicalOption>,
    CanonicalOptionUse,
    ComponentFunctionType?,
) -> Result<CanonicalAbiOptions, ComponentValidatorError>

internal fun CanonicalOptionValidator(
    context: ComponentValidationContext,
    options: List<CanonicalOption>,
    use: CanonicalOptionUse,
    functionType: ComponentFunctionType? = null,
): Result<CanonicalAbiOptions, ComponentValidatorError> =
    CanonicalOptionValidator(
        context = context,
        options = options,
        use = use,
        functionType = functionType,
        coreFunction = ::CoreFunction,
        coreMemory = ::CoreMemory,
        coreFunctionTypeValidator = ::RequireCoreFunctionType,
        featureValidator = ::RequireComponentFeature,
    )

internal inline fun CanonicalOptionValidator(
    context: ComponentValidationContext,
    options: List<CanonicalOption>,
    use: CanonicalOptionUse,
    functionType: ComponentFunctionType?,
    crossinline coreFunction: (
        ComponentValidationContext,
        io.github.charlietap.chasm.ast.module.Index.FunctionIndex,
    ) -> Result<io.github.charlietap.chasm.type.DefinedType, ComponentValidatorError>,
    crossinline coreMemory: (
        ComponentValidationContext,
        io.github.charlietap.chasm.ast.module.Index.MemoryIndex,
    ) -> Result<io.github.charlietap.chasm.type.MemoryType, ComponentValidatorError>,
    crossinline coreFunctionTypeValidator: (
        io.github.charlietap.chasm.type.DefinedType,
        io.github.charlietap.chasm.type.DefinedType,
        String,
    ) -> Result<Unit, ComponentValidatorError>,
    crossinline featureValidator: (
        ComponentValidationContext,
        ComponentFeature,
    ) -> Result<Unit, ComponentValidatorError>,
): Result<CanonicalAbiOptions, ComponentValidatorError> = binding {
    var stringEncoding = ComponentStringEncoding.Utf8
    var memory: io.github.charlietap.chasm.type.MemoryType? = null
    var realloc: io.github.charlietap.chasm.type.DefinedType? = null
    var postReturn: io.github.charlietap.chasm.type.DefinedType? = null
    var callback: io.github.charlietap.chasm.type.DefinedType? = null
    var async = false

    var hasStringEncoding = false
    var hasMemory = false
    var hasRealloc = false
    var hasPostReturn = false
    var hasCallback = false
    var hasAsync = false

    options.forEach { option ->
        if (!use.allows(option)) {
            Err(ComponentValidatorError.InvalidCanonicalDefinition("canonical option is not allowed for ${use.name}"))
                .bind<Unit>()
        }
        when (option) {
            is CanonicalOption.StringEncoding -> {
                duplicate(hasStringEncoding, "string encoding").bind()
                hasStringEncoding = true
                stringEncoding = option.encoding
            }

            is CanonicalOption.Memory -> {
                duplicate(hasMemory, "memory").bind()
                hasMemory = true
                memory = coreMemory(context, option.index).bind()
            }

            is CanonicalOption.Realloc -> {
                duplicate(hasRealloc, "realloc").bind()
                hasRealloc = true
                realloc = coreFunction(context, option.index).bind()
            }

            is CanonicalOption.PostReturn -> {
                duplicate(hasPostReturn, "post-return").bind()
                hasPostReturn = true
                postReturn = coreFunction(context, option.index).bind()
            }

            CanonicalOption.Async -> {
                duplicate(hasAsync, "async").bind()
                hasAsync = true
                async = true
            }

            is CanonicalOption.Callback -> {
                duplicate(hasCallback, "callback").bind()
                hasCallback = true
                callback = coreFunction(context, option.index).bind()
            }
        }
    }

    val addressType = memory?.addressType ?: io.github.charlietap.chasm.type.AddressType.I32
    RequireMemory64(context, addressType).bind()

    if (realloc != null) {
        if (memory == null) {
            Err(ComponentValidatorError.InvalidCanonicalDefinition("realloc requires a memory option")).bind<Unit>()
        }
        val address = addressType.coreValueType()
        val expected = CanonicalCoreFunctionType(
            params = listOf(address, address, address, address),
            results = listOf(address),
        )
        coreFunctionTypeValidator(
            realloc,
            expected,
            "realloc uses a core function with an incorrect signature",
        ).bind()
    }

    if (async) {
        featureValidator(context, ComponentFeature.Async).bind()
        when (use) {
            CanonicalOptionUse.Lift,
            CanonicalOptionUse.Lower,
            -> if (functionType?.async != true) {
                Err(
                    ComponentValidatorError.InvalidCanonicalDefinition(
                        "the async canonical option requires an async function type",
                    ),
                ).bind<Unit>()
            }

            CanonicalOptionUse.StreamRead,
            CanonicalOptionUse.StreamWrite,
            CanonicalOptionUse.FutureRead,
            CanonicalOptionUse.FutureWrite,
            -> featureValidator(context, ComponentFeature.MoreAsyncBuiltins).bind()

            else -> Err(
                ComponentValidatorError.InvalidCanonicalDefinition(
                    "the async canonical option is not allowed for ${use.name}",
                ),
            ).bind<Unit>()
        }
    }

    if (postReturn != null && async) {
        Err(ComponentValidatorError.InvalidCanonicalDefinition("post-return cannot be combined with async")).bind<Unit>()
    }

    if (callback != null) {
        if (!async || use != CanonicalOptionUse.Lift) {
            Err(ComponentValidatorError.InvalidCanonicalDefinition("callback requires an async canonical lift"))
                .bind<Unit>()
        }
        val expected = CanonicalCoreFunctionType(
            params = listOf(I32, I32, I32),
            results = listOf(I32),
        )
        coreFunctionTypeValidator(
            callback,
            expected,
            "callback uses a core function with an incorrect signature",
        ).bind()
    } else if (async && use == CanonicalOptionUse.Lift) {
        featureValidator(context, ComponentFeature.StackfulAsync).bind()
    }

    CanonicalAbiOptions(
        stringEncoding = stringEncoding,
        addressType = addressType,
        memory = memory,
        realloc = realloc,
        postReturn = postReturn,
        callback = callback,
        async = async,
    )
}

private fun CanonicalOptionUse.allows(option: CanonicalOption): Boolean = when (this) {
    CanonicalOptionUse.Lift -> true
    CanonicalOptionUse.Lower -> option !is CanonicalOption.PostReturn && option !is CanonicalOption.Callback
    CanonicalOptionUse.TaskReturn -> option is CanonicalOption.StringEncoding || option is CanonicalOption.Memory
    CanonicalOptionUse.StreamRead,
    CanonicalOptionUse.StreamWrite,
    CanonicalOptionUse.FutureRead,
    CanonicalOptionUse.FutureWrite,
    -> option is CanonicalOption.StringEncoding ||
        option is CanonicalOption.Memory ||
        option is CanonicalOption.Realloc ||
        option == CanonicalOption.Async

    CanonicalOptionUse.ErrorContextNew,
    CanonicalOptionUse.ErrorContextDebugMessage,
    -> option is CanonicalOption.StringEncoding ||
        option is CanonicalOption.Memory ||
        option is CanonicalOption.Realloc
}

private fun duplicate(
    duplicate: Boolean,
    option: String,
): Result<Unit, ComponentValidatorError> = if (duplicate) {
    Err(ComponentValidatorError.InvalidCanonicalDefinition("$option is specified more than once"))
} else {
    com.github.michaelbull.result.Ok(Unit)
}

private val I32 = io.github.charlietap.chasm.type.ValueType.Number(io.github.charlietap.chasm.type.NumberType.I32)
