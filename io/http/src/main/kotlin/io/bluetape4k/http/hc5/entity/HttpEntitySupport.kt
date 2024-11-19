package io.bluetape4k.http.hc5.entity

import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.nio.charset.Charset

/**
 * [HttpEntity]의 내용을 완전히 소비되고, 콘텐츠 스트림이 존재하는 경우 닫습니다.
 *
 * 이 과정은 **조용히** 수행되며, IOException을 throw하지 않습니다.
 *
 * ```
 * val response = httpClient.execute(httpGet)
 * response.entity.consumeQuietly()
 * ```
 */
fun HttpEntity?.consumeQuietly() {
    this?.run { EntityUtils.consumeQuietly(this) }
}

/**
 * 엔티티 컨텐츠가 모두 소비되고 컨텐츠 스트림이 존재하는 경우 닫습니다.
 *
 * ```
 * val response = httpClient.execute(httpGet)
 * response.entity.consume()
 * ```
 */
fun HttpEntity?.consume() {
    this?.run { EntityUtils.consume(this) }
}

/**
 * 엔티티의 내용을 읽어 ByteArray 로 반환합니다.
 *
 * ```
 * val response = httpClient.execute(httpGet)
 * val content = response.entity.toByteArrayOrNull()
 * ```
 *
 * @param maxResultLength 반환할 ByteArray의 최대 크기; 이를 사용하여 무리한 또는 악의적인 처리에 대비합니다.
 * @return ByteArray 또는 null
 */
fun HttpEntity.toByteArrayOrNull(maxResultLength: Int = Int.MAX_VALUE): ByteArray? {
    return EntityUtils.toByteArray(this, maxResultLength)
}

/**
 * 엔티티 컨텐츠를 문자열로 반환합니다. 문자셋이 없는 경우 제공된 기본 문자셋 ("UTF-8") 을 사용합니다.
 *
 * ```
 * val response = httpClient.execute(httpGet)
 * val content = response.entity.toStringOrNull()
 * ```
 *
 * @param charset 문자셋 (기본값: UTF-8)
 * @param maxResultLength 반환할 문자열의 최대 크기; 이를 사용하여 무리한 또는 악의적인 처리에 대비합니다.
 * @return 문자열 또는 null
 */
fun HttpEntity.toStringOrNull(
    charset: Charset = Charsets.UTF_8,
    maxResultLength: Int = Int.MAX_VALUE,
): String? {
    return EntityUtils.toString(this, charset, maxResultLength)
}

/**
 * [HttpEntity]의 컨텐츠를 읽어 [NameValuePair]의 리스트로 반환합니다.
 *
 * 인코딩은 엔티티의 `Content-Encoding` 헤더에서 가져옵니다.
 *
 * ```
 * val response = httpClient.execute(httpGet)
 * val params = response.entity.parse()
 * ```
 *
 * @param maxResultLength 반환할 문자열의 최대 크기; 이를 사용하여 무리한 또는 악의적인 처리에 대비합니다.
 * @return [NameValuePair]의 리스트
 */
fun HttpEntity.parse(maxResultLength: Int = Int.MAX_VALUE): List<NameValuePair> =
    EntityUtils.parse(this, maxResultLength)
