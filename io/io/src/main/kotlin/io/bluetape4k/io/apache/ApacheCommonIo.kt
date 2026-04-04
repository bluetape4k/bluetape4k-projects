package io.bluetape4k.io.apache

/**
 * Apache Commons의 ByteArrayOutputStream은 Buffer Size의 Array를 사용하므로,
 * 대용량인 경우 JDK 기본인 [java.io.ByteArrayOutputStream] 보다 성능이 좋습니다.
 *
 * ```kotlin
 * val out = ApacheByteArrayOutputStream(8192)
 * out.write("Hello, World!".toByteArray())
 * val bytes = out.toByteArray() // "Hello, World!" bytes
 * ```
 *
 * @see org.apache.commons.io.output.ByteArrayOutputStream
 */
typealias ApacheByteArrayOutputStream = org.apache.commons.io.output.ByteArrayOutputStream
