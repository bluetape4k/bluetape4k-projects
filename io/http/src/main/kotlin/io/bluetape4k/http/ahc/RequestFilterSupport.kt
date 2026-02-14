package io.bluetape4k.http.ahc

import org.asynchttpclient.filter.FilterContext
import org.asynchttpclient.filter.FilterContext.FilterContextBuilder
import org.asynchttpclient.filter.RequestFilter

/**
 * [RequestFilter]를 생성합니다.
 *
 * @param builder [FilterContextBuilder]를 이용하여 [FilterContext]를 생성하는 intializer
 * @return [RequestFilter] instance
 */
@JvmName("requestFilterWithBuilder")
inline fun requestFilter(
    @BuilderInference crossinline builder: FilterContextBuilder<*>.() -> Unit,
): RequestFilter {
    return object: RequestFilter {
        /**
         * HTTP 처리에서 `filter` 함수를 제공합니다.
         */
        override fun <T> filter(ctx: FilterContext<T>): FilterContext<T> {
            return FilterContextBuilder(ctx).apply(builder).build()
        }
    }
}

/**
 * [RequestFilter]를 생성합니다.
 *
 * @param handler [FilterContext]를 받아서 처리하는 함수
 * @return [RequestFilter] instance
 */
@JvmName("requestFilter")
inline fun requestFilter(
    @BuilderInference crossinline handler: (FilterContext<*>) -> Unit,
): RequestFilter {
    return object: RequestFilter {
        /**
         * HTTP 처리에서 `filter` 함수를 제공합니다.
         */
        override fun <T> filter(ctx: FilterContext<T>): FilterContext<T> {
            handler(ctx)
            return ctx
        }
    }
}

/**
 * [org.asynchttpclient.Request]에 Header를 추가해주는 [RequestFilter]를 생성합니다.
 *
 * @param headers
 * @return [RequestFilter] instance
 */
fun attachHeaderRequestFilterOf(headers: Map<String, Any?>): RequestFilter {
    return requestFilter { ctx ->
        headers.forEach { (name, value) ->
            ctx.request.headers.add(name, value)
        }
    }
}

/**
 * [org.asynchttpclient.Request]에 Header를 추가해주는 [RequestFilter]를 생성합니다.
 *
 * @param namesSupplier Header name 제공 함수
 * @param valueSupplier Header value 제공 함수
 * @return [RequestFilter] instance
 */
inline fun attachHeaderRequestFilterOf(
    @BuilderInference crossinline namesSupplier: () -> Iterable<String>,
    @BuilderInference crossinline valueSupplier: (String) -> Any?,
): RequestFilter {
    return requestFilter { ctx ->
        namesSupplier().forEach { name ->
            ctx.request.headers.add(name, valueSupplier(name))
        }
    }
}
