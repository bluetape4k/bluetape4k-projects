package io.bluetape4k.http.hc5.entity.mime

import org.apache.hc.client5.http.entity.mime.ContentBody
import org.apache.hc.client5.http.entity.mime.MimeField
import org.apache.hc.client5.http.entity.mime.MultipartPart
import org.apache.hc.client5.http.entity.mime.MultipartPartBuilder

/**
 * MultipartPart 를 생성합니다.
 *
 * ```
 * val part = multipartPart {
 *     setBody(StringBody("Hello, World!", ContentType.TEXT_PLAIN))
 *     addHeader("Content-Disposition", "form-data; name=\"file\"; filename=\"hello.txt\"")
 * }
 * ```
 *
 * @param initializer [MultipartPartBuilder] 초기화 람다
 * @return [MultipartPart]
 */
inline fun multipartPart(initializer: MultipartPartBuilder.() -> Unit): MultipartPart {
    return MultipartPartBuilder.create().apply(initializer).build()
}

/**
 * MultipartPart 를 생성합니다.
 *
 * ```
 * val part = multipartPart(
 *     StringBody("Hello, World!", ContentType.TEXT_PLAIN)
 * ) {
 *     addHeader("Content-Disposition", "form-data; name=\"file\"; filename=\"hello.txt\"")
 * }
 * ```
 *
 * @param body [ContentBody]
 * @param initializer [MultipartPartBuilder] 초기화 람다
 * @return [MultipartPart]
 */
inline fun multipartPart(
    body: ContentBody,
    initializer: MultipartPartBuilder.() -> Unit = {},
): MultipartPart = multipartPart {
    setBody(body)
    initializer()
}

/**
 * MultipartPart 를 생성합니다.
 *
 * ```
 * val part = multipartPartOf(
 *     StringBody("Hello, World!", ContentType.TEXT_PLAIN),
 *     MimeField("Content-Disposition", "form-data; name=\"file\"; filename=\"hello.txt\"")
 * )
 * ```
 *
 * @param body [ContentBody]
 * @param mimeFields [MimeField]
 * @param initializer [MultipartPartBuilder] 초기화 람다
 * @return [MultipartPart]
 */
fun multipartPartOf(
    body: ContentBody,
    vararg mimeFields: MimeField,
    initializer: MultipartPartBuilder.() -> Unit = {},
): MultipartPart = multipartPart {
    setBody(body)
    mimeFields.forEach { field ->
        addHeader(field.name, field.value, field.parameters)
    }
    initializer()
}

/**
 * MultipartPart 를 생성합니다.
 *
 * ```
 * val part = multipartPartOf(
 *     StringBody("Hello, World!", ContentType.TEXT_PLAIN),
 *     mapOf("Content-Disposition" to "form-data; name=\"file\"; filename=\"hello.txt\"")
 * )
 * ```
 *
 * @param body [ContentBody]
 * @param fields [Map] 필드 정보
 * @param initializer [MultipartPartBuilder] 초기화 람다
 * @return [MultipartPart]
 */
fun multipartPartOf(
    body: ContentBody,
    fields: Map<String, String> = emptyMap(),
    initializer: MultipartPartBuilder.() -> Unit = {},
): MultipartPart = multipartPart {
    setBody(body)
    fields.forEach { field ->
        addHeader(field.key, field.value)
    }
    initializer()
}
