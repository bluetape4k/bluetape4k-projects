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
 * 하나의 JSON response를 반환하는 Ktor [MockEngine]을 생성합니다.
 *
 * ## 계약
 * - [jsonFormat]을 지정하지 않으면 response JSON은 bluetape4k Ktor 기본값을 사용합니다.
 * - `Content-Type: application/json`은 기본으로 추가됩니다.
 * - 이 helper는 의도적으로 작게 유지합니다. request-dependent 또는 multi-step scenario에는
 *   Ktor [MockEngine]을 직접 사용하십시오.
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
