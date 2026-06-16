# bluetape4k-ktor-core

[English](./README.md) | [한국어](./README.ko.md)

bluetape4k 애플리케이션에서 공통으로 쓰는 작은 Ktor 서버 기본값 모듈입니다.

## 아키텍처 다이어그램

![Ktor Core Architecture](../../docs/images/readme-diagrams/ktor-core-architecture-01.png)

## 기능

- `Bluetape4kKtorJson.defaultJson()`는 공통 kotlinx serialization 기본값을 제공합니다.
- `installBluetape4kKtorCore()`는 Ktor 기본 플러그인을 명시적으로 설치합니다.
- `ApiErrorResponse`와 `StatusPagesConfig.bluetape4kErrorResponses()`는 일관된 JSON 오류 응답을 만듭니다.
- `/healthz`, `/readyz` 라우트는 기본적으로 `HealthResponse.up()`을 반환합니다.
- Query/path 파라미터 도우미로 반복되는 Ktor 라우트 검증 코드를 줄일 수 있습니다.

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
추가합니다. 애플리케이션이 특정 Ktor plugin을 직접 관리해야 한다면
`Bluetape4kKtorCoreConfig`로 해당 기능만 끄면 됩니다.
