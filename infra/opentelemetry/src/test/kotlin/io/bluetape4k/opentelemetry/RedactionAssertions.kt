package io.bluetape4k.opentelemetry

import io.bluetape4k.assertions.shouldNotContain
import io.opentelemetry.sdk.trace.data.SpanData

internal infix fun SpanData.shouldNotExpose(secret: String) {
    val exported = buildString {
        append(status.description)
        attributes.asMap().forEach { (key, value) ->
            append('|').append(key.key).append('=').append(value)
        }
        events.forEach { event ->
            append('|').append(event.name)
            event.attributes.asMap().forEach { (key, value) ->
                append('|').append(key.key).append('=').append(value)
            }
        }
    }

    exported shouldNotContain secret
}
