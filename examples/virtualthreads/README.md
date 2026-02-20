# Examples - Java 21 Virtual Threads

Java 21의 Virtual Threads를 효과적으로 사용하기 위한 모범 사례와 규칙을 학습하는 예제 모음입니다.

## 예제 목록

### Virtual Threads 사용 규칙

| 예제 파일                                              | 규칙         | 설명                          |
|----------------------------------------------------|------------|-----------------------------|
| `Rule2RunBlockingSynchronousCode.kt`               | **Rule 2** | 동기 코드를 비동기 방식으로 실행          |
| `Rule3DoNotPooledVirtualThreads.kt`                | **Rule 3** | Virtual Thread 풀링 금지        |
| `Rule4UseSemaphoreInsteadOfFixedThreadPool.kt`     | **Rule 4** | 고정 스레드 풀 대신 Semaphore 사용    |
| `Rule5UseThreadLocalCarefully.kt`                  | **Rule 5** | ThreadLocal 신중하게 사용         |
| `Rule6UseSynchronizedBlocksAndMethodsCarefully.kt` | **Rule 6** | synchronized 블록/메서드 신중하게 사용 |

## 주요 학습 포인트

### Rule 2: 동기 코드 실행 방식 선택

```kotlin
// CPU 집약적 작업 → Platform Thread + CompletableFuture
CompletableFuture.supplyAsync { cpuIntensiveTask() }

// I/O 집약적 작업 → Virtual Thread
Executors.newVirtualThreadPerTaskExecutor().use { executor ->
    executor.submit { ioTask() }
}

// 또는 Kotlin Coroutines + Virtual Thread Dispatcher
runSuspendTest(Dispatchers.VT) {
    async { ioTaskAwait() }
}
```

### Rule 3: Virtual Thread 풀링 금지

```kotlin
// ❌ 잘못된 방식
val pool = Executors.newFixedThreadPool(100)  // Virtual Thread 풀 생성 금지

// ✅ 올바른 방식
val executor = Executors.newVirtualThreadPerTaskExecutor()
```

### Rule 4: Semaphore로 동시성 제어

```kotlin
// ❌ 잘못된 방식
val pool = Executors.newFixedThreadPool(10)

// ✅ 올바른 방식
val semaphore = Semaphore(10)
Executors.newVirtualThreadPerTaskExecutor().use { executor ->
    semaphore.acquire()
    try { task() } finally { semaphore.release() }
}
```

### Rule 5: ThreadLocal 주의사항

Virtual Thread는 많이 생성될 수 있으므로 ThreadLocal 메모리 사용에 주의해야 합니다.

### Rule 6: synchronized 블록 주의

synchronized 블록은 Virtual Thread를 차단(pinning)할 수 있습니다.

## 실행 방법

```bash
# 모든 예제 실행 (Java 21+ 필요)
./gradlew :examples:virtualthreads:test

# 특정 규칙 예제만 실행
./gradlew :examples:virtualthreads:test --tests "*Rule2*"
```

## 요구사항

- Java 21 이상
- `--enable-preview` 플래그 (Java 21에서 필요할 수 있음)

## 참고

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Virtual Threads - Baeldung](https://www.baeldung.com/java-virtual-thread)
