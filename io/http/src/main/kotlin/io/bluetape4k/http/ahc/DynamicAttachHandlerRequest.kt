package io.bluetape4k.http.ahc

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import org.asynchttpclient.filter.FilterContext
import org.asynchttpclient.filter.RequestFilter

/**
 * 요청 시점에 동적으로 헤더 값을 계산해 추가하는 [RequestFilter]입니다.
 *
 * [headerValueSupplier] 실행 중 예외가 발생해도 요청 전체를 중단하지 않으며,
 * 실패 내용은 로그로 남깁니다.
 */
class DynamicAttachHandlerRequest(
    private val headerNames: List<String>,
    private val headerValueSupplier: (String) -> Any?,
): RequestFilter {

    companion object: KLogging()

    /** [headerValueSupplier]로 헤더 값을 계산해 요청에 추가합니다. */
    override fun <T> filter(ctx: FilterContext<T>): FilterContext<T> {
        headerNames.forEach { name ->
            runCatching {
                val value = headerValueSupplier(name)
                log.trace { "Add header name=$name, value=$value" }
                ctx.request.headers.add(name, value)
            }.onFailure { cause ->
                log.warn(cause) { "Fail to resolve dynamic header. name=$name" }
            }
        }
        return ctx
    }
}
