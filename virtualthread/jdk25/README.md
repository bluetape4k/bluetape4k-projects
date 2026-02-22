# Module bluetape4k-virtualthread-jdk25

Java 25 Virtual Thread 구현체 모듈입니다.

## 개요

이 모듈은
`bluetape4k-virtualthread-api`가 정의한 인터페이스를 Java 25 기준으로 구현합니다. ServiceLoader를 통해 런타임에 자동으로 로드되며, JDK 25 이상 환경에서 활성화됩니다.

JDK 21 구현체보다 높은 우선순위(`priority = 25`)를 가지므로, JDK 25 환경에서는 이 구현체가 자동으로 선택됩니다.

## 주요 구현체

### Jdk25VirtualThreadRuntime

Java 25의 Virtual Thread API를 사용하여 `VirtualThreadRuntime` 인터페이스를 구현합니다.

```java
public final class Jdk25VirtualThreadRuntime implements VirtualThreadRuntime {
    @Override
    public String getRuntimeName() {
        return "jdk25";
    }

    @Override
    public int getPriority() {
        return 25;  // JDK 21 구현체보다 높은 우선순위
    }

    @Override
    public boolean isSupported() {
        return Runtime.version().feature() >= 25;
    }

    @Override
    public ThreadFactory threadFactory(String prefix) {
        return Thread.ofVirtual().name(prefix, 0).factory();
    }

    @Override
    public ExecutorService executorService() {
        return Executors.newThreadPerTaskExecutor(threadFactory("vt25-"));
    }
}
```

### Jdk25StructuredTaskScopeProvider

Java 25의 `StructuredTaskScope` API를 사용하여 구조화된 동시성을 지원합니다.

```kotlin
class Jdk25StructuredTaskScopeProvider: StructuredTaskScopeProvider {
    override val providerName = "jdk25"
    override val priority = 25

    override fun isSupported(): Boolean {
        return Runtime.version().feature() >= 25
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
io.bluetape4k.concurrent.virtualthread.jdk25.Jdk25VirtualThreadRuntime
```

*src/main/resources/META-INF/services/io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeProvider*

```
io.bluetape4k.concurrent.virtualthread.jdk25.Jdk25StructuredTaskScopeProvider
```

## 빌드 설정

이 모듈은 Java 25 Toolchain을 사용하여 빌드됩니다.

```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
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

    // JDK 25 구현체 (JDK 25 환경에서 사용)
    runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk25:$version")
}
```

## 사용 예시

이 모듈은 런타임에 자동으로 로드되므로, API 모듈만 사용하면 됩니다.

```kotlin
import io.bluetape4k.concurrent.virtualthread.VirtualThreads
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes

fun main() {
    // JDK 25 환경에서 실행 시 자동으로 Jdk25VirtualThreadRuntime 사용
    println("Runtime: ${VirtualThreads.runtimeName()}") // "jdk25"

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
        val task1 = scope.fork { fetchDataFromApi1() }
        val task2 = scope.fork { fetchDataFromApi2() }
        val task3 = scope.fork { fetchDataFromApi3() }

        scope.join().throwIfFailed { error ->
            logger.error { "Task failed: ${error.message}" }
        }

        Triple(task1.get(), task2.get(), task3.get())
    }
}
```

## JDK 25의 개선사항

Java 25에서는 Virtual Thread와 Structured Concurrency에 다음과 같은 개선사항이 포함될 수 있습니다:

### Virtual Thread 성능 최적화

- Carrier Thread 스케줄링 개선
- Pinning 감소 및 최적화
- 메모리 사용량 최적화

### Structured Concurrency 안정화

- API 안정화 (Preview에서 Final로 전환 가능)
- 더 나은 예외 처리 및 에러 전파
- Scoped Values 통합 개선

**참고**: Java 25 전용 최적화가 필요한 경우, 이 클래스에서 구현할 수 있습니다.

## 테스트

```kotlin
class Jdk25VirtualThreadRuntimeTest {
    private val runtime = Jdk25VirtualThreadRuntime()

    @Test
    fun `should be supported on JDK 25+`() {
        runtime.isSupported() shouldBe true
        runtime.runtimeName shouldBe "jdk25"
        runtime.priority shouldBe 25
    }

    @Test
    fun `should have higher priority than JDK 21`() {
        val jdk21Priority = 21
        runtime.priority shouldBeGreaterThan jdk21Priority
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
| JDK 21    | ⚠️    | 클래스 버전 충돌 가능 (권장하지 않음)          |
| JDK 25    | ✅     | 자동 활성화 (최우선 선택)                 |

## 우선순위 기반 선택

ServiceLoader는 여러 구현체가 있을 경우, 우선순위가 높은 것을 선택합니다:

```kotlin
// JDK 25 환경에서 두 구현체가 모두 classpath에 있을 경우
VirtualThreads.runtimeName() // "jdk25" (priority 25 > 21)

// 우선순위 비교
Jdk25VirtualThreadRuntime.priority = 25  // ← 선택됨
Jdk21VirtualThreadRuntime.priority = 21
```

## 주의사항

### JDK 버전과 구현체 매칭

JDK 25 환경이 아닌 곳에서 이 모듈을 포함하면 불필요한 의존성이 추가됩니다.

```kotlin
// ✅ 올바른 사용 (JDK 25 환경)
dependencies {
    runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk25:$version")
}

// ⚠️ 권장하지 않음 (JDK 21 환경에서 JDK 25 모듈 포함)
// JDK 21에서는 isSupported()가 false를 반환하여 사용되지 않음
dependencies {
    runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk25:$version")
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

## 마이그레이션 가이드

### JDK 21에서 JDK 25로 업그레이드

```kotlin
// 1. build.gradle.kts 의존성 변경
dependencies {
    // runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk21:$version")
    runtimeOnly("io.bluetape4k:bluetape4k-virtualthread-jdk25:$version")
}

// 2. JDK 25 설치 및 JAVA_HOME 변경

// 3. 애플리케이션 재빌드 및 테스트
    ./ gradlew clean build

// 4. 런타임 확인
        VirtualThreads.runtimeName() // "jdk25"
```

코드 변경은 필요 없습니다! API 모듈을 사용하는 모든 코드는 그대로 동작합니다.

## 참고 자료

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 462: Structured Concurrency (Second Preview)](https://openjdk.org/jeps/462)
- [Java 25 Release Notes](https://www.oracle.com/java/technologies/javase/25-relnote-issues.html)
- [Project Loom](https://openjdk.org/projects/loom/)
