package io.bluetape4k.io.okio

import okio.ByteString
import java.nio.charset.Charset

/**
 * [bytes]를 가지는 [ByteString] 인스턴스를 생성합니다.
 */
@JvmName("byteStringOfBytes")
fun byteStringOf(vararg bytes: Byte): ByteString = ByteString.of(*bytes)

/**
 * [byteArray]를 가지는 [ByteString] 인스턴스를 생성합니다.
 */
@JvmName("byteStringOfByteArray")
fun byteStringOf(byteArray: ByteArray): ByteString = ByteString.of(*byteArray)

/**
 * [text]를 가지는 [ByteString] 인스턴스를 생성합니다.
 */
fun byteStringOf(text: String, charset: Charset = Charsets.UTF_8): ByteString =
    byteStringOf(text.toByteArray(charset))
