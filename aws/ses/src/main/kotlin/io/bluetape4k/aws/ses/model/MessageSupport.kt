package io.bluetape4k.aws.ses.model

import software.amazon.awssdk.services.ses.model.Body
import software.amazon.awssdk.services.ses.model.Content
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.MessageTag
import java.nio.charset.Charset

/**
 * [Message.Builder]를 사용하여 [Message] 인스턴스를 생성합니다.
 *
 * ```
 * val message = Message {
 *    subject {
 *        data("Hello")
 *        charset("UTF-8")
 *        ...
 *    }
 *    body {
 *        text {
 *            data("Hello")
 *        }
 *        html {
 *            data("<p>Hello</p>")
 *        }
 *    }
 * }
 * ```
 *
 * @param initializer [Message.Builder] 초기화 람다
 * @return [Message] 인스턴스
 */
inline fun Message(initializer: Message.Builder.() -> Unit): Message {
    return Message.builder().apply(initializer).build()
}

/**
 * [Message] 인스턴스를 생성합니다.
 *
 * ```
 * val message = messageOf(
 *     subject = contentOf("Hello", Charsets.UTF_8),
 *     body = bodyOf("Hello", "<p>Hello</p>", Charsets.UTF_8)
 * )
 * ```
 *
 * @param subject [Content] 제목
 * @param body [Body] 본문
 * @return [Message] 인스턴스
 */
fun messageOf(subject: Content, body: Body): Message {
    return Message { subject(subject).body(body) }
}

/**
 * [Body.Builder]를 이용하여 [Body] 인스턴스를 생성합니다.
 *
 * ```
 * val body = Body {
 *    text {
 *        contentOf("Hello", Charsets.UTF_8)
 *    }
 *    html {
 *        contentOf("<p>Hello</p>", Charsets.UTF_8)
 *    }
 * }
 * ```
 *
 * @param initializer [Body.Builder] 초기화 람다
 * @return [Body] 인스턴스
 */
fun Body(initializer: Body.Builder.() -> Unit): Body {
    return Body.builder().apply(initializer).build()
}

/**
 * [Body] 인스턴스를 생성합니다.
 *
 * ```
 * val body = bodyOf("Hello", "<p>Hello</p>", Charsets.UTF_8)
 * ```
 *
 * @param text [String] 텍스트 본문
 * @param html [String] HTML 본문
 * @param charset [Charset] 문자셋
 * @return [Body] 인스턴스
 */
fun bodyOf(text: String, html: String, charset: Charset = Charsets.UTF_8): Body = Body {
    text(contentOf(text, charset))
    html(contentOf(html, charset))
}

/**
 * Text [Body] 인스턴스를 생성합니다.
 *
 * ```
 * val body = bodyOf("Hello", Charsets.UTF_8)
 * ```
 *
 * @param text [String] 텍스트 본문
 * @param charset [Charset] 문자셋
 * @return [Body] 인스턴스
 */
fun bodyAsText(text: String, charset: Charset = Charsets.UTF_8): Body = Body {
    text(contentOf(text, charset))
}

/**
 * HTML [Body] 인스턴스를 생성합니다.
 *
 * ```
 * val body = bodyOf("<p>Hello</p>", Charsets.UTF_8)
 * ```
 *
 * @param html [String] HTML 본문
 * @param charset [Charset] 문자셋
 * @return [Body] 인스턴스
 */
fun bodyAsHtml(html: String, charset: Charset = Charsets.UTF_8): Body = Body {
    html(contentOf(html, charset))
}

/**
 * [Content.Builder]를 이용하여 [Content] 인스턴스를 생성합니다.
 *
 * ```
 * val content = Content {
 *    data("Hello")
 *    charset("UTF-8")
 * }
 * ```
 *
 * @param initializer [Content.Builder] 초기화 람다
 * @return [Content] 인스턴스
 */
inline fun Content(initializer: Content.Builder.() -> Unit): Content {
    return Content.builder().apply(initializer).build()
}

/**
 * [Content] 인스턴스를 생성합니다.
 *
 * ```
 * val content = contentOf("Hello", Charsets.UTF_8)
 * ```
 *
 * @param data [String] 데이터
 * @param charset [Charset] 문자셋
 * @return [Content] 인스턴스
 */
fun contentOf(data: String? = null, charset: Charset = Charsets.UTF_8) = Content {
    data(data)
    charset(charset.name())
}

/**
 * [MessageTag.Builder]를 이용하여 [MessageTag] 인스턴스를 생성합니다.
 *
 * ```
 * val messageTag = MessageTag {
 *    name("key")
 *    value("value")
 * }
 * ```
 *
 * @param initializer [MessageTag.Builder] 초기화 람다
 * @return [MessageTag] 인스턴스
 */
inline fun MessageTag(initializer: MessageTag.Builder.() -> Unit): MessageTag {
    return MessageTag.builder().apply(initializer).build()
}

/**
 * [MessageTag] 인스턴스를 생성합니다.
 *
 * ```
 * val messageTag = messageTagOf("key", "value")
 * ```
 *
 * @param name [String] 태그 이름
 * @param value [String] 태그 값
 * @return [MessageTag] 인스턴스
 */
fun messageTagOf(name: String, value: String) = MessageTag {
    name(name)
    value(value)
}
