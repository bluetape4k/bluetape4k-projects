package io.bluetape4k.http.ahc

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.asynchttpclient.filter.FilterContext
import org.asynchttpclient.filter.RequestFilter

/**
 * 모든 요청에 고정 헤더를 추가하는 [RequestFilter]입니다.
 *
 * ```kotlin
 * val filter = AttachHeaderRequestFilter(
 *     mapOf("Authorization" to "Bearer token", "X-App-Version" to "1.0")
 * )
 * val client = asyncHttpClientOf(filter)
 * val response = client.prepareGet("https://api.example.com").execute().get()
 * // response.statusCode == 200
 * ```
 *
 * @param headers 추가할 헤더 이름/값 쌍
 * @see attachHeaderRequestFilterOf
 */
class AttachHeaderRequestFilter(val headers: Map<String, Any?>): RequestFilter {

    companion object: KLogging()

    /** 설정한 헤더를 현재 요청에 추가합니다. */
    override fun <T> filter(ctx: FilterContext<T>): FilterContext<T> {
        this.headers.forEach { (name, value) ->
            log.trace { "Add header name=$name, value=$value" }
            ctx.request.headers.add(name, value)
        }
        return ctx
    }
}
