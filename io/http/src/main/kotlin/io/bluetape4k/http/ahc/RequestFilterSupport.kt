package io.bluetape4k.http.ahc

import org.asynchttpclient.filter.FilterContext
import org.asynchttpclient.filter.FilterContext.FilterContextBuilder
import org.asynchttpclient.filter.RequestFilter

/**
 * [FilterContextBuilder]를 사용해 [FilterContext]를 재구성하는 [RequestFilter]를 생성합니다.
 *
 * ```kotlin
 * val filter = requestFilter<Any> {
 *     // FilterContextBuilder를 통해 요청을 재구성
 * }
 * val client = asyncHttpClientOf(filter)
 * ```
 *
 * @param builder [FilterContextBuilder] 변경 블록
 * @return 생성된 [RequestFilter]
 */
@JvmName("requestFilterWithBuilder")
inline fun requestFilter(
    crossinline builder: FilterContextBuilder<*>.() -> Unit,
): RequestFilter {
    return object: RequestFilter {
        /** 변경된 [FilterContext]를 생성해 반환합니다. */
        override fun <T> filter(ctx: FilterContext<T>): FilterContext<T> {
            return FilterContextBuilder(ctx).apply(builder).build()
        }
    }
}

/**
 * 람다 핸들러 기반 [RequestFilter]를 생성합니다.
 *
 * ```kotlin
 * val filter = requestFilter { ctx ->
 *     ctx.request.headers.add("X-Custom-Header", "custom-value")
 * }
 * val client = asyncHttpClientOf(filter)
 * val response = client.prepareGet("https://api.example.com").execute().get()
 * // response.statusCode == 200
 * ```
 *
 * @param handler 현재 [FilterContext]를 받아 처리하는 함수
 * @return 생성된 [RequestFilter]
 */
@JvmName("requestFilter")
inline fun requestFilter(
    crossinline handler: (FilterContext<*>) -> Unit,
): RequestFilter {
    return object: RequestFilter {
        /** [handler]를 적용하고 원본 [FilterContext]를 반환합니다. */
        override fun <T> filter(ctx: FilterContext<T>): FilterContext<T> {
            handler(ctx)
            return ctx
        }
    }
}

/**
 * 요청에 고정 헤더를 추가하는 [RequestFilter]를 생성합니다.
 *
 * ```kotlin
 * val filter = attachHeaderRequestFilterOf(
 *     mapOf("Authorization" to "Bearer token", "X-App-Id" to "my-app")
 * )
 * val client = asyncHttpClientOf(filter)
 * val response = client.prepareGet("https://api.example.com").execute().get()
 * // response.statusCode == 200
 * ```
 *
 * @param headers 헤더 이름/값 쌍
 * @return 생성된 [RequestFilter]
 */
fun attachHeaderRequestFilterOf(headers: Map<String, Any?>): RequestFilter {
    return requestFilter { ctx ->
        headers.forEach { (name, value) ->
            ctx.request.headers.add(name, value)
        }
    }
}

/**
 * 요청 시점에 헤더 이름/값을 계산해 추가하는 [RequestFilter]를 생성합니다.
 *
 * ```kotlin
 * val filter = attachHeaderRequestFilterOf(
 *     namesSupplier = { listOf("X-Timestamp", "X-Nonce") },
 *     valueSupplier = { name ->
 *         when (name) {
 *             "X-Timestamp" -> System.currentTimeMillis().toString()
 *             else -> java.util.UUID.randomUUID().toString()
 *         }
 *     }
 * )
 * val client = asyncHttpClientOf(filter)
 * val response = client.prepareGet("https://api.example.com").execute().get()
 * // response.statusCode == 200
 * ```
 *
 * @param namesSupplier 헤더 이름 공급 함수
 * @param valueSupplier 헤더 값 공급 함수
 * @return 생성된 [RequestFilter]
 */
inline fun attachHeaderRequestFilterOf(
    crossinline namesSupplier: () -> Iterable<String>,
    crossinline valueSupplier: (String) -> Any?,
): RequestFilter {
    return requestFilter { ctx ->
        namesSupplier().forEach { name ->
            ctx.request.headers.add(name, valueSupplier(name))
        }
    }
}
