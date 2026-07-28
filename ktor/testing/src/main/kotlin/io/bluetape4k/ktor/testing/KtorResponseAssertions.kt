package io.bluetape4k.ktor.testing

import io.bluetape4k.assertions.fail
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.ktor.core.ApiErrorResponse
import io.bluetape4k.ktor.core.Bluetape4kKtorJson
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * HTTP status를 검증하고 chaining할 수 있도록 같은 response를 반환합니다.
 */
infix fun HttpResponse.shouldHaveStatus(expected: HttpStatusCode): HttpResponse {
    status shouldBeEqualTo expected
    return this
}

/**
 * Decodes the response body as JSON using bluetape4k Ktor defaults.
 *
 * ## Contract
 * - Decode failures are converted to assertion failures with the raw body.
 * - The response body is consumed once, matching Ktor client semantics.
 */
suspend inline fun <reified T> HttpResponse.decodeJsonBody(
    jsonFormat: Json = Bluetape4kKtorJson.defaultJson(),
): T {
    val rawBody = bodyAsText()
    return try {
        jsonFormat.decodeFromString(rawBody)
    } catch (e: SerializationException) {
        fail(
            message = "Expected response body to decode as ${T::class.qualifiedName}, but body was: $rawBody",
            cause = e
        )
    }
}

/**
 * Decodes the response body and verifies structural equality.
 */
suspend inline fun <reified T> HttpResponse.shouldHaveJsonBody(
    expected: T,
    jsonFormat: Json = Bluetape4kKtorJson.defaultJson(),
): T {
    val actual = decodeJsonBody<T>(jsonFormat)
    actual shouldBeEqualTo expected
    return actual
}

/**
 * 표준 bluetape4k [ApiErrorResponse]를 검증합니다.
 */
suspend fun HttpResponse.shouldHaveApiError(
    expected: ExpectedApiError,
    jsonFormat: Json = Bluetape4kKtorJson.defaultJson(),
): ApiErrorResponse {
    shouldHaveStatus(expected.status)

    val actual = decodeJsonBody<ApiErrorResponse>(jsonFormat)
    actual.status shouldBeEqualTo expected.status.value
    actual.error shouldBeEqualTo expected.error
    expected.message?.let { actual.message shouldBeEqualTo it }
    expected.path?.let { actual.path shouldBeEqualTo it }
    return actual
}
