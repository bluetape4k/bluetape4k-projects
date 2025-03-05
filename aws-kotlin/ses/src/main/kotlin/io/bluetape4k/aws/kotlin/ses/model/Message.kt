package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.Body
import aws.sdk.kotlin.services.ses.model.Content
import aws.sdk.kotlin.services.ses.model.Message

inline fun contentOf(
    data: String,
    charset: String = Charsets.UTF_8.name(),
    crossinline configurer: Content.Builder.() -> Unit = {},
): Content {
    return Content {
        this.data = data
        this.charset = charset

        configurer()
    }
}

inline fun htmlBodyOf(
    html: Content? = null,
    crossinline configurer: Body.Builder.() -> Unit = {},
): Body {
    return Body {
        html?.let { this.html = it }
        configurer()
    }
}

inline fun textBodyOf(
    text: Content? = null,
    crossinline configurer: Body.Builder.() -> Unit = {},
): Body {
    return Body {
        text?.let { this.text = it }
        configurer()
    }
}


inline fun messageOf(
    subject: Content,
    body: Body,
    crossinline configurer: Message.Builder.() -> Unit = {},
): Message {
    return Message {
        this.subject = subject
        this.body = body

        configurer()
    }
}
