package io.bluetape4k.http.hc5.entity

import io.bluetape4k.http.hc5.http.ContentTypes
import io.bluetape4k.support.ifTrue
import org.apache.hc.client5.http.entity.EntityBuilder
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.NameValuePair
import java.io.File
import java.io.InputStream
import java.io.Serializable

/**
 * Builder for [HttpEntity] instances.
 *
 * Several setter methods of this builder are mutually exclusive. In case of multiple invocations
 * of the following methods only the last one will have effect:
 *
 * - [setText(String)]
 * - [setBinary(ByteArray)]
 * - [setStream(java.io.InputStream)]
 * - [setSerializable(java.io.Serializable)]
 * - [setParameters(java.util.List)]
 * - [setParameters(NameValuePair...)]
 * - [setFile(java.io.File)]
 *
 * ```
 * val entity = httpEntity {
 *    setText("Hello, World!")
 *    setContentType(ContentTypes.TEXT_PLAIN_UTF8)
 *    setContentEncoding("UTF-8")
 *    gzipCompressed()
 * }
 * ```
 *
 * @param builder  [EntityBuilder]를 이용한 초기화 코드
 * @return [HttpEntity] 인스턴스
 */
inline fun httpEntity(
    @BuilderInference builder: EntityBuilder.() -> Unit,
): HttpEntity =
    EntityBuilder.create().apply(builder).build()

/**
 * [HttpEntity]를 생성합니다.
 *
 * ```
 * val entity = httpEntityOf("Hello, World!") {
 *      setParameters(NameValuePair("key", "value"))
 * }
 * ```
 *
 * @param text 텍스트
 * @param contentType 컨텐츠 타입
 * @param contentEncoding 컨텐츠 인코딩
 * @param gzipCompressed GZIP 압축 여부
 * @param builder [EntityBuilder]를 이용한 초기화 코드
 * @return [HttpEntity] 인스턴스
 */
inline fun httpEntityOf(
    text: String? = null,
    contentType: ContentType = ContentTypes.TEXT_PLAIN_UTF8,
    contentEncoding: String? = null,
    gzipCompressed: Boolean? = null,
    @BuilderInference builder: EntityBuilder.() -> Unit = {},
): HttpEntity =
    httpEntity {
        setText(text)
        setContentType(contentType)
        contentEncoding?.run { setContentEncoding(this) }
        gzipCompressed?.ifTrue { gzipCompressed() }

        builder()
    }

/**
 * [HttpEntity]를 생성합니다.
 *
 * ```
 * val entity = httpEntityOf(byteArrayOf(1, 2, 3, 4)) {
 *    setParameters(NameValuePair("key", "value"))
 * }
 * ```
 *
 * @param binary 바이너리
 * @param contentType 컨텐츠 타입
 * @param contentEncoding 컨텐츠 인코딩
 * @param gzipCompressed GZIP 압축 여부
 * @param builder [EntityBuilder]를 이용한 초기화 코드
 * @return [HttpEntity] 인스턴스
 */
inline fun httpEntityOf(
    binary: ByteArray,
    contentType: ContentType = ContentType.DEFAULT_BINARY,
    contentEncoding: String? = null,
    gzipCompressed: Boolean? = null,
    @BuilderInference builder: EntityBuilder.() -> Unit = {},
): HttpEntity =
    httpEntity {
        setBinary(binary)
        setContentType(contentType)
        contentEncoding?.run { setContentEncoding(this) }
        gzipCompressed?.ifTrue { gzipCompressed() }

        builder()
    }

/**
 * [HttpEntity]를 생성합니다.
 *
 * ```
 * val entity = httpEntityOf(inputStream) {
 *   setParameters(NameValuePair("key", "value"))
 * }
 * ```
 *
 * @param inputStream 스트림
 * @param contentType 컨텐츠 타입
 * @param contentEncoding 컨텐츠 인코딩
 * @param gzipCompressed GZIP 압축 여부
 * @param builder [EntityBuilder]를 이용한 초기화 코드
 * @return [HttpEntity] 인스턴스
 */
inline fun httpEntityOf(
    inputStream: InputStream,
    contentType: ContentType = ContentType.DEFAULT_BINARY,
    contentEncoding: String? = null,
    gzipCompressed: Boolean? = null,
    @BuilderInference builder: EntityBuilder.() -> Unit = {},
): HttpEntity =
    httpEntity {
        setStream(inputStream)
        setContentType(contentType)
        contentEncoding?.run { setContentEncoding(this) }
        gzipCompressed?.ifTrue { gzipCompressed() }

        builder()
    }

/**
 * [HttpEntity]를 생성합니다.
 *
 * ```
 * val entity = httpEntityOf(file) {
 *   setParameters(NameValuePair("key", "value"))
 * }
 * ```
 *
 * @param file 파일
 * @param contentType 컨텐츠 타입
 * @param contentEncoding 컨텐츠 인코딩
 * @param gzipCompressed GZIP 압축 여부
 * @param builder [EntityBuilder]를 이용한 초기화 코드
 * @return [HttpEntity] 인스턴스
 */
inline fun httpEntityOf(
    file: File,
    contentType: ContentType = ContentType.DEFAULT_BINARY,
    contentEncoding: String? = null,
    gzipCompressed: Boolean? = null,
    @BuilderInference builder: EntityBuilder.() -> Unit = {},
): HttpEntity =
    httpEntity {
        setFile(file)
        setContentType(contentType)
        contentEncoding?.run { setContentEncoding(this) }
        gzipCompressed?.ifTrue { gzipCompressed() }

        builder()
    }

/**
 * [HttpEntity]를 생성합니다.
 *
 * ```
 * val entity = httpEntityOf(serializable) {
 *   setParameters(NameValuePair("key", "value"))
 *   setContentType(ContentTypes.APPLICATION_JAVA_OBJECT)
 * }
 * ```
 * @param serializable 직렬화 가능한 객체
 * @param contentType 컨텐츠 타입
 * @param contentEncoding 컨텐츠 인코딩
 * @param gzipCompressed GZIP 압축 여부
 * @param builder [EntityBuilder]를 이용한 초기화 코드
 * @return [HttpEntity] 인스턴스
 */
inline fun httpEntityOf(
    serializable: Serializable,
    contentType: ContentType = ContentType.DEFAULT_BINARY,
    contentEncoding: String? = null,
    gzipCompressed: Boolean? = null,
    @BuilderInference builder: EntityBuilder.() -> Unit = {},
): HttpEntity =
    httpEntity {
        setSerializable(serializable)
        setContentType(contentType)
        contentEncoding?.run { setContentEncoding(this) }
        gzipCompressed?.ifTrue { gzipCompressed() }

        builder()
    }

/**
 * [HttpEntity]를 생성합니다.
 *
 * ```
 * val entity = httpEntityOf(parameters) {
 *   setContentType(ContentTypes.APPLICATION_FORM_URLENCODED)
 * }
 * ```
 *
 * @param parameters 파라미터
 * @param contentType 컨텐츠 타입
 * @param contentEncoding 컨텐츠 인코딩
 * @param gzipCompressed GZIP 압축 여부
 * @param builder [EntityBuilder]를 이용한 초기화 코드
 * @return [HttpEntity] 인스턴스
 */
inline fun httpEntityOf(
    parameters: List<NameValuePair>,
    contentType: ContentType = ContentTypes.TEXT_PLAIN_UTF8,
    contentEncoding: String? = null,
    gzipCompressed: Boolean? = null,
    @BuilderInference builder: EntityBuilder.() -> Unit = {},
): HttpEntity =
    httpEntity {
        setParameters(parameters)
        setContentType(contentType)
        contentEncoding?.run { setContentEncoding(this) }
        gzipCompressed?.ifTrue { gzipCompressed() }

        builder()
    }
