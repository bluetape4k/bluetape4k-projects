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
- `installApplicationResourceLifecycle()`은 애플리케이션이 소유한 동기식 리소스를
  `ApplicationStopped`에서 닫습니다.

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

## 애플리케이션 소유 리소스

애플리케이션 시작 시 resource lifecycle을 명시적으로 설치하고, 해당 애플리케이션이 소유한
리소스만 등록합니다.

```kotlin
import io.bluetape4k.ktor.core.installApplicationResourceLifecycle

fun Application.module() {
    val resources = installApplicationResourceLifecycle()
    val client = createClient()

    resources.register(client)
}
```

registry는 아직 남은 항목을 등록의 역순으로 닫고, 종료 뒤 등록된 항목은 즉시 닫습니다.
registration token과 registry 종료는 멱등이며 서로 경합해도 각 항목을 최대 한 번만
claim합니다. close 실패는 다른 항목과 격리하고 opaque registration ID, close phase, fatal
여부만 report에 남깁니다. 예외 message, stack trace, class 이름, resource 문자열은 노출하지
않습니다.

close action은 호출한 thread에서 동기식으로 실행됩니다. 애플리케이션 시작 경로가 소유하고
유한한 시간 안에 끝나는 trusted action만 등록해야 합니다. registry는 coroutine scope,
dispatcher, thread, timeout, retry, backend별 drain 정책을 만들지 않습니다. background job이
리소스를 계속 사용한다면 adapter가 resource 종료 전에 bounded drain을 수행하거나, job이 더
이상 resource에 접근하지 않는다는 조건을 보장해야 합니다.

Ktor application job의 disposal timeout이 발생한 뒤에도 `ApplicationStopped`가 발생할 수
있으며, 이 event handler는 해당 timeout 바깥에서 실행됩니다. 따라서 lifecycle 설치만으로
모든 application coroutine의 완료를 보장하지 않습니다. 이 기능은 graceful shutdown 전용이며
`SIGKILL`, JVM crash, OOM 경로의 cleanup은 보장하지 않습니다. registry를 닫는 lifecycle
bridge는 하나만 두고, 의존 리소스는 하나의 composite action으로 묶거나 의존 순서대로
등록합니다.
