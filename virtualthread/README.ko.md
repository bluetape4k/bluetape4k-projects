# Module bluetape4k-virtualthreads

[English](./README.md) | 한국어

Java 21/25를 같은 프로젝트에서 모듈 분리로 지원하기 위한 구조입니다.

## 아키텍처

### 모듈 구조 및 런타임 선택

```mermaid
flowchart TD
    APP["애플리케이션<br/>(API 모듈만 의존)"]

    API["bluetape4k-virtualthread-api<br/>공통 인터페이스 + ServiceLoader"]

    JDK21["bluetape4k-virtualthread-jdk21<br/>Java 21 구현체<br/>priority = 21"]
    JDK25["bluetape4k-virtualthread-jdk25<br/>Java 25 구현체<br/>priority = 25"]
    FALLBACK["Platform Thread Fallback<br/>(JDK 17 이하)<br/>priority = MIN_VALUE"]

    RUNTIME{"런타임 JDK 버전<br/>ServiceLoader 선택"}

    APP -->|"implementation"| API
    APP -->|"runtimeOnly"| JDK21
    APP -->|"runtimeOnly"| JDK25

    API --> RUNTIME
    RUNTIME -->|"JDK 25 환경<br/>priority 25 선택"| JDK25
    RUNTIME -->|"JDK 21 환경<br/>priority 21 선택"| JDK21
    RUNTIME -->|"JDK 17 이하<br/>isSupported() = false"| FALLBACK

    classDef appStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F,font-weight:bold
    classDef apiStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef implStyle fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    classDef fallbackStyle fill:#F5F5F5,stroke:#BDBDBD,color:#424242
    classDef runtimeStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class APP appStyle
    class API apiStyle
    class JDK21,JDK25 implStyle
    class FALLBACK fallbackStyle
    class RUNTIME runtimeStyle
```

---

### 클래스 다이어그램

```mermaid
classDiagram
    class VirtualThreadFactory {
        <<interface>>
        +isSupported() Boolean
        +priority() Int
        +newThread(runnable: Runnable) Thread
        +executorService() ExecutorService
        +scheduledExecutorService() ScheduledExecutorService
    }

    class VirtualThreads {
        <<object>>
        +isSupported() Boolean
        +newThread(runnable: Runnable) Thread
        +executorService() ExecutorService
        +scheduledExecutorService() ScheduledExecutorService
    }

    class Jdk21VirtualThreadFactory {
        <<class>>
        +isSupported() Boolean
        +priority() Int = 21
        +newThread(runnable) Thread
        +executorService() ExecutorService
    }

    class Jdk25VirtualThreadFactory {
        <<class>>
        +isSupported() Boolean
        +priority() Int = 25
        +newThread(runnable) Thread
        +executorService() ExecutorService
        +joinUntil(thread, instant) Boolean
    }

    class PlatformThreadFallback {
        <<class>>
        +isSupported() Boolean = false
        +priority() Int = MIN_VALUE
        +newThread(runnable) Thread
        +executorService() ExecutorService
    }

    VirtualThreadFactory <|.. Jdk21VirtualThreadFactory
    VirtualThreadFactory <|.. Jdk25VirtualThreadFactory
    VirtualThreadFactory <|.. PlatformThreadFallback
    VirtualThreads ..> VirtualThreadFactory: ServiceLoader가 최고 우선순위 선택

    style VirtualThreadFactory fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style VirtualThreads fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style Jdk21VirtualThreadFactory fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style Jdk25VirtualThreadFactory fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style PlatformThreadFallback fill:#F5F5F5,stroke:#BDBDBD,color:#424242
```

---

### ServiceLoader 선택 시퀀스

```mermaid
sequenceDiagram
        participant App as 애플리케이션
        participant VT as VirtualThreads
        participant SL as ServiceLoader
        participant F21 as Jdk21VirtualThreadFactory
        participant F25 as Jdk25VirtualThreadFactory
    App ->> VT: VirtualThreads.executorService()
    VT ->> SL: ServiceLoader.load(VirtualThreadFactory)
    SL -->> VT: [Jdk21Factory(21), Jdk25Factory(25), Fallback(MIN)]
    VT ->> VT: isSupported() 필터링 후 priority 내림차순 정렬
    Note over VT: JDK 25 환경 → Jdk25Factory 선택
    VT ->> F25: executorService()
    F25 -->> App: VirtualThreadExecutorService (JDK 25)
    Note over App, F25: JDK 21 환경이면 → Jdk21Factory 선택
    VT ->> F21: executorService()
    F21 -->> App: VirtualThreadExecutorService (JDK 21)
```

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

## 사용 방식

애플리케이션은 API 모듈을 기준으로 개발하고, 실행 환경에 맞는 구현 모듈을 classpath에 추가합니다.

```kotlin
import io.bluetape4k.concurrent.virtualthread.VirtualThreads

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
