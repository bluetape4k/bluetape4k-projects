package io.bluetape4k.codec

import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.support.unsafeLazy

/**
 * 바이트 배열을 문자열로 인코딩/디코딩 하는 Interface
 */
private val base64Encoder by unsafeLazy { Base64StringEncoder() }

/**
 * 문자열을 16진법 (Hex Decimal) 문자로 인코딩/디코딩 합니다
 */
private val hexEncoder by unsafeLazy { HexStringEncoder() }

/**
 * [ByteArray]를 Base64 인코딩한 바이트 배열로 변환합니다.
 */
fun ByteArray?.encodeBase64ByteArray(): ByteArray =
    this?.run { base64Encoder.encode(this).toUtf8Bytes() } ?: emptyByteArray

/**
 * [ByteArray]를 Base64 인코딩한 문자열로 변환합니다.
 */
fun ByteArray?.encodeBase64String(): String =
    this?.run { base64Encoder.encode(this) } ?: EMPTY_STRING

/**
 * [String]을 Base64 인코딩한 문자열의 UTF-8 바이트 배열로 변환합니다.
 */
fun String?.encodeBase64ByteArray(): ByteArray =
    this?.run { base64Encoder.encode(this.toUtf8Bytes()).toUtf8Bytes() } ?: emptyByteArray

/**
 * [String]을 Base64 인코딩한 문자열로 변환합니다.
 */
fun String?.encodeBase64String(): String =
    this?.run { base64Encoder.encode(this.toUtf8Bytes()) } ?: EMPTY_STRING


/**
 * Base64 인코딩된 [ByteArray]를 디코딩한 바이트 배열로 변환합니다.
 */
fun ByteArray?.decodeBase64ByteArray(): ByteArray =
    this?.run { base64Encoder.decode(this.toUtf8String()) } ?: emptyByteArray

/**
 * Base64 인코딩된 [ByteArray]를 디코딩한 문자열로 변환합니다.
 */
fun ByteArray?.decodeBase64String(): String =
    this?.run { base64Encoder.decode(this.toUtf8String()).toUtf8String() } ?: EMPTY_STRING

/**
 * Base64 인코딩된 [String]을 디코딩한 바이트 배열로 변환합니다.
 */
fun String?.decodeBase64ByteArray(): ByteArray =
    this?.run { base64Encoder.decode(this) } ?: emptyByteArray

/**
 * Base64 인코딩된 [String]을 디코딩한 문자열로 변환합니다.
 */
fun String?.decodeBase64String(): String =
    this?.run { base64Encoder.decode(this).toUtf8String() } ?: EMPTY_STRING

/**
 * [ByteArray]를 16진법 문자열로 변환합니다.
 */
fun ByteArray?.encodeHexByteArray(): ByteArray =
    this?.run { hexEncoder.encode(this).toUtf8Bytes() } ?: emptyByteArray

/**
 * [ByteArray]를 16진법 문자열로 변환합니다.
 */
fun ByteArray?.encodeHexString(): String =
    this?.run { hexEncoder.encode(this) } ?: EMPTY_STRING

/**
 * [String]을 16진법 문자열로 인코딩한 UTF-8 바이트 배열로 변환합니다.
 */
fun String?.encodeHexByteArray(): ByteArray =
    this?.run { hexEncoder.encode(this.toUtf8Bytes()).toUtf8Bytes() } ?: emptyByteArray

/**
 * [String]을 16진법 문자열로 인코딩합니다.
 */
fun String?.encodeHexString(): String =
    this?.run { hexEncoder.encode(this.toUtf8Bytes()) } ?: EMPTY_STRING

/**
 * 16진법 문자열을 [ByteArray]로 변환합니다.
 */
fun ByteArray?.decodeHexByteArray(): ByteArray =
    this?.run { hexEncoder.decode(this.toUtf8String()) } ?: emptyByteArray

/**
 * 16진법 문자열을 [ByteArray]로 변환합니다.
 */
fun ByteArray?.decodeHexString(): String =
    this?.run { hexEncoder.decode(this.toUtf8String()).toUtf8String() } ?: EMPTY_STRING

/**
 * 16진법 문자열을 [ByteArray]로 변환합니다.
 */
fun String?.decodeHexByteArray(): ByteArray =
    this?.run { hexEncoder.decode(this) } ?: emptyByteArray

/**
 * 16진법 문자열을 [ByteArray]로 변환합니다.
 */
fun String?.decodeHexString(): String =
    this?.run { hexEncoder.decode(this).toUtf8String() } ?: EMPTY_STRING
