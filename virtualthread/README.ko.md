# Module bluetape4k-virtualthreads

[English](./README.md) | 한국어

Java 21/25를 같은 프로젝트에서 모듈 분리로 지원하기 위한 구조입니다.

API 모듈은 `io.bluetape4k.concurrent.virtualthread.api` 패키지를
소유하고, core 유틸리티는 `io.bluetape4k.concurrent.virtualthread`에
남깁니다. 이 경계로 published `core`와 `virtualthread-api` artifact의
split package를 제거했습니다. 패키지 이동 뒤에는 API import를 갱신하고
소비자를 재컴파일해야 합니다.

## 아키텍처

### 런타임 선택 흐름

![ServiceLoader provider, 우선순위 정렬, platform fallback을 사용하는 VirtualThreads 런타임 선택 흐름](../docs/images/readme-diagrams/virtualthread-diagram-01.png)

---

### 클래스 다이어그램

![런타임 파사드, provider 인터페이스, JDK 구현체, scope 계약, TaskContext를 보여주는 가상 스레드 클래스 구조](../docs/images/readme-diagrams/virtualthread-diagram-02.png)

---

### ServiceLoader 선택 시퀀스

![VirtualThreadRuntime 발견, 지원 여부 필터링, 우선순위 정렬, executor 위임을 보여주는 ServiceLoader 선택 시퀀스](../docs/images/readme-diagrams/virtualthread-sequence-01.png)

---

## 모듈

- `bluetape4k-virtualthreads-api`
    - 공통 API 및 `ServiceLoader` 기반 런타임 선택기
- `bluetape4k-virtualthreads-jdk21`
    - Java 21 구현체
- `bluetape4k-virtualthreads-jdk25`
    - Java 25 구현체

## 주요 기능

- **ServiceLoader 기반 디스패치**: 런타임에 사용 가능한 가장 높은 우선순위 구현체를 자동 선택
- **Platform Thread 폴백**: JDK 17 이하에서는 플랫폼 스레드로 자연스럽게 대체
- **통합 API**: 애플리케이션 코드는 `api` 모듈에만 의존 — 런타임별 임포트 불필요
- **JDK 25 추가 기능**: `joinUntil(Instant)` — 데드라인까지 가상 스레드를 대기 (JDK 25 전용)
- **TaskContext**: `ScopedValue` 기반 컨텍스트 전파 — `StructuredTaskScope.fork()` subtask에 자동 상속

## TaskContext — ScopedValue 기반 컨텍스트 전파

`TaskContext`는 `ScopedValue`를 래핑하여 가상 스레드 간 안전하고 immutable한 컨텍스트 전파를 제공합니다.
`ThreadLocal`과 달리, `ScopedValue` 바인딩은 스코프를 벗어나면 자동으로 해제되며 누출되지 않습니다.

> **주의**: 바인딩은 `StructuredTaskScope.fork()`로 생성된 스레드에만 자동 전파됩니다.
> 일반 `Thread.ofVirtual().start {}`로 생성된 스레드는 바인딩을 **상속하지 않습니다**.

```kotlin
val REQUEST_ID: ScopedValue<String> = TaskContext.newKey()
val TENANT_ID:  ScopedValue<String> = TaskContext.newKey()

// 단일 바인딩 — top-level 함수 스타일 (권장)
withTaskContext(REQUEST_ID, "req-001") {
    println(TaskContext.get(REQUEST_ID))  // "req-001"

    // forked subtask에 자동 전파
    StructuredTaskScopes.failFast { scope ->
        val result = scope.fork { TaskContext.get(REQUEST_ID) }
        scope.join().throwIfFailed()
        result.get()  // "req-001"
    }
}

// 단일 바인딩 — 멤버 함수 스타일
TaskContext.run(REQUEST_ID, "req-001") {
    println(TaskContext.get(REQUEST_ID))  // "req-001"
}

// 다중 바인딩
TaskContext.bind(REQUEST_ID, "req-001")
    .and(TENANT_ID, "tenant-42")
    .run {
        println(TaskContext.get(REQUEST_ID))  // "req-001"
        println(TaskContext.get(TENANT_ID))   // "tenant-42"
    }

// supervised scope 에서 Result<T> 수집
val results: List<Result<String>> =
    withTaskContext(REQUEST_ID, "req-001") {
        StructuredTaskScopes.supervised<String, List<Result<String>>> { scope ->
            scope.fork { TaskContext.get(REQUEST_ID) ?: "" }
            scope.fork { TaskContext.get(REQUEST_ID) ?: "" }
            scope.join()
            scope.results()
        }
    }
```

| API | 설명 |
|-----|------|
| `TaskContext.newKey<T>()` | 타입 안전 `ScopedValue` 키 생성 |
| `TaskContext.get(key)` | 바인딩된 값 반환, 미바인딩 시 `null` |
| `TaskContext.getOrDefault(key, default)` | 바인딩된 값 반환, 미바인딩 시 기본값 |
| `TaskContext.isBound(key)` | 현재 스코프에서 키 바인딩 여부 확인 |
| `TaskContext.run(key, value) {}` | 단일 바인딩 스코프 블록 |
| `withTaskContext(key, value) {}` | `TaskContext.run`의 top-level 별칭 (권장) |
| `TaskContext.bind(key, value).and(...).run {}` | 다중 바인딩 스코프 블록 |

## 사용 방식

애플리케이션은 API 모듈을 기준으로 개발하고, 실행 환경에 맞는 구현 모듈을 classpath에 추가합니다.

```kotlin
import io.bluetape4k.concurrent.virtualthread.api.VirtualThreads

// 가상 스레드 Executor 생성
val executor = VirtualThreads.executorService()

// 단일 가상 스레드 시작
val thread = VirtualThreads.newThread {
    // 가상 스레드에서 실행
    println("Hello from virtual thread!")
}
thread.start()

// 런타임에 가상 스레드 지원 여부 확인
if (VirtualThreads.isSupported()) {
    println("가상 스레드 사용 가능")
}
```

### Gradle 의존성

```kotlin
// API만 (컴파일 타임)
implementation("io.github.bluetape4k:bluetape4k-virtualthread-api:${version}")

// 런타임 구현체 (JDK 버전에 맞는 것 추가)
runtimeOnly("io.github.bluetape4k:bluetape4k-virtualthread-jdk21:${version}")
// 또는
runtimeOnly("io.github.bluetape4k:bluetape4k-virtualthread-jdk25:${version}")
```

## 주의

- Java 21 런타임에서 Java 25 구현 모듈을 함께 classpath에 올리면 클래스 버전 충돌이 날 수 있습니다.
- 배포 시에는 런타임 버전에 맞는 구현 모듈만 포함하거나, 배포 파이프라인에서 JDK별 아티팩트를 분리하세요.
- `virtualthread-api`의 인터페이스를 추가/변경할 때는 `jdk21`과 `jdk25` 구현체를 반드시 같은 커밋에 함께 수정하세요.
