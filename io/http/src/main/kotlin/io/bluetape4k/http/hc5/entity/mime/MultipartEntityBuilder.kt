package io.bluetape4k.http.hc5.entity.mime

import org.apache.hc.client5.http.entity.mime.HttpMultipartMode
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.message.BasicNameValuePair
import java.nio.charset.Charset

/**
 * Multi-part 포맷의 [HttpEntity]를 빌드합니다.
 *
 * ```
 * val multipartEntity = multipartEntity {
 *    addTextBody("name", "value")
 *    addBinaryBody("file", file)
 *    addPart(formBodyPart)
 * }
 * ```
 *
 * @param initializer 초기화 람다
 * @return [HttpEntity]
 */
inline fun multipartEntity(initializer: MultipartEntityBuilder.() -> Unit): HttpEntity =
    MultipartEntityBuilder.create().apply(initializer).build()

/**
 * Multi-part 포맷의 [HttpEntity]를 빌드합니다.
 *
 * ```
 * val multipartEntity = multipartEntity(
 *    mode = HttpMultipartMode.BROWSER_COMPATIBLE,
 *    charset = Charsets.UTF_8,
 *    boundary = "boundary",
 *    subType = "form-data",
 *    contentType = ContentType.MULTIPART_FORM_DATA,
 *    parameters = listOf(
 *      BasicNameValuePair("name", "value"),
 *    ),
 * ) {
 *    addTextBody("name", "value")
 *    addBinaryBody("file", file)
 *    addPart(formBodyPart)
 * }
 * ```
 *
 * @param mode [HttpMultipartMode] (기본값: STRICT)
 * @param charset [Charset] (기본값: UTF-8)
 * @param boundary boundary 문자열
 * @param subType 서브 타입
 * @param contentType [ContentType]
 * @param parameters [BasicNameValuePair] 컬렉션
 * @param initializer 초기화 람다
 * @return [HttpEntity] 인스턴스
 */
inline fun multipartEntity(
    mode: HttpMultipartMode = HttpMultipartMode.STRICT,
    charset: Charset = Charsets.UTF_8,
    boundary: String? = null,
    subType: String? = null,
    contentType: ContentType? = null,
    parameters: Collection<BasicNameValuePair> = emptyList(),
    initializer: MultipartEntityBuilder.() -> Unit = {},
): HttpEntity = multipartEntity {
    setMode(mode)
    setCharset(charset)

    boundary?.run { setBoundary(boundary) }
    subType?.run { setMimeSubtype(subType) }
    contentType?.run { setContentType(contentType) }
    parameters.forEach {
        addParameter(it)
    }

    initializer()
}
