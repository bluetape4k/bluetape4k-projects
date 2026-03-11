package io.bluetape4k.codec

import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String

/**
 * 바이트 배열을 문자열로 인코딩/디코딩 하는 Interface
 */
private val base64Encoder by lazy { Base64StringEncoder() }

/**
 * 문자열을 16진법 (Hex Decimal) 문자로 인코딩/디코딩 합니다
 */
private val hexEncoder by lazy { HexStringEncoder() }

/**
 * [ByteArray]를 Base64 인코딩한 바이트 배열로 변환합니다.
 */
fun ByteArray?.encodeBase64ByteArray(): ByteArray =
    this?.let(base64Encoder::encode)?.toUtf8Bytes() ?: emptyByteArray

/**
 * [ByteArray]를 Base64 인코딩한 문자열로 변환합니다.
 */
fun ByteArray?.encodeBase64String(): String =
    this?.let(base64Encoder::encode).orEmpty()

/**
 * [String]을 Base64 인코딩한 문자열의 UTF-8 바이트 배열로 변환합니다.
 */
fun String?.encodeBase64ByteArray(): ByteArray =
    this?.toUtf8Bytes()?.let(base64Encoder::encode)?.toUtf8Bytes() ?: emptyByteArray

/**
 * [String]을 Base64 인코딩한 문자열로 변환합니다.
 */
fun String?.encodeBase64String(): String =
    this?.toUtf8Bytes()?.let(base64Encoder::encode).orEmpty()


/**
 * Base64 인코딩된 [ByteArray]를 디코딩한 바이트 배열로 변환합니다.
 */
fun ByteArray?.decodeBase64ByteArray(): ByteArray =
    this?.toUtf8String()?.let(base64Encoder::decode) ?: emptyByteArray

/**
 * Base64 인코딩된 [ByteArray]를 디코딩한 문자열로 변환합니다.
 */
fun ByteArray?.decodeBase64String(): String =
    this?.toUtf8String()?.let(base64Encoder::decode)?.toUtf8String().orEmpty()

/**
 * Base64 인코딩된 [String]을 디코딩한 바이트 배열로 변환합니다.
 */
fun String?.decodeBase64ByteArray(): ByteArray =
    this?.let(base64Encoder::decode) ?: emptyByteArray

/**
 * Base64 인코딩된 [String]을 디코딩한 문자열로 변환합니다.
 */
fun String?.decodeBase64String(): String =
    this?.let(base64Encoder::decode)?.toUtf8String().orEmpty()

/**
 * [ByteArray]를 16진법 문자열로 변환합니다.
 */
fun ByteArray?.encodeHexByteArray(): ByteArray =
    this?.let(hexEncoder::encode)?.toUtf8Bytes() ?: emptyByteArray

/**
 * [ByteArray]를 16진법 문자열로 변환합니다.
 */
fun ByteArray?.encodeHexString(): String =
    this?.let(hexEncoder::encode).orEmpty()

/**
 * [String]을 16진법 문자열로 인코딩한 UTF-8 바이트 배열로 변환합니다.
 */
fun String?.encodeHexByteArray(): ByteArray =
    this?.toUtf8Bytes()?.let(hexEncoder::encode)?.toUtf8Bytes() ?: emptyByteArray

/**
 * [String]을 16진법 문자열로 인코딩합니다.
 */
fun String?.encodeHexString(): String =
    this?.toUtf8Bytes()?.let(hexEncoder::encode).orEmpty()

/**
 * 16진법 문자열을 [ByteArray]로 변환합니다.
 */
fun ByteArray?.decodeHexByteArray(): ByteArray =
    this?.toUtf8String()?.let(hexEncoder::decode) ?: emptyByteArray

/**
 * 16진법 문자열을 [ByteArray]로 변환합니다.
 */
fun ByteArray?.decodeHexString(): String =
    this?.toUtf8String()?.let(hexEncoder::decode)?.toUtf8String().orEmpty()

/**
 * 16진법 문자열을 [ByteArray]로 변환합니다.
 */
fun String?.decodeHexByteArray(): ByteArray =
    this?.let(hexEncoder::decode) ?: emptyByteArray

/**
 * 16진법 문자열을 [ByteArray]로 변환합니다.
 */
fun String?.decodeHexString(): String =
    this?.let(hexEncoder::decode)?.toUtf8String().orEmpty()
