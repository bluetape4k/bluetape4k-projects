package io.bluetape4k.ktor.core

import kotlinx.serialization.json.Json

/**
 * JSON defaults shared by bluetape4k Ktor modules.
 *
 * ## Contract
 * - Unknown fields are ignored to keep clients forward-compatible.
 * - Default values are encoded so small health/error DTOs remain explicit.
 * - Null properties are omitted from responses.
 *
 * ```kotlin
 * install(ContentNegotiation) {
 *     json(Bluetape4kKtorJson.defaultJson())
 * }
 * ```
 */
object Bluetape4kKtorJson {

    fun defaultJson(): Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
}
