package io.bluetape4k.http.hc5.entity

import io.bluetape4k.http.readOwnedBodyBytes
import io.bluetape4k.io.ByteLimitExceededException
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.apache.hc.core5.http.HttpEntity
import java.io.IOException
import java.nio.charset.Charset

/**
 * HC5 entity 본문을 [maxBytes] 이하일 때만 전체 [ByteArray]로 반환합니다.
 *
 * null entity는 빈 배열로 정규화합니다. null을 보존해야 하면 `entity?.let { ... }`을 사용합니다.
 * `Content-Length`는 조기 거부용 힌트일 뿐이며 실제 stream에도 같은 상한을 적용합니다.
 * 획득한 content stream은 성공과 실패 모두에서 정확히 한 번 닫지만 상위 response는 닫지 않습니다.
 * 이 호출은 blocking될 수 있으므로 client timeout과 상위 response 중단은 호출자가 구성해야 합니다.
 * prefix preview가 필요하면 기존 `toByteArrayOrNull`을 사용하고, 완전한 본문만 허용할 때 이 함수를 사용합니다.
 *
 * @throws IllegalArgumentException [maxBytes]가 음수인 경우
 * @throws ByteLimitExceededException 본문이 [maxBytes]를 초과한 경우
 * @throws IOException content 획득, 읽기 또는 닫기에 실패한 경우
 */
fun HttpEntity?.readBodyBytes(maxBytes: Int): ByteArray {
    maxBytes.requireZeroOrPositiveNumber("maxBytes")
    if (this == null) return byteArrayOf()

    val declaredLength = contentLength
    return readOwnedBodyBytes(
        maxBytes = maxBytes,
        knownOversize = declaredLength >= 0L && declaredLength > maxBytes.toLong(),
        acquireBody = { content },
    )
}

/**
 * HC5 entity 본문을 byte 상한 안에서 읽고 [charset]으로 변환합니다.
 *
 * 상한은 변환된 문자 수가 아니라 원본 stream byte 수에 적용됩니다. stream과 상위 response의
 * 소유권, blocking 및 strict/truncation 선택은 [readBodyBytes]와 같습니다.
 *
 * @throws IllegalArgumentException [maxBytes]가 음수인 경우
 * @throws ByteLimitExceededException 본문이 [maxBytes]를 초과한 경우
 * @throws IOException content 획득, 읽기 또는 닫기에 실패한 경우
 */
fun HttpEntity?.readBodyString(
    maxBytes: Int,
    charset: Charset = Charsets.UTF_8,
): String = readBodyBytes(maxBytes).toString(charset)
