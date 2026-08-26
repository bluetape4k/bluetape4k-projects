# Epic #1422 #1353 Redisson LocalCachedMap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `RLocalCachedMap`의 Int/Double `addAndGetAsync`와 두 client remote invalidation을 실행 가능한 Redisson coroutine example과 회귀 테스트로 증명한다.

**Architecture:** numeric map은 value type과 map 이름을 분리하고 String key + numeric value의 concrete `CompositeCodec`를 front/back view에 동일하게 전달한다. shared Redis server/client는 기존 base와 `ShutdownQueue`가 소유하며, invalidation용 test-owned client만 명시적으로 만들고 `@AfterAll`에서 한 번 닫는다. 모든 실제 Redis 호출은 `runSuspendIO`와 bounded future await를 사용한다.

**Tech Stack:** Kotlin 2.4/JVM 25, Redisson 4.7.0, `RLocalCachedMap`, `CompositeCodec`, `RedissonCodecs.Int`/`Double`, JUnit 5, `runSuspendIO`, `SuspendedJobTester`, Awaitility Kotlin, `bluetape4k-assertions`, Testcontainers Redis.

**Approved basis:** `docs/superpowers/specs/2026-08-26-epic-1422-executable-examples-design.md` at commit `7d22431a975e12a237083c93d6e2e6749f966b9d`, based on `origin/develop` `a907d144f39bfb94cba783cf65a5412e0714e9d5`.

---

## 계획 범위와 파일 소유권

- Modify: `examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/AbstractRedissonCoroutineTest.kt` — `registerShutdown` 선택 인자로 test-owned client lifecycle을 허용한다.
- Modify: `examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/collections/LocalCachedMapExamples.kt` — Int/Double numeric example과 concrete codec을 추가한다.
- Modify: `examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/collections/LocalCachedMapTest.kt` — concurrent increment, invalidation, timeout, negative contract를 추가한다.
- Modify: `examples/redisson-demo/README.md` — 정확한 task, Redis/Testcontainers precondition과 eventual consistency를 영어로 갱신한다.
- Modify: `examples/redisson-demo/README.ko.md` — 같은 명령과 계약을 한국어로 갱신한다.

`examples/redisson-demo/build.gradle.kts`는 이미 `:bluetape4k-testcontainers`, `:bluetape4k-junit5`, `:bluetape4k-redisson`과 Redisson dependency를 선언하므로 변경하지 않는다. Kafka child가 소유하는 `.github/workflows/examples.yml`도 변경하지 않는다.

## Codec 재사용 결정

두 test source가 각각 `Int`/`Double` 제네릭 타입을 추론해야 하므로 file-local
`CompositeCodec` 상수는 각 파일에 둔다. 값은 새 serializer나 wrapper를 만들지
않고 공통 `RedissonCodecs.String`, `.Int`, `.Double`을 동일한 순서로 조합한다.
production module에 테스트 전용 helper를 추출하지 않는 것이 이 examples 범위의
중복·의존성 경계를 지키는 선택이다.

## Task 1: test-owned Redisson client lifecycle을 먼저 고정한다

**Files:**

- Modify: `examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/AbstractRedissonCoroutineTest.kt`

- [ ] **Step 1: `newRedisson`에 shutdown registration 경계를 추가한다**

기존 `redis` lazy property가 사용하는 mutable tag 대신 다음 immutable image reference를
사용한다. `RedisServer`의 기존 `DockerImageName` 생성자와 `ShutdownQueue` ownership을
그대로 재사용하며, production testcontainers module은 변경하지 않는다. 현재
`RedisServer.Launcher.redis`가 Redisson classpath에서 수행하는 `warmupPubSubChannel`
호출도 반드시 보존해 첫 연결 시 `StacklessClosedChannelException`을 예방한다.

```kotlin
@JvmStatic
val redis: RedisServer by lazy {
    RedisServer(
        DockerImageName.parse(
            "redis@sha256:4e070415a5713188624f93815e62d6c6a1fcbb416d2e0b578ab3db627db3a93a"
        )
    ).apply {
        start()
        ShutdownQueue.register(this)

        if (classIsPresent("org.redisson.Redisson")) {
            val warmupClient = Redisson.create(
                RedisServer.Launcher.RedissonLib.getRedissonConfig(url)
            )
            try {
                RedisServer.Launcher.RedissonLib.warmupPubSubChannel(warmupClient)
            } finally {
                warmupClient.shutdown()
            }
        }
    }
}
```

필요한 import는 `io.bluetape4k.support.classIsPresent`, `org.redisson.Redisson`와
`org.testcontainers.utility.DockerImageName`다. 기존 함수의
signature는 다음으로 바꾸고, `ShutdownQueue.register`는 조건부로 실행한다. 기본값은
기존 shared client 동작을 보존한다.

```kotlin
@JvmStatic
protected fun newRedisson(registerShutdown: Boolean = true): RedissonClient {
    val config = Config().apply {
        useSingleServer()
            .setAddress(redis.url)
            .setConnectionPoolSize(128)
            .setConnectionMinimumIdleSize(32)
            .setIdleConnectionTimeout(100_000)
            .setTimeout(5000)
            .setRetryAttempts(3)
            .setRetryDelay { attempt -> Duration.ofMillis((attempt + 1) * 100L) }
            .setDnsMonitoringInterval(5000)
        executor = VirtualThreadExecutor
        threads = 256
        nettyThreads = 128
        codec = RedissonCodecs.LZ4ForyComposite
        setTcpNoDelay(true)
        setTcpUserTimeout(5000)
    }

    return Redisson.create(config).also { client ->
        if (registerShutdown) {
            ShutdownQueue.register { client.shutdown() }
        }
    }
}
```

반환 타입은 기존 호출자 호환성을 위해 `RedissonClient`를 유지한다. `redissonClient` lazy property는 인자를 생략해 shared client로 남긴다.

같은 base file에 다음 bounded await helper도 Task 1에서 먼저 추가한다. 필요한
import는 `org.redisson.api.RFuture`, `kotlinx.coroutines.CancellationException`,
`kotlinx.coroutines.TimeoutCancellationException`, `kotlinx.coroutines.future.await`,
`kotlinx.coroutines.withTimeout`, `kotlin.time.Duration.Companion.seconds`다.

```kotlin
protected suspend fun <T> awaitRedis(
    future: RFuture<T>,
    timeout: kotlin.time.Duration = 5.seconds,
): T = try {
    withTimeout(timeout) { future.await() }
} catch (cause: TimeoutCancellationException) {
    future.cancel(false)
    throw cause
} catch (cause: CancellationException) {
    future.cancel(false)
    throw cause
}
```

Task 2부터 이 helper를 사용할 수 있도록 Task 1의 compile gate가 helper까지
포함하는지 확인한다.

- [ ] **Step 2: 기존 Redisson example 전체 compile을 확인한다**

Run:

```bash
./gradlew :bluetape4k-examples-redisson-demo:testClasses --no-configuration-cache
```

Expected: 기존 호출부가 default `registerShutdown=true`로 컴파일되고 `BUILD SUCCESSFUL`이다.

- [ ] **Step 3: lifecycle 변경을 독립 commit으로 기록한다**

```bash
git add examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/AbstractRedissonCoroutineTest.kt
git commit -F - <<'EOF'
Redisson 예제의 shared와 test-owned client 종료 경계를 분리한다

Constraint: 기존 shared redissonClient와 ShutdownQueue 등록 동작은 기본값으로 보존한다.
Rejected: 모든 client를 전역 ShutdownQueue에 등록 | @AfterAll 단일 종료와 setup 실패 cleanup을 증명할 수 없다.
Confidence: high
Scope-risk: narrow
Directive: local-cache invalidation test만 registerShutdown=false를 명시한다.
Tested: ./gradlew :bluetape4k-examples-redisson-demo:testClasses --no-configuration-cache
Not-tested: numeric codec round-trip, invalidation, hosted CI
EOF
```

## Task 2: numeric codec와 atomic update RED 테스트를 작성한다

**Files:**

- Modify: `examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/collections/LocalCachedMapExamples.kt`
- Modify: `examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/collections/LocalCachedMapTest.kt`

- [ ] **Step 1: concrete codec와 map construction을 선언한다**

두 파일에서 다음 import와 value를 사용한다.

```kotlin
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import org.redisson.codec.CompositeCodec

private val intCodec = CompositeCodec(
    RedissonCodecs.String,
    RedissonCodecs.Int,
    RedissonCodecs.Int,
)
private val doubleCodec = CompositeCodec(
    RedissonCodecs.String,
    RedissonCodecs.Double,
    RedissonCodecs.Double,
)
```

`LocalCachedMapOptions.name<String, Int>(name).codec(intCodec)`와 `LocalCachedMapOptions.name<String, Double>(name).codec(doubleCodec)`를 사용하고, remote view도 `redisson.getMap(name, intCodec)` 또는 `redisson.getMap(name, doubleCodec)`로 같은 codec을 전달한다.

- [ ] **Step 2: RED assertion을 추가한다**

```kotlin
@Test
fun `empty Int key is initialized by addAndGetAsync`() = runSuspendIO {
    val name = randomName()
    val map = redisson.getLocalCachedMap(
        LocalCachedMapOptions.name<String, Int>(name).codec(intCodec)
    )

    val result = awaitRedis(map.addAndGetAsync("count", 32))

    result shouldBeEqualTo 32
    awaitRedis(map.getAsync("count")) shouldBeEqualTo 32
}

@Test
fun `empty Double key is initialized by HINCRBYFLOAT`() = runSuspendIO {
    val name = randomName()
    val map = redisson.getLocalCachedMap(
        LocalCachedMapOptions.name<String, Double>(name).codec(doubleCodec)
    )

    val result = awaitRedis(map.addAndGetAsync("ratio", 0.25))

    result shouldBeEqualTo 0.25
    awaitRedis(map.getAsync("ratio")) shouldBeEqualTo 0.25
}
```

모든 future await는 기본 5초 deadline과 cancellation 전파를 제공하는
`awaitRedis`로 실행하고, assertion은 `bluetape4k-assertions`의 `shouldBeEqualTo`를
사용한다.

- [ ] **Step 3: RED 상태를 확인한다**

Run:

```bash
./gradlew :bluetape4k-examples-redisson-demo:test \
  --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapExamples' \
  --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapTest' \
  --no-configuration-cache --max-workers=1
```

Expected: concrete codec map과 numeric implementation이 아직 없거나 기존 임시 구현 경로를 사용하므로 새 test가 실패한다. Docker startup failure는 별도 환경 evidence로 분리한다.

## Task 3: LocalCachedMapExamples에 Int/Double 실행 예제를 구현한다

**Files:**

- Modify: `examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/collections/LocalCachedMapExamples.kt`

- [ ] **Step 1: Int 예제를 빈 key atomic increment로 교체한다**

기존 `fastPutAsync` numeric map은 일반 직렬화 동작 설명으로 유지하되 기존 임시 주석은 제거한다. 별도 unique map에서 다음 순서를 사용한다.

```kotlin
val name = randomName()
val options = LocalCachedMapOptions.name<String, Int>(name)
    .cacheSize(100)
    .codec(intCodec)
val cachedMap: RLocalCachedMap<String, Int> = redisson.getLocalCachedMap(options)
val backendMap: RMap<String, Int> = redisson.getMap(name, intCodec)

val first = awaitRedis(cachedMap.addAndGetAsync("count", 32))
val second = awaitRedis(cachedMap.addAndGetAsync("count", 10))

first shouldBeEqualTo 32
second shouldBeEqualTo 42
awaitRedis(backendMap.getAsync("count")) shouldBeEqualTo 42
```

`fastPutAsync`로 같은 numeric key를 먼저 직렬화하지 않는다. key가 없을 때 Redis
`HINCRBY` 경로가 초기값을 만들고, Double은 `HINCRBYFLOAT` 경로를 사용한다는 점을
KDoc와 assertion으로 설명한다.

- [ ] **Step 2: Double 예제와 제약 KDoc을 추가한다**

```kotlin
val name = randomName()
val options = LocalCachedMapOptions.name<String, Double>(name)
    .cacheSize(100)
    .codec(doubleCodec)
val cachedMap: RLocalCachedMap<String, Double> = redisson.getLocalCachedMap(options)
val backendMap: RMap<String, Double> = redisson.getMap(name, doubleCodec)

val result = awaitRedis(cachedMap.addAndGetAsync("ratio", 0.25))
result shouldBeEqualTo 0.25
awaitRedis(backendMap.getAsync("ratio")) shouldBeEqualTo 0.25
```

KDoc에는 Int/Double map을 섞지 않고 finite Double delta만 사용하며, `HINCRBYFLOAT`가 숫자 hash field를 요구한다는 제약을 한국어로 기록한다.

- [ ] **Step 3: example tests를 GREEN으로 확인한다**

```bash
./gradlew :bluetape4k-examples-redisson-demo:test \
  --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapExamples' \
  --no-configuration-cache --max-workers=1
```

Expected: Redis Testcontainers가 실행 가능한 환경에서 Int/Double round-trip이 PASS하고, 각 future가 5초 deadline 안에 완료된다.

## Task 4: 두 client 동시성·remote invalidation·negative contract를 구현한다

**Files:**

- Modify: `examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/collections/LocalCachedMapTest.kt`

- [ ] **Step 1: setup에서 test-owned clients를 만든다**

`@BeforeAll`의 기존 `newRedisson()` 호출을 다음으로 바꾸고, `@AfterAll`에서 초기화된
client만 한 번 닫는다.

이 코드 블록에는 `java.util.concurrent.TimeUnit` import를 추가한다.

```kotlin
@BeforeAll
fun setup() {
    redisson1 = newRedisson(registerShutdown = false)
    redisson2 = newRedisson(registerShutdown = false)
}

@AfterAll
fun cleanup() {
    var firstFailure: Throwable? = null
    if (this::redisson1.isInitialized) {
        runCatching { redisson1.shutdown(0, 5, TimeUnit.SECONDS) }
            .onFailure { firstFailure = it }
    }
    if (this::redisson2.isInitialized) {
        runCatching { redisson2.shutdown(0, 5, TimeUnit.SECONDS) }
            .onFailure { failure ->
                if (firstFailure == null) firstFailure = failure
                else firstFailure!!.addSuppressed(failure)
            }
    }
firstFailure?.let { throw it }
}
```

모든 Redisson `RFuture`는 Task 1에서 추가한 bounded helper를 통해 await한다.
Task 2–4의 raw future await 표기는 구현 시 `awaitRedis(future)`로 치환해
timeout/cancellation 때 underlying future도 취소한다. helper 자체는 `runSuspendIO`의
IO dispatcher에서 호출하며, 테스트
종료 시점의 client shutdown과 별개로 pending operation을 남기지 않는다.

두 options와 `backCache`는 같은 concrete codec과 unique `cacheName`을 공유하고,
shared `redisson`는 base의 `ShutdownQueue` 소유권을 유지한다. Redisson의
`shutdown(0, 5, TimeUnit.SECONDS)` overload를 사용해 각 test-owned client가
5초 bounded shutdown을 갖고, 첫 번째 shutdown 예외가 발생하면 두 번째 client도
정리한 뒤 테스트 lifecycle failure로 보고한다.

기존 `options1`, `options2`의 LFU·maxIdle·timeToLive 동작은 보존하고 value codec만
명시한다. `backCache`에도 같은 codec을 전달한다.

```kotlin
private val options1 = LocalCachedMapOptions.name<String, Int>(cacheName)
    .cacheSize(100)
    .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
    .maxIdle(10.seconds.toJavaDuration())
    .timeToLive(5.seconds.toJavaDuration())
    .codec(intCodec)
private val options2 = LocalCachedMapOptions.name<String, Int>(cacheName)
    .cacheSize(100)
    .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
    .maxIdle(10.seconds.toJavaDuration())
    .timeToLive(5.seconds.toJavaDuration())
    .codec(intCodec)
private val backCache: RMap<String, Int> by lazy { redisson.getMap(cacheName, intCodec) }
```

이 클래스의 기존 Redis/Testcontainers 세 테스트 wrapper는 모두
`runSuspendIO`로 바꾸고, `io.bluetape4k.junit5.awaitility.untilSuspending`을
import해 suspend Redis future를 IO 경계 안에서 평가한다. `runTest`와 동기
`containsKey`를 남기지 않는다.

각 기존 테스트 선언의 정확한 변경은 `runTest` wrapper를
`runSuspendIO(timeout = 60.seconds)`로 교체하고,
`fastPutAsync`·`fastRemoveAsync`·`getAsync`·`containsKeyAsync`의 반환
`RFuture`를 `awaitRedis(future)` 또는 `untilSuspending` 내부의 bounded await로
소비하는 것이다.

- [ ] **Step 2: concurrent Int/Double increment를 `SuspendedJobTester`로 검증한다**

```kotlin
@Test
fun `concurrent numeric increments match independent remote final value`() = runSuspendIO {
    val name = randomName()
    val map = redisson1.getLocalCachedMap(
        LocalCachedMapOptions.name<String, Int>(name).codec(intCodec)
    )
    val calls = 32 * 8 // rounds는 전체 호출 수이고 workers는 동시 실행 수다.

    withTimeout(30.seconds) {
        SuspendedJobTester()
            .workers(4)
            .rounds(32 * 8)
            .add { awaitRedis(map.addAndGetAsync("count", 1)) }
            .run()
    }

    val remote = redisson.getMap<String, Int>(name, intCodec)
    awaitRedis(remote.getAsync("count")) shouldBeEqualTo calls

    val map2 = redisson2.getLocalCachedMap(
        LocalCachedMapOptions.name<String, Int>(name).codec(intCodec)
    )
    awaitRedis(map.getAsync("count")) shouldBeEqualTo calls
    awaitRedis(map2.getAsync("count")) shouldBeEqualTo calls
}
```

같은 구조로 Double은 `0.25` delta와 `Double` codec을 사용하고, expected 값은
`calls * 0.25`로 assertion한다. Double도 `redisson2.getLocalCachedMap`으로
동일한 `name`과 `doubleCodec`을 사용한 두 번째 local view를 만들고,
`awaitRedis(map2.getAsync("ratio"))`가
`calls * 0.25`와 일치하는지 확인한다. Int와 Double 모두 `redisson2` local
view를 같은 5초 deadline으로 reread해 remote final value와 일치하는지
확인한다. worker는 ad hoc thread를 만들지 않는다.

Double 경로의 핵심은 다음처럼 Int와 분리된 map과 동일한 front/back codec을
사용하는 것이다.

```kotlin
val name = randomName()
val calls = 32 * 8
val doubleMap1 = redisson1.getLocalCachedMap(
    LocalCachedMapOptions.name<String, Double>(name).codec(doubleCodec)
)
val doubleMap2 = redisson2.getLocalCachedMap(
    LocalCachedMapOptions.name<String, Double>(name).codec(doubleCodec)
)
withTimeout(30.seconds) {
    SuspendedJobTester()
        .workers(4)
        .rounds(32 * 8)
        .add { awaitRedis(doubleMap1.addAndGetAsync("ratio", 0.25)) }
        .run()
}
val remoteDouble = redisson.getMap<String, Double>(name, doubleCodec)
awaitRedis(remoteDouble.getAsync("ratio")) shouldBeEqualTo calls * 0.25
awaitRedis(doubleMap2.getAsync("ratio")) shouldBeEqualTo calls * 0.25
```
worker는 ad hoc thread를 만들지 않는다.

- [ ] **Step 3: remote put/remove invalidation을 bounded suspend Awaitility로 검증한다**

기존 세 invalidation 테스트를 보존하면서 각 wrapper를 `runSuspendIO`로 바꾸고,
모든 future를 `awaitRedis(future)`로 감싼다. remote `fastPutAsync`/`fastRemoveAsync`
뒤에는 blocking `containsKey` 대신 `untilSuspending`에서
`awaitRedis(containsKeyAsync(key))`를 IO 경계로 평가한다. 다음 조건을
`atMost(5.seconds)`, 100ms poll로 기다린다.

```kotlin
await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100))
    .untilSuspending {
        awaitRedis(frontCache1.containsKeyAsync(key)) && awaitRedis(frontCache2.containsKeyAsync(key))
    }
awaitRedis(frontCache1.getAsync(key)) shouldBeEqualTo 42
awaitRedis(frontCache2.getAsync(key)) shouldBeEqualTo 42

await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100))
    .untilSuspending {
        !awaitRedis(frontCache1.containsKeyAsync(key)) && !awaitRedis(frontCache2.containsKeyAsync(key))
    }
awaitRedis(frontCache1.getAsync(key)).shouldBeNull()
awaitRedis(frontCache2.getAsync(key)).shouldBeNull()
```

remote 변경 직후 stale read를 허용하되 bounded await 뒤 두 local view가 갱신/삭제된
값을 읽는지 확인한다.

- [ ] **Step 4: unsupported numeric value의 명시적 negative test를 추가한다**

unique map에 String codec으로 `"not-a-number"`를 저장한 뒤 Double composite codec
view에서 `addAndGetAsync`를 호출하고 Redisson의 `RedisException`을 기대한다. 예외
message는 비교하지 않아 환경별 Redis endpoint를 노출하지 않는다.

```kotlin
@Test
fun `non numeric stored value is rejected by numeric increment`() = runSuspendIO {
    val name = randomName()
    val raw = redisson.getMap<String, String>(name, RedissonCodecs.String)
    awaitRedis(raw.fastPutAsync("ratio", "not-a-number"))

    val numeric = redisson1.getLocalCachedMap(
        LocalCachedMapOptions.name<String, Double>(name).codec(doubleCodec)
    )
    assertFailsWith<RedisException> {
        awaitRedis(numeric.addAndGetAsync("ratio", 0.25))
    }
}
```

예외 타입이 현재 Redisson client 계약과 다르면 실제 failure cause를 확인하고
`RedisException`의 정확한 subtype으로 test와 KDoc를 함께 갱신한다.

- [ ] **Step 5: RED 테스트를 GREEN으로 확인한다**

```bash
./gradlew :bluetape4k-examples-redisson-demo:test \
  --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapTest' \
  --no-configuration-cache --max-workers=1
```

Expected: concurrent final value, both-client invalidation, 5초 future/await deadline,
negative `RedisException` test가 PASS한다. Redis/Testcontainers startup failure는
코드 결함과 분리해 evidence로 기록한다.

## Task 5: Redisson README locale parity를 맞춘다

**Files:**

- Modify: `examples/redisson-demo/README.md`
- Modify: `examples/redisson-demo/README.ko.md`

- [ ] **Step 1: LocalCachedMap 행과 numeric contract를 갱신한다**

두 README의 `LocalCachedMapExamples.kt` 설명에 Int/Double map 분리, 동일 `CompositeCodec`, `HINCRBYFLOAT`, 두 client eventual invalidation을 추가한다. 실행 섹션에는 다음 두 명령을 그대로 넣는다.

```bash
./gradlew :bluetape4k-examples-redisson-demo:test \
  --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapExamples' \
  --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapTest' \
  --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-examples-redisson-demo:test \
  --no-configuration-cache --max-workers=1
```

Docker daemon/Testcontainers가 필요하고 dynamic port를 사용한다는 점, invalidation 직후 stale read는 허용되지만 5초 bounded await 뒤 local reread가 갱신값 또는 `null`을 확인한다는 점을 두 locale에 같은 의미로 기록한다.

- [ ] **Step 2: README parity를 검사한다**

```bash
git diff --check
for file in examples/redisson-demo/README.md examples/redisson-demo/README.ko.md; do
  rg -F -- "bluetape4k-examples-redisson-demo:test" "$file"
  rg -F -- "LocalCachedMapExamples" "$file"
  rg -F -- "LocalCachedMapTest" "$file"
  rg -F -- "--no-configuration-cache" "$file"
  rg -F -- "--max-workers=1" "$file"
  rg -F -- "CompositeCodec" "$file"
  rg -F -- "HINCRBYFLOAT" "$file"
done
python3 - <<'PY'
from pathlib import Path

english = Path("examples/redisson-demo/README.md").read_text()
korean = Path("examples/redisson-demo/README.ko.md").read_text()
required = (
    ":bluetape4k-examples-redisson-demo:test",
    "LocalCachedMapExamples",
    "LocalCachedMapTest",
    "--no-configuration-cache",
    "--max-workers=1",
    "CompositeCodec",
    "HINCRBYFLOAT",
)
assert all(token in english and token in korean for token in required)
PY
```

Expected: 두 locale의 task name, test class와 numeric/invalidation 설명이 일치한다.

## Task 6: child-level verification과 module 6-R review를 만든다

**Files:**

- Create: `docs/superpowers/reviews/2026-08-26-epic-1422-1353-module-6r.md`

- [ ] **Step 1: targeted와 full module test를 순차 실행한다**

```bash
./gradlew :bluetape4k-examples-redisson-demo:test \
  --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapExamples' \
  --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapTest' \
  --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-examples-redisson-demo:test \
  --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-examples-redisson-demo:detekt \
  --no-configuration-cache --max-workers=1
git diff --check
```

Expected: 세 명령이 모두 PASS하고 Testcontainers 종료·test-owned client shutdown이 확인된다. failure가 발생하면 전체 suite를 재실행하기 전에 해당 test를 동일 조건으로 재현한다.

- [ ] **Step 2: 6-R evidence를 기록한다**

검토 문서에는 touched file, 실제 command/output, `CompositeCodec` front/back 동일성, `runSuspendIO`·future timeout, `SuspendedJobTester`, Awaitility bounded polling, test-owned/shared ownership, negative `RedisException`, known gaps를 기록한다. six perspective 결과가 P0=0/P1=0이 될 때까지 수정하고 한국어 용어 audit를 실행한다.

- [ ] **Step 3: child PR merge-ready evidence를 parent head에 연결한다**

parent #1347 merge 전 #1353 child의 base/head SHA와 temporary base ref를 기록한다. parent merge 후 child base를 `develop` branch로 retarget하고 exact diff, fresh CI, reviews/threads를 다시 확인한다. child PR body의 마지막은 한국어 `## DoD Status`로 끝내며 `Closes #1353`와 최종 child에서만 `Closes #1422`를 사용한다.

## Redisson plan traceability

| 명세 acceptance | 계획 task | 증거 |
|---|---|---|
| Int/Double atomic increment | 2, 3 | empty-key `addAndGetAsync` round-trip |
| `HINCRBYFLOAT` numeric codec | 2, 3, 4 | concrete composite codec와 remote read |
| concurrent remote final value | 4 | `SuspendedJobTester(workers=4, rounds=32*8)` |
| two-client invalidation | 4 | Awaitility 5초/100ms와 local reread |
| bounded cancellation/cleanup | 1, 4, 6 | Future 5초, `@AfterAll`, ownership evidence |
| unsupported/mismatched input | 4 | `RedisException` negative test |
| README locale parity | 5 | exact task/name diff |
| child module 6-R | 6 | review artifact와 P0/P1 table |

## Rollback과 stop condition

numeric example/test가 실패하면 Task 2–4 commit을 되돌리고 기존 `fastPutAsync` map example을 유지한다. `newRedisson(registerShutdown = false)`가 기존 test에 영향을 주면 signature 변경만 revert하고 shared default 동작을 복구한다. Redis future timeout, test-owned client leak, invalidation timeout, exact-head CI failure 또는 unresolved P1이면 merge-ready를 중단하고 해당 test·ownership·`## DoD Status`를 다시 검증한다. Redisson codec version, production API, 새 dependency를 추가하지 않는다.

unique map은 Testcontainers Redis 수명과 함께 폐기되며, 테스트 중간에 별도
delete를 호출하지 않아 invalidation 관찰을 오염시키지 않는다. 장시간 shared Redis의
잔여 key 정리가 필요해지면 별도 운영 이슈로 다루고 이 child의 numeric 계약에는
추가하지 않는다.

## 계획 완료 조건

- 모든 checkbox가 실제 commit과 fresh command evidence로 완료된다.
- Redisson child 6-R의 최신 P0/P1이 0이다.
- Int/Double map의 front/back codec이 동일하고 invalidation이 bounded reread로 증명된다.
- README locale, child issue, PR `## DoD Status`가 실제 test behavior와 일치한다.
