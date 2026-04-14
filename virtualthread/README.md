# Module bluetape4k-virtualthreads

English | [한국어](./README.ko.md)

This structure supports Java 21 and Java 25 in the same project by splitting the implementations into separate modules.

## Architecture

### Module Structure and Runtime Selection

```mermaid
flowchart TD
    APP["Application<br/>(depends only on the API module)"]

    API["bluetape4k-virtualthread-api<br/>Common interfaces + ServiceLoader"]

    JDK21["bluetape4k-virtualthread-jdk21<br/>Java 21 implementation<br/>priority = 21"]
    JDK25["bluetape4k-virtualthread-jdk25<br/>Java 25 implementation<br/>priority = 25"]
    FALLBACK["Platform Thread Fallback<br/>(JDK 17 or lower)<br/>priority = MIN_VALUE"]

    RUNTIME{"Runtime JDK version<br/>selected by ServiceLoader"}

    APP -->|"implementation"| API
    APP -->|"runtimeOnly"| JDK21
    APP -->|"runtimeOnly"| JDK25

    API --> RUNTIME
    RUNTIME -->|"JDK 25 runtime<br/>select priority 25"| JDK25
    RUNTIME -->|"JDK 21 runtime<br/>select priority 21"| JDK21
    RUNTIME -->|"JDK 17 or lower<br/>isSupported() = false"| FALLBACK

    classDef appStyle fill:#37474F,stroke:#263238,color:#FFFFFF,font-weight:bold
    classDef apiStyle fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    classDef implStyle fill:#00897B,stroke:#00695C,color:#FFFFFF
    classDef fallbackStyle fill:#616161,stroke:#424242,color:#FFFFFF
    classDef runtimeStyle fill:#E65100,stroke:#BF360C,color:#FFFFFF

    class APP appStyle
    class API apiStyle
    class JDK21,JDK25 implStyle
    class FALLBACK fallbackStyle
    class RUNTIME runtimeStyle
```

---

### Class Diagram

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
    VirtualThreads ..> VirtualThreadFactory: ServiceLoader selects highest priority

    style VirtualThreadFactory fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    style VirtualThreads fill:#E65100,stroke:#BF360C,color:#FFFFFF
    style Jdk21VirtualThreadFactory fill:#00897B,stroke:#00695C,color:#FFFFFF
    style Jdk25VirtualThreadFactory fill:#00897B,stroke:#00695C,color:#FFFFFF
    style PlatformThreadFallback fill:#616161,stroke:#424242,color:#FFFFFF
```

---

### ServiceLoader Selection Sequence

```mermaid
sequenceDiagram
    box rgb(207,216,220) Application Layer
        participant App as Application
    end
    box rgb(178,223,219) API Layer
        participant VT as VirtualThreads
        participant SL as ServiceLoader
    end
    box rgb(178,223,219) Implementations
        participant F21 as Jdk21VirtualThreadFactory
        participant F25 as Jdk25VirtualThreadFactory
    end
    App ->> VT: VirtualThreads.executorService()
    VT ->> SL: ServiceLoader.load(VirtualThreadFactory)
    SL -->> VT: [Jdk21Factory(21), Jdk25Factory(25), Fallback(MIN)]
    VT ->> VT: filter isSupported() && sort by priority desc
    Note over VT: JDK 25 runtime → Jdk25Factory wins
    VT ->> F25: executorService()
    F25 -->> App: VirtualThreadExecutorService (JDK 25)
    Note over App, F25: JDK 21 runtime → Jdk21Factory wins instead
    VT ->> F21: executorService()
    F21 -->> App: VirtualThreadExecutorService (JDK 21)
```

---

## Modules

- `bluetape4k-virtualthreads-api`
    - shared API and a `ServiceLoader`-based runtime selector
- `bluetape4k-virtualthreads-jdk21`
    - Java 21 implementation
- `bluetape4k-virtualthreads-jdk25`
    - Java 25 implementation

## Key Features

- **ServiceLoader-based dispatch**: Automatically selects the highest-priority implementation available at runtime
- **Platform thread fallback**: Gracefully degrades to platform threads on JDK 17 and below
- **Unified API**: Application code depends only on the `api` module — no runtime-specific imports needed
- **JDK 25 extras**: `joinUntil(Instant)` — wait for a virtual thread until a deadline (JDK 25 only)

## Usage

Applications should depend on the API module and add the implementation module that matches the target runtime to the classpath.

```kotlin
import io.bluetape4k.concurrent.virtualthread.VirtualThreads

// Create a virtual thread executor
val executor = VirtualThreads.executorService()

// Start a single virtual thread
val thread = VirtualThreads.newThread {
    // runs on a virtual thread
    println("Hello from virtual thread!")
}
thread.start()

// Check if virtual threads are supported at runtime
if (VirtualThreads.isSupported()) {
    println("Virtual threads available")
}
```

### Gradle Dependency

```kotlin
// API only (compile time)
implementation("io.github.bluetape4k:bluetape4k-virtualthread-api:${version}")

// Runtime implementation (add the one matching your JDK)
runtimeOnly("io.github.bluetape4k:bluetape4k-virtualthread-jdk21:${version}")
// or
runtimeOnly("io.github.bluetape4k:bluetape4k-virtualthread-jdk25:${version}")
```

## Caution

- If you place the Java 25 implementation module on the classpath of a Java 21 runtime, you can run into class-version conflicts.
- During deployment, include only the implementation module that matches the target runtime, or split artifacts by JDK version in the deployment pipeline.
- When changing interfaces in `virtualthread-api`, always update **both** `jdk21` and
  `jdk25` implementations in the same commit.
