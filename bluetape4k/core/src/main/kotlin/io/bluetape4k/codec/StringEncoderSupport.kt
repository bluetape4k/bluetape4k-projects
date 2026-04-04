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
 *
 * 예제:
 * ```kotlin
 * val bytes = "hello".toByteArray()
 * val encoded = bytes.encodeBase64ByteArray() // Base64 인코딩된 ByteArray ("aGVsbG8=" 의 UTF-8 bytes)
 * ```
 *
 * @return Base64 인코딩된 바이트 배열. null이면 빈 바이트 배열 반환
 */
fun ByteArray?.encodeBase64ByteArray(): ByteArray =
    this?.let(base64Encoder::encode)?.toUtf8Bytes() ?: emptyByteArray

/**
 * [ByteArray]를 Base64 인코딩한 문자열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * val bytes = "hello".toByteArray()
 * bytes.encodeBase64String() // "aGVsbG8="
 * null.encodeBase64String()  // ""
 * ```
 *
 * @return Base64 인코딩된 문자열. null이면 빈 문자열 반환
 */
fun ByteArray?.encodeBase64String(): String =
    this?.let(base64Encoder::encode).orEmpty()

/**
 * [String]을 Base64 인코딩한 문자열의 UTF-8 바이트 배열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * "hello".encodeBase64ByteArray() // "aGVsbG8=" 의 UTF-8 ByteArray
 * (null as String?).encodeBase64ByteArray() // ByteArray(0)
 * ```
 *
 * @return Base64 인코딩 결과의 UTF-8 바이트 배열. null이면 빈 바이트 배열 반환
 */
fun String?.encodeBase64ByteArray(): ByteArray =
    this?.toUtf8Bytes()?.let(base64Encoder::encode)?.toUtf8Bytes() ?: emptyByteArray

/**
 * [String]을 Base64 인코딩한 문자열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * "hello".encodeBase64String()             // "aGVsbG8="
 * "aGVsbG8=".decodeBase64String()          // "hello"
 * (null as String?).encodeBase64String()   // ""
 * ```
 *
 * @return Base64 인코딩된 문자열. null이면 빈 문자열 반환
 */
fun String?.encodeBase64String(): String =
    this?.toUtf8Bytes()?.let(base64Encoder::encode).orEmpty()


/**
 * Base64 인코딩된 [ByteArray]를 디코딩한 바이트 배열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * val encoded = "aGVsbG8=".toByteArray() // Base64("hello")의 UTF-8 bytes
 * encoded.decodeBase64ByteArray()         // "hello"의 ByteArray
 * ```
 *
 * @return 디코딩된 바이트 배열. null이면 빈 바이트 배열 반환
 */
fun ByteArray?.decodeBase64ByteArray(): ByteArray =
    this?.toUtf8String()?.let(base64Encoder::decode) ?: emptyByteArray

/**
 * Base64 인코딩된 [ByteArray]를 디코딩한 문자열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * val encoded = "aGVsbG8=".toByteArray()
 * encoded.decodeBase64String() // "hello"
 * ```
 *
 * @return 디코딩된 UTF-8 문자열. null이면 빈 문자열 반환
 */
fun ByteArray?.decodeBase64String(): String =
    this?.toUtf8String()?.let(base64Encoder::decode)?.toUtf8String().orEmpty()

/**
 * Base64 인코딩된 [String]을 디코딩한 바이트 배열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * "aGVsbG8=".decodeBase64ByteArray() // "hello"의 ByteArray
 * (null as String?).decodeBase64ByteArray() // ByteArray(0)
 * ```
 *
 * @return 디코딩된 바이트 배열. null이면 빈 바이트 배열 반환
 */
fun String?.decodeBase64ByteArray(): ByteArray =
    this?.let(base64Encoder::decode) ?: emptyByteArray

/**
 * Base64 인코딩된 [String]을 디코딩한 문자열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * "aGVsbG8=".decodeBase64String()          // "hello"
 * (null as String?).decodeBase64String()   // ""
 * ```
 *
 * @return 디코딩된 UTF-8 문자열. null이면 빈 문자열 반환
 */
fun String?.decodeBase64String(): String =
    this?.let(base64Encoder::decode)?.toUtf8String().orEmpty()

/**
 * [ByteArray]를 16진법 문자열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * val bytes = "hi".toByteArray()
 * bytes.encodeHexByteArray() // "6869"의 UTF-8 ByteArray
 * ```
 *
 * @return 16진법 인코딩 결과의 UTF-8 바이트 배열. null이면 빈 바이트 배열 반환
 */
fun ByteArray?.encodeHexByteArray(): ByteArray =
    this?.let(hexEncoder::encode)?.toUtf8Bytes() ?: emptyByteArray

/**
 * [ByteArray]를 16진법 문자열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * "hi".toByteArray().encodeHexString() // "6869"
 * null.encodeHexString()               // ""
 * ```
 *
 * @return 16진법 인코딩된 문자열. null이면 빈 문자열 반환
 */
fun ByteArray?.encodeHexString(): String =
    this?.let(hexEncoder::encode).orEmpty()

/**
 * [String]을 16진법 문자열로 인코딩한 UTF-8 바이트 배열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * "hi".encodeHexByteArray() // "6869"의 UTF-8 ByteArray
 * ```
 *
 * @return 16진법 인코딩 결과의 UTF-8 바이트 배열. null이면 빈 바이트 배열 반환
 */
fun String?.encodeHexByteArray(): ByteArray =
    this?.toUtf8Bytes()?.let(hexEncoder::encode)?.toUtf8Bytes() ?: emptyByteArray

/**
 * [String]을 16진법 문자열로 인코딩합니다.
 *
 * 예제:
 * ```kotlin
 * "hi".encodeHexString()             // "6869"
 * "6869".decodeHexString()           // "hi"
 * (null as String?).encodeHexString() // ""
 * ```
 *
 * @return 16진법 인코딩된 문자열. null이면 빈 문자열 반환
 */
fun String?.encodeHexString(): String =
    this?.toUtf8Bytes()?.let(hexEncoder::encode).orEmpty()

/**
 * 16진법으로 인코딩된 [ByteArray]를 원래 바이트 배열로 디코딩합니다.
 *
 * 예제:
 * ```kotlin
 * val encoded = "6869".toByteArray() // hex("hi")의 UTF-8 bytes
 * encoded.decodeHexByteArray()       // "hi"의 ByteArray
 * ```
 *
 * @return 디코딩된 바이트 배열. null이면 빈 바이트 배열 반환
 */
fun ByteArray?.decodeHexByteArray(): ByteArray =
    this?.toUtf8String()?.let(hexEncoder::decode) ?: emptyByteArray

/**
 * 16진법으로 인코딩된 [ByteArray]를 디코딩한 문자열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * "6869".toByteArray().decodeHexString() // "hi"
 * ```
 *
 * @return 디코딩된 UTF-8 문자열. null이면 빈 문자열 반환
 */
fun ByteArray?.decodeHexString(): String =
    this?.toUtf8String()?.let(hexEncoder::decode)?.toUtf8String().orEmpty()

/**
 * 16진법 문자열을 디코딩한 바이트 배열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * "6869".decodeHexByteArray()          // "hi"의 ByteArray
 * (null as String?).decodeHexByteArray() // ByteArray(0)
 * ```
 *
 * @return 디코딩된 바이트 배열. null이면 빈 바이트 배열 반환
 */
fun String?.decodeHexByteArray(): ByteArray =
    this?.let(hexEncoder::decode) ?: emptyByteArray

/**
 * 16진법 문자열을 디코딩한 문자열로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * "6869".decodeHexString()           // "hi"
 * (null as String?).decodeHexString() // ""
 * ```
 *
 * @return 디코딩된 UTF-8 문자열. null이면 빈 문자열 반환
 */
fun String?.decodeHexString(): String =
    this?.let(hexEncoder::decode)?.toUtf8String().orEmpty()
