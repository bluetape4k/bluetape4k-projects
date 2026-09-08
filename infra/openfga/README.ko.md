# Module bluetape4k-openfga

한국어 | [English](./README.md)

공식 OpenFGA Java SDK를 Kotlin Coroutines 환경에서 사용할 수 있도록 확장한 모듈입니다. SDK의 요청·응답
모델을 그대로 유지하면서 취소 가능한 suspend 호출과 tuple 페이지를 cold `Flow`로 제공합니다.

## 주요 기능

- store ID와 authorization model ID를 담는 불변 `OpenFgaScope`
- SDK의 low-level `OpenFgaApi`를 감싼 `checkSuspending`, `batchCheckSuspending`,
  `readSuspending`, `writeSuspending`
- 순차 페이징, backpressure, 1..100 page 크기, 기본 10,000 page 상한을 제공하는
  `readTuplesFlow`
- `openFgaTuple` 입력 검증과 SDK tuple-key 타입 변환
- SDK·서버 기본 상한을 사전 검증: batch check 1..50개, writes와 deletes 합계 최대 100개
- 호출자가 소유하는 SDK 설정·수명주기. 숨은 retry, cache, close 동작을 추가하지 않음
- `kotlinx.coroutines.future.await`를 통한 Future 취소 전파

## 설치

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-openfga:$version")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.bluetape4k</groupId>
    <artifactId>bluetape4k-openfga</artifactId>
    <version>${version}</version>
</dependency>
```

이 모듈은 `dev.openfga:openfga-sdk:0.10.0`, `bluetape4k-coroutines`,
`bluetape4k-logging`을 사용합니다.

## 사용법

애플리케이션이 소유한 설정과 인증 정보로 공식 low-level API를 만들고, 모든 작업에 불변 scope를 전달합니다.

```kotlin
import dev.openfga.sdk.api.OpenFgaApi
import dev.openfga.sdk.api.configuration.Configuration
import dev.openfga.sdk.api.model.CheckRequest
import io.bluetape4k.openfga.OpenFgaScope
import io.bluetape4k.openfga.checkSuspending
import io.bluetape4k.openfga.openFgaTuple
import io.bluetape4k.openfga.toCheckRequestTupleKey

val api = OpenFgaApi(Configuration().apiUrl("http://localhost:8080"))
val scope = OpenFgaScope(
    storeId = "store-id",
    authorizationModelId = "model-id",
)
val tuple = openFgaTuple("user:anne", "reader", "document:budget")

val response = api.checkSuspending(
    scope,
    CheckRequest().tupleKey(tuple.toCheckRequestTupleKey()),
)
check(response.data.allowed == true)
```

wrapper는 mutable SDK request를 복사한 뒤 scope를 적용하므로 공유 request 객체의 원래 model ID와 옵션을
변경하지 않습니다. 요청별 timeout, header, retry 설정이 필요하면 공식 `ConfigurationOverride`를 그대로
전달합니다.

```kotlin
import dev.openfga.sdk.api.configuration.ConfigurationOverride
import java.time.Duration

val response = api.checkSuspending(
    scope,
    request,
    ConfigurationOverride().readTimeout(Duration.ofSeconds(2)),
)
```

tuple을 지연해서 한 페이지씩 읽습니다.

```kotlin
import dev.openfga.sdk.api.model.ReadRequest
import io.bluetape4k.openfga.readTuplesFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

val tuples = api.readTuplesFlow(
    scope = OpenFgaScope("store-id"),
    request = ReadRequest(),
    pageSize = 50,
    maxPages = 10_000,
).take(100).toList()
```

`readTuplesFlow`는 수집하기 전에는 요청을 보내지 않고, 서버가 빈 continuation token을 반환하면 종료합니다.
즉시 반복되는 cursor는 거부하며, `maxPages`에 도달한 뒤에도 cursor가 남으면 실패합니다. collector가 취소되면
다음 page를 요청하지 않습니다. 각 suspend wrapper는 `Future.await`를 사용하므로 취소 시 local await를 중단하고
반환된 `CompletableFuture`의 취소를 요청합니다. SDK 내부 transport 또는 retry 작업의 중단까지 보장하지 않으므로,
네트워크 기한이 필요하면 `ConfigurationOverride`로 SDK 요청 timeout/deadline을 설정해야 합니다. 서버에 이미
반영된 write를 롤백하지는 않습니다.

SDK에는 close API가 없으므로 이 모듈도 주입받은 `OpenFgaApi`를 닫지 않습니다. SDK 설정과 HTTP 자원의 소유권은
호출자에게 있습니다.

## 테스트

단위 테스트는 SDK Future를 mock합니다. 실제 서버 테스트는 `integration` 태그를 사용하며, SDK와 무관한
Testcontainers OpenFGA endpoint 하나를 시작합니다. 임시 store와 model을 만든 뒤 tuple 쓰기, allow/deny 검사,
Flow 페이징 읽기를 수행하고 `finally`에서 store를 삭제합니다.

```bash
./gradlew :bluetape4k-openfga:test :bluetape4k-openfga:detekt --max-workers=1
./gradlew :bluetape4k-openfga:test -PexcludeIntegrationTests=true --max-workers=1
```
