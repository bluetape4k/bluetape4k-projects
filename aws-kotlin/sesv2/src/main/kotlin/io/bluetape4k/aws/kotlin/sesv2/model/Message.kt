package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.Body
import aws.sdk.kotlin.services.sesv2.model.Content
import aws.sdk.kotlin.services.sesv2.model.Message

fun contentOf(
    data: String,
    charset: String = Charsets.UTF_8.name(),
    @BuilderInference builder: Content.Builder.() -> Unit = {},
): Content {
    require(data.isNotBlank()) { "data must not be blank." }

    return Content {
        this.data = data
        this.charset = charset

        builder()
    }
}

fun htmlBodyOf(
    html: Content? = null,
    @BuilderInference builder: Body.Builder.() -> Unit = {},
): Body =
    Body {
        html?.let { this.html = it }
        builder()
    }

fun textBodyOf(
    text: Content? = null,
    @BuilderInference builder: Body.Builder.() -> Unit = {},
): Body =
    Body {
        text?.let { this.text = it }
        builder()
    }

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
