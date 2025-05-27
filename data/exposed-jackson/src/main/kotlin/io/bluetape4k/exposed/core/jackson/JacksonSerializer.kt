package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.jackson.JacksonSerializer

/**
 * Default [JacksonSerializer] instance.
 */
val DefaultJacksonSerializer: JacksonSerializer by lazy { JacksonSerializer() }
