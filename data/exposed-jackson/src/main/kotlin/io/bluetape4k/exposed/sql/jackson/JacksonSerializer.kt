package io.bluetape4k.exposed.sql.jackson

import io.bluetape4k.jackson.JacksonSerializer

/**
 * Default [JacksonSerializer] instance.
 */
val DefaultJacksonSerializer: JacksonSerializer by lazy { JacksonSerializer() }
