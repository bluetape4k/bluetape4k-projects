package io.bluetape4k.http.jdk

import io.bluetape4k.http.readOwnedBodyBytes
import io.bluetape4k.io.ByteLimitExceededException
import io.bluetape4k.support.requireZeroOrPositiveNumber
import java.io.IOException
import java.io.InputStream
import java.net.http.HttpHeaders
import java.net.http.HttpResponse
import java.nio.charset.Charset

/**
 * JDK HTTP 응답의 body stream을 [maxBytes] 이하일 때만 전체 [ByteArray]로 반환합니다.
 *
 * 단일 non-negative `Content-Length`만 조기 거부 힌트로 사용하며 실제 stream에도 같은
 * 상한을 적용합니다. status code는 해석하지 않습니다. 획득한 body stream은 성공과 실패
 * 모두에서 정확히 한 번 닫지만 response의 다른 자원 수명은 호출자가 관리합니다.
 *
 * 이 호출과 마지막 EOF 확인은 blocking될 수 있습니다. client timeout을 구성하고 coroutine
 * 취소만 의존하지 말고 supervisor가 stream이나 상위 transport를 중단할 수 있게 해야 합니다.
 *
 * @throws IllegalArgumentException [maxBytes]가 음수인 경우
 * @throws ByteLimitExceededException 본문이 [maxBytes]를 초과한 경우
 * @throws IOException body 획득, 읽기 또는 닫기에 실패한 경우
 */
fun HttpResponse<InputStream>.readBodyBytes(maxBytes: Int): ByteArray {
    maxBytes.requireZeroOrPositiveNumber("maxBytes")
    val declaredLength = headers().strictContentLengthOrNull()

    return readOwnedBodyBytes(
        maxBytes = maxBytes,
        knownOversize = declaredLength != null && declaredLength > maxBytes.toLong(),
        acquireBody = { body() },
    )
}

/**
 * JDK HTTP 응답 body를 byte 상한 안에서 읽고 [charset]으로 변환합니다.
 *
 * 상한은 문자 수가 아니라 body stream의 byte 수에 적용됩니다. blocking, timeout, stream
 * ownership 및 status 처리 책임은 [readBodyBytes]와 같습니다.
 *
 * @throws IllegalArgumentException [maxBytes]가 음수인 경우
 * @throws ByteLimitExceededException 본문이 [maxBytes]를 초과한 경우
 * @throws IOException body 획득, 읽기 또는 닫기에 실패한 경우
 */
fun HttpResponse<InputStream>.readBodyString(
    maxBytes: Int,
    charset: Charset = Charsets.UTF_8,
): String = readBodyBytes(maxBytes).toString(charset)

private fun HttpHeaders.strictContentLengthOrNull(): Long? {
    val value = allValues("Content-Length").singleOrNull() ?: return null
    if (value.isEmpty() || value != value.trim()) return null
    return value.toLongOrNull()?.takeIf { it >= 0L }
}
