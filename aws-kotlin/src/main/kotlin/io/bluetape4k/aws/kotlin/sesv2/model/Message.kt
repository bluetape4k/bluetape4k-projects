package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.Body
import aws.sdk.kotlin.services.sesv2.model.Content
import aws.sdk.kotlin.services.sesv2.model.Message
import io.bluetape4k.support.requireNotBlank

/**
 * 이메일 본문 [Content]를 생성합니다.
 *
 * ```kotlin
 * val subject = contentOf("Hello, World!")
 * ```
 *
 * @param data 본문 문자열 (비어 있으면 안 됨)
 * @param charset 문자 인코딩 (기본값: UTF-8)
 * @return [Content] 인스턴스
 */
fun contentOf(
    data: String,
    charset: String = Charsets.UTF_8.name(),
    @BuilderInference builder: Content.Builder.() -> Unit = {},
): Content {
    data.requireNotBlank("data")

    return Content {
        this.data = data
        this.charset = charset

        builder()
    }
}

/**
 * HTML 본문 [Body]를 생성합니다.
 *
 * ```kotlin
 * val body = htmlBodyOf(contentOf("<h1>Hello</h1>"))
 * ```
 *
 * @param html HTML [Content] (null이면 생략)
 * @return [Body] 인스턴스
 */
fun htmlBodyOf(
    html: Content? = null,
    @BuilderInference builder: Body.Builder.() -> Unit = {},
): Body =
    Body {
        html?.let { this.html = it }
        builder()
    }

/**
 * 텍스트 본문 [Body]를 생성합니다.
 *
 * ```kotlin
 * val body = textBodyOf(contentOf("Hello, World!"))
 * ```
 *
 * @param text 텍스트 [Content] (null이면 생략)
 * @return [Body] 인스턴스
 */
fun textBodyOf(
    text: Content? = null,
    @BuilderInference builder: Body.Builder.() -> Unit = {},
): Body =
    Body {
        text?.let { this.text = it }
        builder()
    }

/**
 * 제목과 본문으로 이메일 [Message]를 생성합니다.
 *
 * ```kotlin
 * val message = messageOf(
 *     subject = contentOf("Hello"),
 *     body = textBodyOf(contentOf("Hello, World!")),
 * )
 * ```
 *
 * @param subject 이메일 제목 [Content]
 * @param body 이메일 본문 [Body]
 * @return [Message] 인스턴스
 */
fun messageOf(
    subject: Content,
    body: Body,
    @BuilderInference builder: Message.Builder.() -> Unit = {},
): Message =
    Message {
        this.subject = subject
        this.body = body

        builder()
    }
