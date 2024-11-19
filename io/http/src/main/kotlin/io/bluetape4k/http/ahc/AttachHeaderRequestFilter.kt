package io.bluetape4k.http.ahc

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.asynchttpclient.filter.FilterContext
import org.asynchttpclient.filter.RequestFilter

/**
 * AsyncHttpClient [Request] 에 Header를 추가하기 위해 사용합니다.
 *
 * @see attachHeaderRequestFilterOf
 */
class AttachHeaderRequestFilter(val headers: Map<String, Any?>): RequestFilter {

    companion object: KLogging()

    override fun <T: Any?> filter(ctx: FilterContext<T>): FilterContext<T> {
        this.headers.forEach { (name, value) ->
            log.trace { "Add header name=$name, value=$value" }
            ctx.request.headers.add(name, value)
        }
        return ctx
    }
}
