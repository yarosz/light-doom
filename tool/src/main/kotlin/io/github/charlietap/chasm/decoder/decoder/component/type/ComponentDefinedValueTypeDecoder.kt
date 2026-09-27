package io.github.charlietap.chasm.decoder.decoder.component.type

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.github.charlietap.chasm.ast.component.DefinedValueType
import io.github.charlietap.chasm.ast.component.Index.ComponentTypeIndex
import io.github.charlietap.chasm.ast.component.KeyType
import io.github.charlietap.chasm.ast.component.LabeledValueType
import io.github.charlietap.chasm.ast.component.ValueType
import io.github.charlietap.chasm.ast.component.VariantCase
import io.github.charlietap.chasm.ast.value.NameValue
import io.github.charlietap.chasm.decoder.context.ComponentDecoderContext
import io.github.charlietap.chasm.decoder.decoder.ComponentDecoder
import io.github.charlietap.chasm.decoder.decoder.component.ComponentOptionalDecoder
import io.github.charlietap.chasm.decoder.decoder.component.index.ComponentTypeIndexDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ComponentVectorDecoder
import io.github.charlietap.chasm.decoder.decoder.vector.ReaderVectorDecoder
import io.github.charlietap.chasm.decoder.error.ComponentTypeDecodeError
import io.github.charlietap.chasm.decoder.error.WasmDecodeError

internal fun ComponentDefinedValueTypeDecoder(
    context: ComponentDecoderContext,
): Result<DefinedValueType, WasmDecodeError> = ComponentDefinedValueTypeDecoder(
    context = context,
    primitiveValueTypeDecoder = ::ComponentPrimitiveValueTypeDecoder,
    labeledValueTypeDecoder = ::ComponentLabeledValueTypeDecoder,
    labeledValueTypeVectorDecoder = ::ReaderVectorDecoder,
    variantCaseDecoder = ::ComponentVariantCaseDecoder,
    variantCaseVectorDecoder = ::ReaderVectorDecoder,
    valueTypeDecoder = ::ComponentValueTypeDecoder,
    valueTypeVectorDecoder = ::ReaderVectorDecoder,
    labelDecoder = ::ComponentLabelDecoder,
    labelVectorDecoder = ::ReaderVectorDecoder,
    optionalValueTypeDecoder = { scopedContext ->
        ComponentOptionalDecoder(scopedContext, ::ComponentValueTypeDecoder)
    },
    typeIndexDecoder = ::ComponentTypeIndexDecoder,
    mapKeyDecoder = ::ComponentMapKeyDecoder,
)

@Suppress("LongParameterList")
internal inline fun ComponentDefinedValueTypeDecoder(
    context: ComponentDecoderContext,
    crossinline primitiveValueTypeDecoder: ComponentDecoder<DefinedValueType>,
    noinline labeledValueTypeDecoder: ComponentDecoder<LabeledValueType>,
    crossinline labeledValueTypeVectorDecoder: ComponentVectorDecoder<LabeledValueType>,
    noinline variantCaseDecoder: ComponentDecoder<VariantCase>,
    crossinline variantCaseVectorDecoder: ComponentVectorDecoder<VariantCase>,
    noinline valueTypeDecoder: ComponentDecoder<ValueType>,
    crossinline valueTypeVectorDecoder: ComponentVectorDecoder<ValueType>,
    noinline labelDecoder: ComponentDecoder<NameValue>,
    crossinline labelVectorDecoder: ComponentVectorDecoder<NameValue>,
    crossinline optionalValueTypeDecoder: ComponentDecoder<ValueType?>,
    crossinline typeIndexDecoder: ComponentDecoder<ComponentTypeIndex>,
    crossinline mapKeyDecoder: ComponentDecoder<KeyType>,
): Result<DefinedValueType, WasmDecodeError> = binding {
    val opcode = context.reader.peekUByte()
    if (opcode == TYPE_ERROR_CONTEXT || opcode in TYPE_STRING..TYPE_BOOL) {
        primitiveValueTypeDecoder(context).bind()
    } else {
        context.reader.ubyte()
        when (opcode) {
            TYPE_RECORD -> DefinedValueType.Record(
                labeledValueTypeVectorDecoder(context, labeledValueTypeDecoder).bind().vector,
            )
            TYPE_VARIANT -> DefinedValueType.Variant(
                variantCaseVectorDecoder(context, variantCaseDecoder).bind().vector,
            )
            TYPE_LIST -> DefinedValueType.List(valueTypeDecoder(context).bind())
            TYPE_FIXED_LENGTH_LIST -> DefinedValueType.FixedLengthList(
                element = valueTypeDecoder(context).bind(),
                length = context.reader.uint(),
            )
            TYPE_TUPLE -> DefinedValueType.Tuple(
                valueTypeVectorDecoder(context, valueTypeDecoder).bind().vector,
            )
            TYPE_FLAGS -> DefinedValueType.Flags(
                labelVectorDecoder(context, labelDecoder).bind().vector,
            )
            TYPE_ENUM -> DefinedValueType.Enum(
                labelVectorDecoder(context, labelDecoder).bind().vector,
            )
            TYPE_OPTION -> DefinedValueType.Option(valueTypeDecoder(context).bind())
            TYPE_RESULT -> DefinedValueType.Result(
                ok = optionalValueTypeDecoder(context).bind(),
                error = optionalValueTypeDecoder(context).bind(),
            )
            TYPE_OWN -> DefinedValueType.Own(typeIndexDecoder(context).bind())
            TYPE_BORROW -> DefinedValueType.Borrow(typeIndexDecoder(context).bind())
            TYPE_STREAM -> DefinedValueType.Stream(optionalValueTypeDecoder(context).bind())
            TYPE_FUTURE -> DefinedValueType.Future(optionalValueTypeDecoder(context).bind())
            TYPE_MAP -> DefinedValueType.Map(
                key = mapKeyDecoder(context).bind(),
                value = valueTypeDecoder(context).bind(),
            )
            else -> Err(ComponentTypeDecodeError.UnknownDefinedValueType(opcode)).bind<DefinedValueType>()
        }
    }
}

private const val TYPE_BOOL: UByte = 0x7Fu
private const val TYPE_STRING: UByte = 0x73u
private const val TYPE_RECORD: UByte = 0x72u
private const val TYPE_VARIANT: UByte = 0x71u
private const val TYPE_LIST: UByte = 0x70u
private const val TYPE_TUPLE: UByte = 0x6Fu
private const val TYPE_FLAGS: UByte = 0x6Eu
private const val TYPE_ENUM: UByte = 0x6Du
private const val TYPE_OPTION: UByte = 0x6Bu
private const val TYPE_RESULT: UByte = 0x6Au
private const val TYPE_OWN: UByte = 0x69u
private const val TYPE_BORROW: UByte = 0x68u
private const val TYPE_FIXED_LENGTH_LIST: UByte = 0x67u
private const val TYPE_STREAM: UByte = 0x66u
private const val TYPE_FUTURE: UByte = 0x65u
private const val TYPE_ERROR_CONTEXT: UByte = 0x64u
private const val TYPE_MAP: UByte = 0x63u
