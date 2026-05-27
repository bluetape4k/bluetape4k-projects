# bluetape4k-ktor-core

[English](./README.md) | [한국어](./README.ko.md)

bluetape4k 생태계를 위한 기본 Ktor 서버 helper 모듈입니다.

## 기능

- `Bluetape4kKtorJson.defaultJson()` 공통 kotlinx serialization 기본값.
- `installBluetape4kKtorCore()` 명시적 Ktor 서버 baseline installer.
- `ApiErrorResponse` 및 `StatusPagesConfig.bluetape4kErrorResponses()` JSON 오류 응답.
- `HealthResponse`를 반환하는 `/healthz`, `/readyz` helper.
- 반복되는 Ktor route 검증을 줄이는 query/path parameter helper.

## 의존성

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-ktor-core")
}
```

## 사용 예

```kotlin
import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.bluetape4k.ktor.core.intQueryParameter
import io.bluetape4k.ktor.core.requiredPathParameter
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
    installBluetape4kKtorCore()

    routing {
        get("/items/{type}") {
            val type = call.requiredPathParameter("type")
            val size = call.intQueryParameter("size", defaultValue = 10, range = 1..100)

            call.respond(mapOf("type" to type, "size" to size))
        }
    }
}
```

기본 installer는 content negotiation, JSON 오류 처리, health/readiness route를
추가합니다. 애플리케이션이 해당 Ktor plugin을 직접 소유해야 한다면
`Bluetape4kKtorCoreConfig`로 각 기능을 끌 수 있습니다.
