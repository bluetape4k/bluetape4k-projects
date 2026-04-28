package io.bluetape4k.opentelemetry.webflux

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.spring.webflux.v5_3.SpringWebfluxServerTelemetry
import org.springframework.web.server.WebFilter

/**
 * 이 [OpenTelemetry] 인스턴스로 [SpringWebfluxServerTelemetry]를 생성합니다.
 *
 * ## Spring Boot 버전 제약
 * - **Spring Boot 3 전용** — `opentelemetry-spring-webflux-5.3` 아티팩트는 Spring WebFlux 5.3 / 6.x API 기준입니다.
 * - Spring Boot 4 (Spring Framework 7.x) 지원은 OTel instrumentation BOM에서 대응 아티팩트 출시 후 별도 제공됩니다.
 *
 * ## 운영 제약
 * - **반드시 `@Bean`으로 ApplicationContext 초기화 시 1회만 호출하세요.**
 *   [createTracingWebFilter]가 내부적으로 Reactor `Hooks.onEachOperator`를 전역 등록합니다.
 *   중복 호출 시 Reactor 연산자 체인에 훅이 중첩되어 예기치 않은 동작이 발생할 수 있습니다.
 * - 테스트 격리를 위해 `@AfterAll`에서 `Hooks.resetOnEachOperator(key)`를 호출하세요.
 *
 * ## 민감 헤더 캡처 제한
 * - 기본 설정에서 `Authorization` 등 민감 헤더는 Span attribute로 캡처되지 않습니다.
 * - 헤더 캡처가 필요한 경우 환경변수 `OTEL_INSTRUMENTATION_HTTP_CAPTURE_HEADERS_SERVER_REQUEST`에
 *   허용 헤더 목록을 지정하세요. **PII를 포함하는 헤더는 절대 추가하지 마세요.**
 *
 * ```kotlin
 * @Configuration
 * class TracingConfig(private val openTelemetry: OpenTelemetry) {
 *     @Bean
 *     fun tracingWebFilter(): WebFilter = openTelemetry.createTracingWebFilter()
 * }
 * ```
 *
 * @return [SpringWebfluxServerTelemetry] 인스턴스
 * @see createTracingWebFilter
 */
public fun OpenTelemetry.webfluxServerTelemetry(): SpringWebfluxServerTelemetry =
    SpringWebfluxServerTelemetry.create(this)

/**
 * 이 [OpenTelemetry] 인스턴스로 Spring WebFlux 서버 트레이싱 [WebFilter]를 생성하고
 * Reactor 전역 훅을 등록합니다.
 *
 * ## Spring Boot 버전 제약
 * - **Spring Boot 3 전용** — `opentelemetry-spring-webflux-5.3` 아티팩트는 Spring WebFlux 5.3 / 6.x API 기준입니다.
 * - Spring Boot 4 (Spring Framework 7.x) 지원은 OTel instrumentation BOM에서 대응 아티팩트 출시 후 별도 제공됩니다.
 *
 * ## 운영 제약
 * - **반드시 `@Bean`으로 1회만 호출하세요.** 이 함수는 내부적으로 [SpringWebfluxServerTelemetry.createWebFilterAndRegisterReactorHook]을
 *   호출하여 Reactor `Hooks.onEachOperator`를 전역 등록합니다.
 * - 테스트 격리를 위해 `@AfterAll`에서 `Hooks.resetOnEachOperator(key)`를 호출하세요.
 *
 * ## 보안 경고
 * - 기본 설정에서 `Authorization` 등 민감 헤더는 Span attribute로 캡처되지 않습니다.
 * - 헤더 캡처 허용 목록: 환경변수 `OTEL_INSTRUMENTATION_HTTP_CAPTURE_HEADERS_SERVER_REQUEST`
 *
 * ```kotlin
 * @Configuration
 * class TracingConfig(private val openTelemetry: OpenTelemetry) {
 *     @Bean
 *     fun tracingWebFilter(): WebFilter = openTelemetry.createTracingWebFilter()
 * }
 * ```
 *
 * @return 서버 요청/응답 Span을 자동으로 생성하는 [WebFilter]
 * @see webfluxServerTelemetry
 */
public fun OpenTelemetry.createTracingWebFilter(): WebFilter =
    webfluxServerTelemetry().createWebFilterAndRegisterReactorHook()
