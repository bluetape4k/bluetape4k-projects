package io.bluetape4k.crypto.digest

/**
 * 문자열을 지정된 [Digester]로 해시 다이제스트합니다.
 *
 * ```
 * val digest = "Hello, World!".digest(Digesters.SHA256)
 * ```
 *
 * @receiver 다이제스트할 문자열
 * @param digester 사용할 [Digester] 인스턴스
 * @return 다이제스트된 문자열
 */
fun String.digest(digester: Digester): String =
    digester.digest(this)

/**
 * 바이트 배열을 지정된 [Digester]로 해시 다이제스트합니다.
 *
 * ```
 * val digest = "Hello".toByteArray().digest(Digesters.SHA256)
 * ```
 *
 * @receiver 다이제스트할 바이트 배열
 * @param digester 사용할 [Digester] 인스턴스
 * @return 다이제스트된 바이트 배열
 */
fun ByteArray.digest(digester: Digester): ByteArray =
    digester.digest(this)

/**
 * 문자열이 다이제스트된 값과 일치하는지 확인합니다.
 *
 * ```
 * val digest = "Hello".digest(Digesters.SHA256)
 * "Hello".matchesDigest(digest, Digesters.SHA256) // true
 * ```
 *
 * @receiver 원본 문자열
 * @param digest 비교할 다이제스트 문자열
 * @param digester 사용할 [Digester] 인스턴스
 * @return 일치 여부
 */
fun String.matchesDigest(digest: String, digester: Digester): Boolean =
    digester.matches(this, digest)

/**
 * 바이트 배열이 다이제스트된 값과 일치하는지 확인합니다.
 *
 * ```
 * val bytes = "Hello".toByteArray()
 * val digest = bytes.digest(Digesters.SHA256)
 * bytes.matchesDigest(digest, Digesters.SHA256) // true
 * ```
 *
 * @receiver 원본 바이트 배열
 * @param digest 비교할 다이제스트 바이트 배열
 * @param digester 사용할 [Digester] 인스턴스
 * @return 일치 여부
 */
fun ByteArray.matchesDigest(digest: ByteArray, digester: Digester): Boolean =
    digester.matches(this, digest)
