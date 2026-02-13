package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.Body
import aws.sdk.kotlin.services.ses.model.Content
import aws.sdk.kotlin.services.ses.model.Message
import io.bluetape4k.support.requireNotBlank

inline fun contentOf(
    data: String,
    charset: String = Charsets.UTF_8.name(),
    @BuilderInference crossinline builder: Content.Builder.() -> Unit = {},
): Content {
    data.requireNotBlank("data")

    return Content {
        this.data = data
        this.charset = charset

        builder()
    }
}

inline fun htmlBodyOf(
    html: Content? = null,
    @BuilderInference crossinline builder: Body.Builder.() -> Unit = {},
): Body = Body {
    this.html = html
    builder()
}

inline fun textBodyOf(
    text: Content? = null,
    @BuilderInference crossinline builder: Body.Builder.() -> Unit = {},
): Body = Body {
    this.text = text
    builder()
}

inline fun messageOf(
    subject: Content,
    body: Body,
    @BuilderInference crossinline builder: Message.Builder.() -> Unit = {},
): Message = Message {
    this.subject = subject
    this.body = body

    builder()
}
