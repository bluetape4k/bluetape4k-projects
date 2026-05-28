package io.bluetape4k.ktor.testing

import io.bluetape4k.ktor.core.Bluetape4kKtorJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Creates a Ktor [MockEngine] that returns one JSON response.
 *
 * ## Contract
 * - Response JSON uses bluetape4k Ktor defaults unless [jsonFormat] is supplied.
 * - `Content-Type: application/json` is added by default.
 * - The helper stays intentionally small; use Ktor's [MockEngine] directly for
 *   request-dependent or multi-step scenarios.
 */
inline fun <reified T> bluetape4kJsonMockEngine(
    body: T,
    status: HttpStatusCode = HttpStatusCode.OK,
    headers: Headers = Headers.Empty,
    jsonFormat: Json = Bluetape4kKtorJson.defaultJson(),
): MockEngine =
    MockEngine {
        respond(
            content = jsonFormat.encodeToString(body),
            status = status,
            headers = HeadersBuilder()
                .apply {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    appendAll(headers)
                }
                .build()
        )
    }
