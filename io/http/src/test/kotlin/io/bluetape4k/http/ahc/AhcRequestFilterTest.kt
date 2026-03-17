package io.bluetape4k.http.ahc

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.asynchttpclient.filter.FilterContext
import org.junit.jupiter.api.Test

/**
 * [AttachHeaderRequestFilter], [DynamicAttachHandlerRequest], [requestFilter] DSL 에 대한 단위 테스트입니다.
 */
class AhcRequestFilterTest {
    companion object : KLogging()

    /**
     * 테스트용 더미 FilterContext를 생성합니다.
     * 실제 AHC 요청 없이 필터 로직만 검증합니다.
     */
    private fun buildFilterContext(): FilterContext<Any> {
        val request =
            org.asynchttpclient
                .RequestBuilder("GET")
                .setUrl("http://localhost/test")
                .build()
        return FilterContext
            .FilterContextBuilder<Any>()
            .request(request)
            .build()
    }

    @Test
    fun `AttachHeaderRequestFilter - 고정 헤더를 요청에 추가한다`() {
        val headers = mapOf("X-App-Key" to "app-123", "X-Trace-Id" to "trace-1")
        val filter = AttachHeaderRequestFilter(headers)

        val ctx = buildFilterContext()
        val result = filter.filter(ctx)

        result.shouldNotBeNull()
        result.request.headers.get("X-App-Key") shouldBeEqualTo "app-123"
        result.request.headers.get("X-Trace-Id") shouldBeEqualTo "trace-1"
    }

    @Test
    fun `AttachHeaderRequestFilter - 빈 헤더 맵이면 헤더를 추가하지 않는다`() {
        val filter = AttachHeaderRequestFilter(emptyMap())

        val ctx = buildFilterContext()
        val result = filter.filter(ctx)

        result.shouldNotBeNull()
        result.request.headers.size() shouldBeEqualTo 0
    }

    @Test
    fun `DynamicAttachHandlerRequest - 공급 함수로 헤더 값을 계산해 추가한다`() {
        val filter =
            DynamicAttachHandlerRequest(
                headerNames = listOf("X-Dynamic", "X-Counter"),
                headerValueSupplier = { name ->
                    when (name) {
                        "X-Dynamic" -> "dynamic-value"
                        "X-Counter" -> "42"
                        else -> null
                    }
                }
            )

        val ctx = buildFilterContext()
        val result = filter.filter(ctx)

        result.shouldNotBeNull()
        result.request.headers.get("X-Dynamic") shouldBeEqualTo "dynamic-value"
        result.request.headers.get("X-Counter") shouldBeEqualTo "42"
    }

    @Test
    fun `DynamicAttachHandlerRequest - 공급 함수에서 예외 발생 시 요청을 중단하지 않는다`() {
        val filter =
            DynamicAttachHandlerRequest(
                headerNames = listOf("X-Fail", "X-Ok"),
                headerValueSupplier = { name ->
                    if (name == "X-Fail") {
                        throw RuntimeException("Supplier failure")
                    } else {
                        "ok-value"
                    }
                }
            )

        val ctx = buildFilterContext()
        // 예외가 전파되지 않고 필터 컨텍스트가 정상 반환되어야 한다
        val result = filter.filter(ctx)

        result.shouldNotBeNull()
        result.request.headers.get("X-Ok") shouldBeEqualTo "ok-value"
    }

    @Test
    fun `attachHeaderRequestFilterOf with map - 헤더 맵으로 필터를 생성한다`() {
        val filter = attachHeaderRequestFilterOf(mapOf("X-Fixed" to "fixed-val"))

        val ctx = buildFilterContext()
        val result = filter.filter(ctx)

        result.shouldNotBeNull()
        result.request.headers.get("X-Fixed") shouldBeEqualTo "fixed-val"
    }

    @Test
    fun `attachHeaderRequestFilterOf with suppliers - 공급 함수로 필터를 생성한다`() {
        val filter =
            attachHeaderRequestFilterOf(
                namesSupplier = { listOf("X-Supplier-Header") },
                valueSupplier = { "supplier-value" }
            )

        val ctx = buildFilterContext()
        val result = filter.filter(ctx)

        result.shouldNotBeNull()
        result.request.headers.get("X-Supplier-Header") shouldBeEqualTo "supplier-value"
    }

    @Test
    fun `requestFilter with handler - 람다 핸들러로 필터를 생성한다`() {
        var called = false
        val filter =
            requestFilter { ctx ->
                called = true
                ctx.request.headers.add("X-Handler", "handler-value")
            }

        val ctx = buildFilterContext()
        val result = filter.filter(ctx)

        called.shouldBeTrue()
        result.shouldNotBeNull()
        result.request.headers.get("X-Handler") shouldBeEqualTo "handler-value"
    }
}
