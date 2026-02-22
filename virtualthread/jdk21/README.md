# Module bluetape4k-virtualthread-jdk21

Java 21 Virtual Thread 구현체 모듈입니다.

## 개요

이 모듈은
`bluetape4k-virtualthread-api`가 정의한 인터페이스를 Java 21 기준으로 구현합니다. ServiceLoader를 통해 런타임에 자동으로 로드되며, JDK 21 이상 환경에서 활성화됩니다.

## 주요 구현체

### Jdk21VirtualThreadRuntime

Java 21의 Virtual Thread API를 사용하여 `VirtualThreadRuntime` 인터페이스를 구현합니다.

```java
public final class Jdk21VirtualThreadRuntime implements VirtualThreadRuntime {
    @Override
    public String getRuntimeName() {
        return "jdk21";
    }

    @Override
    public int getPriority() {
        return 21;  // JDK 25 구현체보다 낮은 우선순위
    }

    @Override
    public boolean isSupported() {
        return Runtime.version().feature() >= 21;
    }

    @Override
    public ThreadFactory threadFactory(String prefix) {
        return Thread.ofVirtual().name(prefix, 0).factory();
    }

    @Override
    public ExecutorService executorService() {
        return Executors.newThreadPerTaskExecutor(threadFactory("vt21-"));
    }
}
```

### Jdk21StructuredTaskScopeProvider

Java 21의 `StructuredTaskScope` API를 사용하여 구조화된 동시성을 지원합니다.

```kotlin
class Jdk21StructuredTaskScopeProvider: StructuredTaskScopeProvider {
    override val providerName = "jdk21"
    override val priority = 21

    override fun isSupported(): Boolean {
        return Runtime.version().feature() >= 21
    }

    override fun <T> withAll(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAll) -> T
    ): T {
        // StructuredTaskScope.ShutdownOnFailure 래퍼 구현
    }

    override fun <T> withAny(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAny<T>) -> T
    ): T {
        // StructuredTaskScope.ShutdownOnSuccess 래퍼 구현
    }
}
```

## ServiceLoader 설정

이 모듈은 다음 ServiceLoader 설정 파일을 포함합니다:

*src/main/resources/META-INF/services/io.bluetape4k.concurrent.virtualthread.VirtualThreadRuntime*

```
io.bluetape4k.concurrent.virtualthread.jdk21.Jdk21VirtualThreadRuntime
```

*src/main/resources/META-INF/services/io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeProvider*

```
io.bluetape4k.concurrent.virtualthread.jdk21.Jdk21StructuredTaskScopeProvider
```

## 빌드 설정

이 모듈은 Java 21 Toolchain을 사용하여 빌드됩니다.

```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}
```

## 의존성

### 프로젝트 의존성

```kotlin
dependencies {
    api(project(":bluetape4k-virtualthread-api"))
    implementation(project(":bluetape4k-logging"))
    implementation(Libs.kotlinx_coroutines_core)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

### Gradle 사용 예시

```kotlin
dependencies {
    // API 모듈
    implementation("io.bluetape4k:bluetape4k-virtualthread-api:$version")

    // JDK 21 구현체 (JDK 21 환경에서 사용)
    runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk21:$version")
}
```

## 사용 예시

이 모듈은 런타임에 자동으로 로드되므로, API 모듈만 사용하면 됩니다.

```kotlin
import io.bluetape4k.concurrent.virtualthread.VirtualThreads
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes

fun main() {
    // JDK 21 환경에서 실행 시 자동으로 Jdk21VirtualThreadRuntime 사용
    println("Runtime: ${VirtualThreads.runtimeName()}") // "jdk21"

    // Virtual Thread Executor 생성
    val executor = VirtualThreads.executorService()
    executor.submit {
        println("Running on: ${Thread.currentThread()}")
    }

    // Structured Concurrency 사용
    val results = StructuredTaskScopes.all(
        name = "parallel-tasks",
        factory = VirtualThreads.threadFactory()
    ) { scope ->
        val task1 = scope.fork { heavyComputation1() }
        val task2 = scope.fork { heavyComputation2() }

        scope.join().throwIfFailed()

        task1.get() to task2.get()
    }
}
```

## 테스트

```kotlin
class Jdk21VirtualThreadRuntimeTest {
    private val runtime = Jdk21VirtualThreadRuntime()

    @Test
    fun `should be supported on JDK 21+`() {
        runtime.isSupported() shouldBe true
        runtime.runtimeName shouldBe "jdk21"
        runtime.priority shouldBe 21
    }

    @Test
    fun `should create virtual thread factory`() {
        val factory = runtime.threadFactory("test-")
        val thread = factory.newThread { }

        thread.isVirtual shouldBe true
        thread.name shouldStartWith "test-"
    }

    @Test
    fun `should create executor service`() {
        val executor = runtime.executorService()
        val future = executor.submit {
            Thread.currentThread().isVirtual
        }

        future.get() shouldBe true
    }
}
```

## JDK 버전 호환성

| JDK 버전    | 지원 여부 | 활성화 조건                          |
|-----------|-------|---------------------------------|
| JDK 17 이하 | ❌     | `isSupported()` returns `false` |
| JDK 21    | ✅     | 자동 활성화 (JDK 25 구현체 없을 시)        |
| JDK 25    | ✅     | JDK 25 구현체가 우선 선택됨              |

## 주의사항

### Classpath 충돌 방지

JDK 21 환경에서 JDK 25 구현체를 함께 포함하면 클래스 버전 충돌이 발생할 수 있습니다.

```kotlin
// ❌ 잘못된 사용 (JDK 21 환경)
dependencies {
    runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk21:$version")
    runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk25:$version") // 충돌 가능
}

// ✅ 올바른 사용
dependencies {
    runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk21:$version")
}
```

### 배포 전략

프로덕션 배포 시 런타임 JDK 버전에 맞는 구현체만 포함하세요:

```kotlin
// Gradle 조건부 의존성
dependencies {
    implementation("io.bluetape4k:bluetape4k-virtualthread-api:$version")

    if (JavaVersion.current() >= JavaVersion.VERSION_25) {
        runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk25:$version")
    } else {
        runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk21:$version")
    }
}
```

## 참고 자료

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 462: Structured Concurrency (Second Preview)](https://openjdk.org/jeps/462)
- [Java 21 Release Notes](https://www.oracle.com/java/technologies/javase/21-relnote-issues.html)
