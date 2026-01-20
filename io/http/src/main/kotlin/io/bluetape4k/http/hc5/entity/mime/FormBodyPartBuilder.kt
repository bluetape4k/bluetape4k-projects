package io.bluetape4k.http.hc5.entity.mime

import org.apache.hc.client5.http.entity.mime.ContentBody
import org.apache.hc.client5.http.entity.mime.FormBodyPart
import org.apache.hc.client5.http.entity.mime.FormBodyPartBuilder

/**
 * [FormBodyPart] 를 생성합니다.
 *
 * ```
 * val formBodyPart = formBodyPart {
 *    setName("file")
 *    setBody(FileBody(file))
 *    addField("name", "file")
 *    addField("filename", file.name)
 * }
 * ```
 *
 * @param builder 초기화 람다
 * @return [FormBodyPart]
 */
inline fun formBodyPart(
    @BuilderInference builder: FormBodyPartBuilder.() -> Unit,
): FormBodyPart =
    FormBodyPartBuilder.create().apply(builder).build()

/**
 * [FormBodyPart] 를 생성합니다.
 *
 * ```
 * val formBodyPart = formBodyPart("file", FileBody(file)) {
 *   addField("name", "file")
 *   addField("filename", file.name)
 *   setField("Content-Type", "application/octet-stream")
 * }
 * ```
 *
 * @param name 파트 이름
 * @param body 파트 바디
 * @param builder 초기화 람다
 * @return [FormBodyPart]
 */
inline fun formBodyPart(
    name: String,
    body: ContentBody,
    @BuilderInference builder: FormBodyPartBuilder.() -> Unit = {},
): FormBodyPart =
    formBodyPart {
        setName(name)
        setBody(body)
        builder()
    }

/**
 * [FormBodyPart] 를 생성합니다.
 *
 * ```
 * val formBodyPart = formBodyPartOf("file", FileBody(file), mapOf(
 *   "name" to "file",
 *   "filename" to file.name,
 * )) {
 *   setField("Content-Type", "application/octet-stream")
 * }
 * ```
 *
 * @param name 파트 이름
 * @param body 파트 바디
 * @param fields 파트 필드
 * @param builder 초기화 람다
 * @return [FormBodyPart]
 */
inline fun formBodyPartOf(
    name: String,
    body: ContentBody,
    fields: Map<String, String>,
    @BuilderInference builder: FormBodyPartBuilder.() -> Unit = {},
): FormBodyPart =
    formBodyPart {
        setName(name)
        setBody(body)
        fields.forEach { (name, value) ->
            addField(name, value)
        }
        builder()
    }
