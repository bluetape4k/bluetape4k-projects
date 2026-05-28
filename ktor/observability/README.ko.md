# bluetape4k-ktor-observability

[English](./README.md) | [한국어](./README.ko.md)

bluetape4k 생태계를 위한 Ktor observability helper 모듈입니다.

## 기능

- `installBluetape4kKtorObservability()` 명시적 Ktor observability 설정.
- Ktor `CallId` 기반 sanitized correlation ID 전파.
- query string을 제외한 기본 로그 메시지와 `CallLogging` MDC 연동.
- 애플리케이션이 `MeterRegistry`를 제공할 때 Micrometer `MicrometerMetrics` 설치.
- 애플리케이션이 소유한 `PrometheusMeterRegistry` 기반 선택적 scrape route.

## 의존성

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-ktor-observability")

    // Prometheus scrape route를 사용할 때만 필요합니다.
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

## 사용 예

```kotlin
import io.bluetape4k.ktor.observability.Bluetape4kKtorObservabilityConfig
import io.bluetape4k.ktor.observability.installBluetape4kKtorObservability
import io.bluetape4k.ktor.observability.prometheusScrapeRoute
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.module(registry: PrometheusMeterRegistry) {
    installBluetape4kKtorObservability(
        Bluetape4kKtorObservabilityConfig(meterRegistry = registry)
    )

    routing {
        prometheusScrapeRoute(registry)
    }
}
```

## 의존성 정책

이 모듈은 Ktor `CallId`, `CallLogging`, `MicrometerMetrics`를 명시적으로
설치합니다. 실제 `MeterRegistry`, exporter, tracing backend는 애플리케이션이
소유합니다. OpenTelemetry tracing은 이 모듈에서 기본 설치하지 않습니다.
exporter 정책이 정해진 애플리케이션 또는 더 좁은 후속 모듈에서 tracing bridge를
추가하세요.

수신 correlation ID는 그대로 echo하지 않습니다. trim, safe character filtering,
length cap을 거친 값만 MDC와 응답 헤더에 사용합니다.
