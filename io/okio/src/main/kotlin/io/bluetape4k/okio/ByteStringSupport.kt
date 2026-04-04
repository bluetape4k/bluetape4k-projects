package io.bluetape4k.okio

import okio.ByteString
import java.nio.charset.Charset

/**
 * [bytes]를 가지는 [ByteString] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val bs = byteStringOf(0x48.toByte(), 0x69.toByte())
 * val size = bs.size
 * // size == 2
 * ```
 */
@JvmName("byteStringOfBytes")
fun byteStringOf(vararg bytes: Byte): ByteString = ByteString.of(*bytes)

/**
 * [byteArray]를 가지는 [ByteString] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val bytes = byteArrayOf(1, 2, 3)
 * val bs = byteStringOf(bytes)
 * val size = bs.size
 * // size == 3
 * ```
 */
@JvmName("byteStringOfByteArray")
fun byteStringOf(byteArray: ByteArray): ByteString = ByteString.of(*byteArray)

/**
 * [text]를 가지는 [ByteString] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val bs = byteStringOf("hello")
 * val text = bs.utf8()
 * // text == "hello"
 * ```
 */
@JvmName("byteStringOfText")
fun byteStringOf(text: String, charset: Charset = Charsets.UTF_8): ByteString =
    byteStringOf(text.toByteArray(charset))
