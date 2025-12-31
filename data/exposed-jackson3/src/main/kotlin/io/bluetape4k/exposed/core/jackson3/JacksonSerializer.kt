package io.bluetape4k.exposed.core.jackson3

import io.bluetape4k.jackson3.JacksonSerializer

/**
 * Default [JacksonSerializer] instance.
 */
val DefaultJacksonSerializer: JacksonSerializer by lazy { JacksonSerializer() }
