package io.bluetape4k.crypto.digest

/**
 * 문자열을 지정된 [Digester]로 해시 다이제스트합니다.
 *
 * ## 동작/계약
 * - 수신 문자열을 [Digester.digest] 문자열 경로에 위임합니다.
 * - 수신 문자열은 변경하지 않고 새 다이제스트 문자열을 반환합니다.
 *
 * ```kotlin
 * val digest = "Hello, World!".digest(Digesters.SHA256)
 * // digest.isNotBlank() == true
 * ```
 * @param digester 사용할 다이제스터
 */
fun String.digest(digester: Digester): String =
    digester.digest(this)

/**
 * 바이트 배열을 지정된 [Digester]로 해시 다이제스트합니다.
 *
 * ## 동작/계약
 * - 수신 배열을 [Digester.digest] 바이트 경로에 위임합니다.
 * - 수신 배열은 변경하지 않고 새 바이트 배열을 반환합니다.
 *
 * ```kotlin
 * val digest = "Hello".toByteArray().digest(Digesters.SHA256)
 * // digest.isNotEmpty() == true
 * ```
 * @param digester 사용할 다이제스터
 */
fun ByteArray.digest(digester: Digester): ByteArray =
    digester.digest(this)

/**
 * 문자열이 다이제스트된 값과 일치하는지 확인합니다.
 *
 * ## 동작/계약
 * - 수신 문자열과 [digest]를 [Digester.matches] 문자열 경로에 위임합니다.
 * - 비교 과정에서 수신 문자열/인자 문자열을 변경하지 않습니다.
 *
 * ```kotlin
 * val digest = "Hello".digest(Digesters.SHA256)
 * val ok = "Hello".matchesDigest(digest, Digesters.SHA256)
 * // ok == true
 * ```
 * @param digest 비교할 다이제스트 문자열
 * @param digester 사용할 다이제스터
 */
fun String.matchesDigest(digest: String, digester: Digester): Boolean =
    digester.matches(this, digest)

/**
 * 바이트 배열이 다이제스트된 값과 일치하는지 확인합니다.
 *
 * ## 동작/계약
 * - 수신 배열과 [digest]를 [Digester.matches] 바이트 경로에 위임합니다.
 * - 비교 과정에서 수신 배열/인자 배열을 변경하지 않습니다.
 *
 * ```kotlin
 * val bytes = "Hello".toByteArray()
 * val digest = bytes.digest(Digesters.SHA256)
 * val ok = bytes.matchesDigest(digest, Digesters.SHA256)
 * // ok == true
 * ```
 * @param digest 비교할 다이제스트 바이트 배열
 * @param digester 사용할 다이제스터
 */
fun ByteArray.matchesDigest(digest: ByteArray, digester: Digester): Boolean =
    digester.matches(this, digest)
