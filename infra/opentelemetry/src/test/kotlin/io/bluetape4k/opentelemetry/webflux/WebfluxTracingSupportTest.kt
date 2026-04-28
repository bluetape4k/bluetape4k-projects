package io.bluetape4k.opentelemetry.webflux

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.opentelemetry.AbstractOtelTest
import io.bluetape4k.opentelemetry.trace.sdkTracerProvider
import io.bluetape4k.opentelemetry.trace.simpleSpanProcessorOf
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import reactor.core.publisher.Hooks

/**
 * [OpenTelemetry.webfluxServerTelemetry] / [OpenTelemetry.createTracingWebFilter] 스모크 테스트.
 *
 * 목표:
 * - 두 함수가 호출 가능하고 non-null 인스턴스를 반환하는지 확인합니다.
 * - Reactor 전역 훅이 `@AfterAll`에서 리셋되는지 확인합니다.
 *
 * ## 통합 테스트 제한
 * 실제 HTTP 요청 → Span 생성 검증은 Spring Boot 3 통합 테스트 환경(`@SpringBootTest` + `WebTestClient`)이 필요합니다.
 * 이 테스트는 wrapper 함수의 API 연결만 검증합니다.
 */
class WebfluxTracingSupportTest: AbstractOtelTest() {

    companion object: KLoggingChannel() {
        private const val REACTOR_HOOK_KEY = "io.opentelemetry.javaagent.shaded.io.opentelemetry.instrumentation.reactornetty.v1_0"

        @JvmStatic
        @AfterAll
        fun resetReactorHooks() {
            // createWebFilterAndRegisterReactorHook이 전역 Hooks.onEachOperator를 등록하므로 테스트 후 해제
            Hooks.resetOnEachOperator()
        }
    }

    private val spanExporter = InMemorySpanExporter.create()
    private val tracerProvider = sdkTracerProvider {
        addSpanProcessor(simpleSpanProcessorOf(spanExporter))
    }

    @Test
    fun `webfluxServerTelemetry should return non-null SpringWebfluxServerTelemetry`() {
        val telemetry = loggingOtel.webfluxServerTelemetry()
        telemetry.shouldNotBeNull()
    }

    @Test
    fun `createTracingWebFilter should return non-null WebFilter`() {
        val webFilter = loggingOtel.createTracingWebFilter()
        webFilter.shouldNotBeNull()
    }
}
