# bluetape4k-micrometer

Micrometer와 Observation API를 활용한 애플리케이션 성능 측정 및 관찰(observability) 기능을 제공하는 모듈입니다.

## 개요

이 모듈은 [Micrometer](https://micrometer.io/)와 Spring Boot의 Observation API를 확장하여 다음 기능을 제공합니다:

- **Timer 확장**: Suspend 함수 및 Kotlin Flow에 대한 실행 시간 측정
- **Observation 확장**: Coroutine 환경에서의 관찰 기능 지원
- **Retrofit2 메트릭**: HTTP 클라이언트 호출 메트릭 자동 수집
- **Cache2k 메트릭**: 캐시 성능 메트릭 수집
- **KeyValue 유틸리티**: Micrometer KeyValue 생성을 위한 확장 함수

## 의존성

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-micrometer:${bluetape4kVersion}")
}
```

## 주요 기능

### 1. Timer 확장 (TimerExtensions)

#### Suspend 함수 실행 시간 측정

```kotlin
import io.bluetape4k.micrometer.instrument.recordSuspend
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

val registry = SimpleMeterRegistry()
val timer = registry.timer("api.call")

// Suspend 함수의 실행 시간 측정
val result = timer.recordSuspend {
    fetchUserData(userId)  // suspend 함수
}
```

#### Flow 실행 시간 측정

```kotlin
import io.bluetape4k.micrometer.instrument.withTimer
import kotlinx.coroutines.flow.flow

val timer = registry.timer("flow.processing")

val flow = flow {
    emit(fetchData())
}.withTimer(timer)
    .collect { data ->
        // 처리 로직
    }
```

### 2. Observation 확장

#### 기본 Observation 사용

```kotlin
import io.bluetape4k.micrometer.observation.withObservation
import io.micrometer.observation.ObservationRegistry

val registry = ObservationRegistry.create()

val result = withObservation("user.service.getUser", registry) {
    userService.findById(userId)
}
```

#### Observation 컨텍스트 사용

```kotlin
import io.bluetape4k.micrometer.observation.withObservationContext

observation.withObservationContext { context ->
    context.put("user.id", userId)
    context.put("user.type", "premium")
    processUser(userId)
}
```

#### Coroutine 환경에서 Observation 사용

```kotlin
import io.bluetape4k.micrometer.observation.coroutines.withObservationContext
import io.bluetape4k.micrometer.observation.coroutines.currentObservationInContext

withObservationContext("async.operation", registry) {
    val observation = currentObservationInContext()
    observation?.highCardinalityKeyValue("operation.id", generateId())

    // 비동기 작업 수행
    delay(100)
    performAsyncWork()
}
```

#### ObservationRegistry 생성

```kotlin
import io.bluetape4k.micrometer.observation.observationRegistryOf
import io.bluetape4k.micrometer.observation.simpleObservationRegistryOf

// 커스텀 핸들러가 있는 Registry
val registry = observationRegistryOf { ctx ->
    log.debug { "Observation: ${ctx.name}" }
    true  // 계속 처리
}

// 간단한 Registry
val simpleRegistry = simpleObservationRegistryOf { ctx ->
    log.trace { "Context: $ctx" }
}
```

### 3. Retrofit2 메트릭

Retrofit2 HTTP 호출의 실행 시간과 결과를 자동으로 수집합니다.

#### 기본 사용법

```kotlin
import io.bluetape4k.micrometer.instrument.retrofit2.MicrometerRetrofitMetricsFactory
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import retrofit2.Retrofit

val registry = SimpleMeterRegistry()
val metricsFactory = MicrometerRetrofitMetricsFactory(registry)

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .addCallAdapterFactory(metricsFactory)
    .addConverterFactory(JacksonConverterFactory.create())
    .build()

val apiService = retrofit.create(ApiService::class.java)

// 이제 모든 API 호출이 자동으로 메트릭 수집됨
val users = apiService.getUsers().execute()
```

#### 수집되는 메트릭

| 태그            | 설명         | 예시                                  |
|---------------|------------|-------------------------------------|
| `method`      | HTTP 메서드   | GET, POST, PUT, DELETE              |
| `uri`         | 요청 URI     | /users/{id}                         |
| `base_url`    | 기본 URL     | https://api.example.com             |
| `status_code` | HTTP 상태 코드 | 200, 404, 500                       |
| `outcome`     | 결과 분류      | SUCCESS, CLIENT_ERROR, SERVER_ERROR |
| `coroutines`  | 코루틴 사용 여부  | true, false                         |

#### 퍼센타일

다음 퍼센타일이 자동으로 수집됩니다: 50%, 70%, 90%, 95%, 97%, 99%

### 4. Cache2k 메트릭

Cache2k 캐시의 성능 메트릭을 Micrometer에 노출합니다.

```kotlin
import io.bluetape4k.micrometer.instrument.cache.Cache2kCacheMetrics
import io.micrometer.core.instrument.Tags
import org.cache2k.Cache

val cache: Cache<String, User> = Cache2kBuilder.of(String::class.java, User::class.java)
    .name("user-cache")
    .build()

val registry = SimpleMeterRegistry()
val tags = Tags.of("service", "user-service")

// 캐시 메트릭 등록
Cache2kCacheMetrics.monitor(registry, cache, tags)
```

#### 수집되는 메트릭

| 메트릭 이름                    | 타입              | 설명                     |
|---------------------------|-----------------|------------------------|
| `cache.size`              | Gauge           | 현재 캐시 크기               |
| `cache.gets`              | FunctionCounter | 캐시 조회 횟수 (hit/miss 태그) |
| `cache.puts`              | FunctionCounter | 캐시 저장 횟수               |
| `cache.evictions`         | FunctionCounter | 캐시 제거 횟수               |
| `cache.load.duration`     | TimeGauge       | 캐시 로딩 시간               |
| `cache.cleared.timestamp` | Gauge           | 마지막 캐시 클리어 시각          |
| `cache.load`              | FunctionCounter | 로딩 성공/실패 횟수            |
| `cache.expired.count`     | FunctionCounter | 만료된 엔트리 수              |

### 5. KeyValue 유틸리티

Micrometer KeyValue 생성을 위한 확장 함수들을 제공합니다.

```kotlin
import io.bluetape4k.micrometer.common.keyValueOf
import io.bluetape4k.micrometer.common.keyValuesOf

// 단일 KeyValue 생성
val kv1 = keyValueOf("key", "value")

// 값 검증과 함께 생성
val kv2 = keyValueOf("count", 150) { it > 100 }

// 여러 KeyValue 생성
val kvs1 = keyValuesOf("k1", "v1", "k2", "v2")

// Pair로 생성
val kvs2 = keyValuesOf("key1" to "value1", "key2" to "value2")

// Map으로 생성
val kvs3 = keyValueOf(mapOf("x" to "1", "y" to "2"))

// KeyValue 컬렉션으로 생성
val kvs4 = keyValueOf(listOf(KeyValue.of("a", "1")))
```

## 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    bluetape4k-micrometer                     │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │   Timer      │  │ Observation  │  │   KeyValue   │       │
│  │  Extensions  │  │  Extensions  │  │   Support    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Instrumentation Modules                  │   │
│  ├────────────────────┬──────────────────────────────────┤   │
│  │  Retrofit2 Metrics │       Cache2k Metrics            │   │
│  │  - HTTP 호출 측정   │  - 캐시 히트/미스 측정            │   │
│  │  - 응답 시간 기록   │  - 로딩 시간 측정                 │   │
│  │  - 상태 코드 태그   │  - 만료/제거 카운트               │   │
│  └────────────────────┴──────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                    Micrometer Core                          │
└─────────────────────────────────────────────────────────────┘
```

## 테스트

모든 기능에 대한 테스트가 포함되어 있습니다:

```bash
# 모든 테스트 실행
./gradlew :infra:micrometer:test

# 특정 테스트 클래스 실행
./gradlew :infra:micrometer:test --tests "io.bluetape4k.micrometer.instrument.TimerExtensionsTest"
```

## 참고사항

- Kotlin 2.3 이상 필요
- Java 21 이상 필요
- Micrometer 1.13.x 이상 필요
- Coroutines 환경에서는 `runSuspendIO`를 사용하여 실제 시간 측정 필요 (runTest는 시간을 에뮬레이션함)

## 관련 모듈

- `bluetape4k-core`: 핵심 유틸리티
- `bluetape4k-coroutines`: Coroutines 지원
- `bluetape4k-retrofit2`: Retrofit2 통합
- `bluetape4k-cache`: Cache2k 통합
