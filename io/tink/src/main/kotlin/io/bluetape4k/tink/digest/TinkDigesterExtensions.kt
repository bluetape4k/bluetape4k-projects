package io.bluetape4k.tink.digest

/**
 * 바이트 배열의 해시 다이제스트를 [TinkDigester]로 계산합니다.
 *
 * ```kotlin
 * val hash = "Hello".toByteArray().tinkDigest(TinkDigesters.SHA256)
 * ```
 *
 * @param digester 사용할 [TinkDigester] 인스턴스
 * @return 해시된 바이트 배열
 */
fun ByteArray.tinkDigest(digester: TinkDigester): ByteArray = digester.digest(this)

/**
 * 문자열의 해시 다이제스트를 [TinkDigester]로 계산합니다.
 * 결과는 Base64 인코딩 문자열로 반환됩니다.
 *
 * ```kotlin
 * val hash = "Hello, World!".tinkDigest(TinkDigesters.SHA256)
 * ```
 *
 * @param digester 사용할 [TinkDigester] 인스턴스
 * @return Base64 인코딩된 해시 문자열
 */
fun String.tinkDigest(digester: TinkDigester): String = digester.digest(this)

/**
 * 바이트 배열의 해시가 기대값과 일치하는지 [TinkDigester]로 검증합니다.
 *
 * ```kotlin
 * val matches = "Hello".toByteArray().matchesTinkDigest(expectedHash, TinkDigesters.SHA256)
 * ```
 *
 * @param expected 기대하는 해시 바이트 배열
 * @param digester 사용할 [TinkDigester] 인스턴스
 * @return 일치하면 `true`, 아니면 `false`
 */
fun ByteArray.matchesTinkDigest(expected: ByteArray, digester: TinkDigester): Boolean =
    digester.matches(this, expected)

/**
 * 문자열의 해시가 기대값(Base64)과 일치하는지 [TinkDigester]로 검증합니다.
 *
 * ```kotlin
 * val matches = "Hello, World!".matchesTinkDigest(expectedHash, TinkDigesters.SHA256)
 * ```
 *
 * @param expected 기대하는 Base64 인코딩된 해시 문자열
 * @param digester 사용할 [TinkDigester] 인스턴스
 * @return 일치하면 `true`, 아니면 `false`
 */
fun String.matchesTinkDigest(expected: String, digester: TinkDigester): Boolean =
    digester.matches(this, expected)
