package io.bluetape4k.http.ahc

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.asynchttpclient.filter.FilterContext
import org.asynchttpclient.filter.RequestFilter

/**
 * 동적으로 헤더 정보를 조회해서 추가할 수 있도록 합니다.
 */
class DynamicAttachHandlerRequest(
    private val headerNames: List<String>,
    private val headerValueSupplier: (String) -> String,
): RequestFilter {

    companion object: KLogging()

    override fun <T> filter(ctx: FilterContext<T>): FilterContext<T> {
        headerNames.forEach { name ->
            runCatching {
                val value = headerValueSupplier(name)
                log.trace { "Add header name=$name, value=$value" }
                ctx.request.headers.add(name, value)
            }
        }
        return ctx
    }
}
