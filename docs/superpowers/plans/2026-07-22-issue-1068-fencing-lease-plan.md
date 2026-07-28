# Redis Fencing Lease Implementation Plan

> **agentic worker용:** 필수 sub-skill: 이 계획은 superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task별 구현한다. 진행 상태는 checkbox(`- [ ]`) syntax로 추적한다.

**목표:** `bluetape4k-lettuce`에 `(epoch, sequence)`로 정렬 가능한 fencing lease를 추가하고, Redis history loss·모호한 완료·취소·Cluster routing·외부 resilience 조합의 경계를 실행 가능한 test와 영문/한글 문서로 고정한다.

**아키텍처:** 한 instance가 `LettuceFencingLeaseConfig(namespace, resourceName, epoch)` 하나를 소유하고, 파생된 lease/counter 두 key만 동일 slot에서 Lua로 다룬다. public model과 result는 Java serialization invariant를 지키며, sync/`CompletableFuture`/suspend facade는 같은 validation·script·wire decoder·backend classifier를 공유한다. primitive 내부에는 retry/CB/bulkhead를 넣지 않고, 테스트와 README에서 `bluetape4k-resilience4j` decorator 및 downstream tuple CAS를 보여준다.

**기술 스택:** Kotlin 2.3, Java 21, Lettuce, Redis Lua/EVALSHA/EVAL/EVAL_RO, Kotlin Coroutines, JUnit 5, bluetape4k-assertions, bluetape4k-junit5, bluetape4k-testcontainers, bluetape4k-resilience4j, Resilience4j 2.4.0.

---

## 1. 실행 계약

### 1.1 승인된 입력과 범위

- 설계 기준: `docs/superpowers/specs/2026-07-22-issue-1068-fencing-lease-design.md`
- 구현 범위: `infra/lettuce`의 기존 `io.bluetape4k.redis.lettuce.lease` package와 공용 `RedisScriptRunner`의 async cancellation 보강.
- 보존 범위: 기존 `LettuceLock`, `LettuceSemaphore`, `LettuceMultiKeyLease` API와 동작, module registration, publication topology, dependency catalog.
- 새 dependency/module/benchmark framework/production PostgreSQL adapter는 추가하지 않는다.
- Testcontainers 기반 standalone과 Cluster 검증은 다른 module/worktree와 병렬 실행하지 않는다.
- 모든 새 public API KDoc은 English, `README.md`는 English, `README.ko.md`는 자연스러운 Korean으로 작성한다.

### 1.2 구현 그래프와 write scope

| Task                                          | 복잡도 | 선행 작업                | 주 write scope                                                  | 병렬 가능성                                     |
|-----------------------------------------------|-------:|--------------------------|-----------------------------------------------------------------|-------------------------------------------------|
| 1. Public value/result contract               |      M | 없음                     | `FencingLeaseValue.kt`, `FencingLeaseResult.kt`, 대응 unit test | Task 2와 파일 비중첩                            |
| 2. Async script cancellation                  |      M | 없음                     | `RedisScript.kt`, `RedisScriptTest.kt`                          | Task 1과 병렬 가능                              |
| 3. Internal protocol/validation               |      H | 1, 2                     | `LettuceFencingLeaseSupport.kt`, support test                   | 이후 모든 task의 직렬 선행                      |
| 4. Lua state machine/standalone hostile state |      H | 3                        | support와 script integration test                               | 단일 owner로 직렬 수행                          |
| 5. Sync/future/suspend facade parity          |      H | 4                        | facade 2개, contract와 facade test                              | 단일 owner로 직렬 수행                          |
| 6. Cancellation/ambiguous completion          |      H | 5                        | cancellation/fault-injection test                               | Task 7과 test file 비중첩이나 Redis 실행은 직렬 |
| 7. Concurrency/Cluster                        |      H | 5                        | concurrency/Cluster test                                        | Task 6과 작성 병렬 가능, 실행 직렬              |
| 8. Resilience/downstream/recovery examples    |      H | 5                        | resilience/recovery test                                        | Task 6·7과 작성 병렬 가능, 실행 직렬            |
| 9. KDoc/README parity                         |      M | 1–8 public contract 확정 | production KDoc, README locale, docs test                       | 코드 계약 확정 후 수행                          |
| 10. Full regression/DoD                       |      M | 1–9                      | 변경 없음                                                       | 반드시 마지막                                   |

### 1.3 위험 예측과 중단 조건

| 위험                                                               | 사전 방어                                                                              | 실패 시 중단/복구                                                                  |
|--------------------------------------------------------------------|----------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| Lua가 큰 sequence를 `number`로 바꿔 정밀도를 잃음                  | canonical decimal string 길이/사전식 비교, mutation 뒤 `GET` 반환                      | numeric conversion이 hot path에 보이면 Task 4를 중단하고 script unit test부터 수정 |
| `INCR` 이후 runtime error를 rollback으로 오해                      | 모든 expected error를 write 전 preflight, gap 허용, TTL 없는 partial lease fail-closed | partial mutation test가 분류표와 다르면 facade 작업 중단                           |
| chained future 취소가 현재 upstream으로 전파되지 않음              | EVALSHA→EVAL target 교체를 `AtomicReference`로 보존, 전환 race test                    | Task 2가 통과하기 전 future facade 구현 금지                                       |
| broad catch가 cancellation/decoder bug를 `BackendFailure`로 평탄화 | bounded cause normalizer + cancellation 우선 rethrow + allowlist classifier            | unknown exception이 result가 되면 Task 3부터 수정                                  |
| Serializable 누락 또는 singleton identity 손실                     | reflection + `ObjectStreamClass` + canonical reference identity round-trip             | public variant 하나라도 누락되면 Task 1 완료 금지                                  |
| assertion style 퇴행                                               | `bluetape4k.assertions.assertFailsWith`, intent matcher, `shouldNotBeEqualTo` 사용     | boolean assertion이나 JUnit/kotlin raw assertion 발견 시 해당 test 수정            |
| 동일 resource의 mixed epoch activation                             | deterministic control-plane harness와 lower-epoch rollback 거절                        | harness가 mixed epoch를 허용하면 문서 작업 중단                                    |
| Cluster codec routing을 String으로 추정                            | `codec.encodeKey(key)` wire bytes로 `SlotHash.getSlot` 검증                            | custom codec fixture dispatch가 관찰되면 constructor validation 수정               |
| README 예제와 실제 decorator 동작 불일치                           | 동일 helper를 실제 Redis test와 documentation test에서 실행                            | docs test 실패 시 README를 구현에 맞춰 수정, test 완화 금지                        |

### 1.4 공통 TDD/검증 규칙

- 각 task는 RED test를 먼저 추가하고 예상 원인으로 실패하는 것을 확인한 뒤 최소 production code로 GREEN을 만든다.
- `.kt` 변경 후 IDE diagnostics가 가능하면 먼저 확인하고, targeted test → `:bluetape4k-lettuce:test` → detekt 순서로 넓힌다.
- Testcontainers test는 반드시 순차 실행한다. 첫 실패 후 재실행 성공은 바로 flaky로 치부하지 않고 lifecycle/timing 원인을 기록한다.
- 새 test는 JUnit 5와 `bluetape4k-assertions`를 사용한다. 예외는 `io.bluetape4k.assertions.assertFailsWith`, suspend 전용이면 `coInvoking { } shouldThrow`를 사용한다.
- equality/ordering은 intent matcher를 사용한다. 예: `first shouldNotBeEqualTo second`; `(first == second).shouldBeFalse()` 형태를 만들지 않는다.
- 매 commit 전 `git diff --check`를 통과시키고 아래 Lore 형식을 사용한다.

```text
<intent line describing why>

Constraint: <external constraint>
Rejected: <alternative> | <reason>
Confidence: high
Scope-risk: <narrow|moderate>
Directive: <future modifier warning>
Tested: <fresh evidence>
Not-tested: <known gap>
```

## 2. Task 1 — Public value와 sealed result를 먼저 고정

**파일:**

- 생성: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseValue.kt`
- 생성: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseResult.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseValueTest.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseResultTest.kt`

### 2.1 RED — invariant, ordering, serialization test

- [ ] `FencingLeaseValueTest`에 다음 실패 contract를 작성한다.
    - config의 name은 1..128자 `[A-Za-z0-9._-]+`, epoch는 positive.
    - `FencingOwnerId.from`은 UTF-8 1..256 bytes, `random()`은 `Base58.randomString(22)` alphabet/length.
    - owner ID equality/hashCode는 raw value 기준이지만 `toString()`은 항상 `FencingOwnerId(<redacted>)`.
    - `FencingToken`은 epoch 우선, sequence 차순, equality와 natural ordering 일치, redacted `toString()`.
    - config/owner/token Java round-trip, `serialVersionUID == 1L`, crafted invalid payload의 `InvalidObjectException`.
    - config의 `namespace`/`resourceName`과 owner의 raw value를 crafted `null`로 만든 payload도 cause 없는 stable `InvalidObjectException`.
- [ ] `FencingLeaseResultTest`에 모든 enum, sealed interface, nested data class/data object sample table을 만든다.
    - 모든 non-enum public sample은 `Serializable`, `ObjectStreamClass.lookup(...).serialVersionUID == 1L`.
    - sealed interface 자체도 declared `serialVersionUID == 1L`을 가지는지 reflection으로 검증.
    - 모든 `data object` round-trip은 `restored shouldBeSameInstanceAs original`.
    - TTL variant는 negative value와 crafted serialized negative payload를 거절.
    - nested failure property를 `null`로 조작한 crafted payload도 canonical constructor 재검증에서 `InvalidObjectException`으로 거절.
    - invalid payload exception은 stable message, `cause == null`, sentinel value 비노출을 검증.
    - public property에 key/owner/raw reply/`Throwable`/message가 없음을 reflection으로 검증.
- [ ] RED 명령을 실행하고 unresolved type 때문에 실패하는지 확인한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*FencingLeaseValueTest' --tests '*FencingLeaseResultTest'
```

예상 결과: 새 type이 없어 Kotlin test compilation이 실패한다.

### 2.2 GREEN — public model 구현

- [ ] `FencingLeaseValue.kt`에 English KDoc과 함께 다음 shape를 구현한다. constructor invariant가 있는 type의 `readResolve()`는 raw invalid value나 원래 exception을 노출하지 않는다.

```kotlin
data class LettuceFencingLeaseConfig(
    val namespace: String,
    val resourceName: String,
    val epoch: Long,
) : Serializable {
    init {
        require(NAME_PATTERN.matches(namespace)) { "namespace must be 1..128 safe ASCII characters." }
        require(NAME_PATTERN.matches(resourceName)) { "resourceName must be 1..128 safe ASCII characters." }
        epoch.requirePositiveNumber("epoch")
    }

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any = try {
        LettuceFencingLeaseConfig(namespace, resourceName, epoch)
    } catch (_: IllegalArgumentException) {
        throw InvalidObjectException("Invalid fencing lease config.")
    } catch (_: NullPointerException) {
        throw InvalidObjectException("Invalid fencing lease config.")
    }

    private companion object {
        val NAME_PATTERN: Regex = Regex("[A-Za-z0-9._-]{1,128}")
        private const val serialVersionUID: Long = 1L
    }
}

class FencingOwnerId private constructor(
    internal val value: String,
) : Serializable {
    init {
        require(value.isNotBlank() && value.toByteArray(Charsets.UTF_8).size in 1..256) {
            "ownerId must contain 1..256 UTF-8 bytes."
        }
    }

    override fun equals(other: Any?): Boolean = other is FencingOwnerId && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "FencingOwnerId(<redacted>)"

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any = try {
        from(value)
    } catch (_: IllegalArgumentException) {
        throw InvalidObjectException("Invalid fencing owner ID.")
    } catch (_: NullPointerException) {
        throw InvalidObjectException("Invalid fencing owner ID.")
    }

    companion object {
        private const val serialVersionUID: Long = 1L
        fun random(): FencingOwnerId = from(Base58.randomString(22))
        fun from(value: String): FencingOwnerId = FencingOwnerId(value)
    }
}

data class FencingToken(
    val epoch: Long,
    val sequence: Long,
) : Comparable<FencingToken>, Serializable {
    init {
        epoch.requirePositiveNumber("epoch")
        sequence.requirePositiveNumber("sequence")
    }

    override fun compareTo(other: FencingToken): Int =
        compareValuesBy(this, other, FencingToken::epoch, FencingToken::sequence)

    override fun toString(): String = "FencingToken(<redacted>)"

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any = try {
        FencingToken(epoch, sequence)
    } catch (_: IllegalArgumentException) {
        throw InvalidObjectException("Invalid fencing token.")
    } catch (_: NullPointerException) {
        throw InvalidObjectException("Invalid fencing token.")
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
```

- [ ] `FencingLeaseResult.kt`에 설계의 enum과 다섯 sealed result를 이름 그대로 구현한다.
    - `FencingBackendFailureKind`는 `CONNECTION`, `TIMEOUT`, `COMMAND`; `FencingIntegrityFailureKind`는 `MALFORMED_LEASE`, `INVALID_COUNTER`, `COUNTER_BEHIND_LEASE`다. enum serialization은 Java 기본 계약을 그대로 사용한다.
    - `FencingLeaseBackendFailure`, `FencingLeaseIntegrityFailure`, token/TTL/failure property를 가진 모든 variant는 `readResolve()`에서 canonical constructor를 다시 호출한다. `IllegalArgumentException`, `NullPointerException` 등 invariant failure는 raw cause 없이 stable `InvalidObjectException`으로 바꾼다.
    - 모든 nested `data class`는 `Serializable` 상속과 private companion `serialVersionUID = 1L`을 가진다.
    - 모든 nested `data object`는 아래 canonical singleton 패턴을 빠짐없이 사용한다.

| Result                   | Exact variants                                                                                                           |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `FencingBootstrapResult` | `Initialized`, `AlreadyInitialized`, `IntegrityFailure`, `BackendFailure`                                                |
| `FencingAcquireResult`   | `Acquired`, `AlreadyOwned`, `Contended`, `CounterUnavailable`, `SequenceExhausted`, `IntegrityFailure`, `BackendFailure` |
| `FencingInspectResult`   | `Owned`, `Lost`, `Contended`, `IntegrityFailure`, `BackendFailure`                                                       |
| `FencingRenewResult`     | `Renewed`, `Lost`, `OwnershipMismatch`, `IntegrityFailure`, `BackendFailure`                                             |
| `FencingReleaseResult`   | `Released`, `Lost`, `OwnershipMismatch`, `IntegrityFailure`, `BackendFailure`                                            |

```kotlin
sealed interface FencingBootstrapResult : Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }

    data object Initialized : FencingBootstrapResult {
        @Suppress("unused")
        private fun readResolve(): Any = Initialized
        private const val serialVersionUID: Long = 1L
    }

    data object AlreadyInitialized : FencingBootstrapResult {
        @Suppress("unused")
        private fun readResolve(): Any = AlreadyInitialized
        private const val serialVersionUID: Long = 1L
    }

    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ) : FencingBootstrapResult {
        @Throws(ObjectStreamException::class)
        private fun readResolve(): Any = try {
            IntegrityFailure(failure)
        } catch (_: IllegalArgumentException) {
            throw InvalidObjectException("Invalid fencing integrity failure.")
        } catch (_: NullPointerException) {
            throw InvalidObjectException("Invalid fencing integrity failure.")
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ) : FencingBootstrapResult {
        @Throws(ObjectStreamException::class)
        private fun readResolve(): Any = try {
            BackendFailure(failure)
        } catch (_: IllegalArgumentException) {
            throw InvalidObjectException("Invalid fencing backend failure.")
        } catch (_: NullPointerException) {
            throw InvalidObjectException("Invalid fencing backend failure.")
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
```

- [ ] GREEN 명령을 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*FencingLeaseValueTest' --tests '*FencingLeaseResultTest'
```

예상 결과: 모든 ordering/invariant/serialization/redaction test가 통과한다.

### 2.3 Commit

- [ ] `git diff --check` 후 다음 Lore commit을 만든다.

```text
Define a serialization-safe fencing lease vocabulary

Constraint: Every public non-enum value and nested result variant must be Serializable with stable invariants.
Rejected: primitive Long and String API | it would permit domain confusion and leak owner values through default rendering
Confidence: high
Scope-risk: narrow
Directive: Preserve canonical readResolve behavior for every serialized data object and validated value.
Tested: FencingLeaseValueTest; FencingLeaseResultTest; git diff --check
Not-tested: Redis execution is introduced in later tasks
```

## 3. Task 2 — `RedisScriptRunner`의 현재 upstream future 취소 전파

**파일:**

- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/script/RedisScript.kt`
- 수정: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/script/RedisScriptTest.kt`

### 3.1 RED — EVALSHA/NOSCRIPT race contract

- [ ] controllable `TestRedisFuture`와 mocked `RedisScriptingAsyncCommands`로 다음 test를 먼저 추가한다.
    - EVALSHA pending 중 returned future cancel → EVALSHA future cancelled.
    - EVALSHA가 `RedisNoScriptException`으로 끝난 뒤 EVAL pending 중 cancel → EVAL future cancelled.
    - EVALSHA failure와 returned cancel이 경쟁해도 fallback이 시작됐다면 그 current future가 cancelled.
    - caller cancellation은 `CompletionException`이나 failed future로 바뀌지 않고 returned future의 `isCancelled`를 유지.
    - 초기 `evalsha(...)`가 동기 throw해도 returned future가 terminal exceptional 상태가 되고 pending으로 남지 않음.
    - NOSCRIPT fallback의 `eval(...)`가 동기 throw해도 같은 terminal exceptional 상태가 되고 pending으로 남지 않음.
    - 정상 result와 non-NOSCRIPT failure의 기존 동작 유지.
- [ ] RED 명령으로 현재 `exceptionallyCompose` chain이 upstream cancellation contract를 만족하지 못함을 확인한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests 'io.bluetape4k.redis.lettuce.script.RedisScriptTest'
```

예상 결과: 새 cancellation assertion이 실패한다.

### 3.2 GREEN — cancellable bridge 구현

- [ ] `runAsyncScripting`을 `exceptionallyCompose` 반환에서 명시적 bridge로 교체한다. `AtomicReference`는 class property가 아니라 method-local이므로 atomicfu가 아니라 JDK `AtomicReference`를 사용한다.

```kotlin
private fun <T> runAsyncScripting(
    commands: RedisScriptingAsyncCommands<String, String>,
    script: RedisScript,
    outputType: ScriptOutputType,
    keys: Array<String>,
    vararg args: String,
): CompletableFuture<T> {
    val current = AtomicReference<CompletableFuture<T>>()
    val result = object : CompletableFuture<T>() {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            val cancelled = super.cancel(mayInterruptIfRunning)
            if (cancelled) current.get()?.cancel(mayInterruptIfRunning)
            return cancelled
        }
    }

    lateinit var attach: (CompletableFuture<T>) -> Unit
    val dispatch: (() -> CompletableFuture<T>) -> Unit = { command ->
        if (!result.isDone) {
            val upstream = try {
                command()
            } catch (error: Exception) {
                result.completeExceptionally(error)
                null
            }
            if (upstream != null) attach(upstream)
        }
    }

    attach = { upstream ->
        current.set(upstream)
        if (result.isCancelled) {
            upstream.cancel(true)
        } else {
            upstream.whenComplete { value, error ->
                val cause = error?.unwrapCompletionCause()
                if (cause is RedisNoScriptException && !result.isCancelled) {
                    log.debug { "NOSCRIPT(async) fallback (sha1=${script.sha1})" }
                    dispatch { commands.eval<T>(script.source, outputType, keys, *args).toCompletableFuture() }
                } else if (cause != null) {
                    result.completeExceptionally(cause)
                } else {
                    result.complete(value)
                }
            }
        }
    }

    dispatch { commands.evalsha<T>(script.sha1, outputType, keys, *args).toCompletableFuture() }
    return result
}

private fun Throwable.unwrapCompletionCause(): Throwable =
    if (this is CompletionException) cause ?: this else this
```

- [ ] race에서 cancelled result에 fallback이 붙은 직후 `isCancelled` 재확인으로 current future가 남지 않는지 test를 반복 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests 'io.bluetape4k.redis.lettuce.script.RedisScriptTest' --rerun-tasks
```

예상 결과: 기존 sync/async/suspend/NOSCRIPT test와 새 cancellation test가 모두 통과한다.

### 3.3 Commit

- [ ] `git diff --check` 후 commit한다.

```text
Keep script cancellation attached to the active Redis command

Constraint: EVALSHA may hand execution to an EVAL fallback after NOSCRIPT.
Rejected: chained CompletableFuture cancellation | it loses a stable reference to the active Lettuce future
Confidence: high
Scope-risk: moderate
Directive: Any future fallback stage must replace the cancellation target before it becomes observable.
Tested: RedisScriptTest including EVALSHA and EVAL cancellation races; git diff --check
Not-tested: fencing lease integration is introduced later
```

## 4. Task 3 — Key derivation, validation, wire protocol, backend classifier

**파일:**

- 생성: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseSupport.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseSupportTest.kt`

### 4.1 RED — pure support contract

- [ ] 다음 pure unit test를 작성한다.
    - derived key가 정확히 `fence:{namespace:resourceName}:<epoch>:lease|counter`.
    - default/custom codec 모두 `SlotHash.getSlot(codec.encodeKey(key))`를 사용하고, wire bytes가 다른 slot이면 stable `IllegalArgumentException`이며 command mock interaction은 0.
    - decimal parser는 `0`, positive canonical decimal, `Long.MAX_VALUE`를 허용하고 sign, leading zero, whitespace, empty, non-digit, 20+ byte를 거절.
    - TTL은 positive whole millisecond로 변환하고 nanos-only, zero, negative, `toMillis()` overflow를 모두 `IllegalArgumentException`으로 정규화. operation wrapper는 exact Lua integer/TTL reply를 위해 `2^53 - 1` milliseconds 상한도 dispatch 전에 검증.
    - token epoch mismatch는 renew/release dispatch 전에 거절.
    - `CompletionException`/`ExecutionException` 최대 8단계 unwrap, identity cycle 방지, chain 어디의 `CancellationException`도 rethrow.
    - `RedisConnectionException`→`CONNECTION`, `RedisCommandTimeoutException`/`TimeoutException`→`TIMEOUT`, 다른 `RedisException`→`COMMAND`.
    - validation, internal wire exception, unknown non-Lettuce exception은 classifier가 다시 던지고 result로 만들지 않음.
    - log capture에 sentinel namespace/resource/key/owner/token/raw reply/exception message가 없고 allowlisted operation/kind/class name만 존재.
- [ ] RED 명령을 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseSupportTest'
```

예상 결과: support symbol이 없어 test compilation이 실패한다.

### 4.2 GREEN — 고정 내부 계약 구현

- [ ] 다음 internal type을 구현한다. script reply는 Lua number가 아니라 fixed-size bulk string vector로 decode한다.

```kotlin
internal data class FencingLeaseKeys(
    val lease: String,
    val counter: String,
)

internal enum class FencingLeaseOperation {
    BOOTSTRAP,
    ACQUIRE,
    INSPECT,
    RENEW,
    RELEASE,
}

internal class FencingLeaseProtocolException : IllegalStateException(
    "Malformed fencing lease response.",
)

internal fun deriveFencingLeaseKeys(
    config: LettuceFencingLeaseConfig,
    codec: RedisCodec<String, String>,
): FencingLeaseKeys {
    val tag = "${config.namespace}:${config.resourceName}"
    val prefix = "fence:{$tag}:${config.epoch}"
    val keys = FencingLeaseKeys("$prefix:lease", "$prefix:counter")
    require(SlotHash.getSlot(codec.encodeKey(keys.lease)) == SlotHash.getSlot(codec.encodeKey(keys.counter))) {
        "Derived fencing lease keys must share one Redis Cluster slot."
    }
    return keys
}
```

- [ ] decimal comparison은 `Long`/`Double` 변환 전에 canonical string 자체를 검증한다.

```kotlin
internal fun compareCanonicalDecimals(left: String, right: String): Int {
    requireCanonicalDecimal(left)
    requireCanonicalDecimal(right)
    val lengthComparison = left.length.compareTo(right.length)
    return if (lengthComparison != 0) lengthComparison else left.compareTo(right)
}

internal fun requireCanonicalDecimal(value: String): String {
    require(value == "0" || value.firstOrNull() in '1'..'9' && value.all(Char::isDigit)) {
        "Invalid decimal value."
    }
    require(value.length <= Long.MAX_VALUE.toString().length) { "Decimal value is out of range." }
    require(compareCanonicalDecimalsWithoutValidation(value, Long.MAX_VALUE.toString()) <= 0) {
        "Decimal value is out of range."
    }
    return value
}

private fun compareCanonicalDecimalsWithoutValidation(left: String, right: String): Int {
    val lengthComparison = left.length.compareTo(right.length)
    return if (lengthComparison != 0) lengthComparison else left.compareTo(right)
}
```

- [ ] duration conversion은 overflow 원인을 public message/cause에 보존하지 않는다.

```kotlin
internal fun Duration.requireFencingLeaseMillis(): Long {
    val millis = try {
        toMillis()
    } catch (_: ArithmeticException) {
        throw IllegalArgumentException("leaseTime must fit positive whole milliseconds.")
    }
    require(millis > 0 && this == Duration.ofMillis(millis)) {
        "leaseTime must fit positive whole milliseconds."
    }
    return millis
}
```

- [ ] Redis operation TTL은 `2^53 - 1` milliseconds 이하로 제한해 `PEXPIRE` absolute timestamp overflow와 Lua `PTTL` precision loss를 write 전에 차단한다. `PTTL` wire field는 `string.format('%.0f', ttl)`로 canonical decimal을 유지한다.

- [ ] operation별 decoder와 result factory를 분리하되 unknown status/field count/malformed success payload는 `FencingLeaseProtocolException`으로만 실패하게 한다.
- [ ] backend classifier는 public result를 받는 operation wrapper 하나에서 공유하고 raw `Throwable`을 public value나 log message에 넣지 않는다.
- [ ] `domainFingerprint`가 필요할 때만 `namespace + NUL + resourceName + NUL + epoch` SHA-256 앞 12 bytes를 24 lowercase hex로 계산하며 metric label API는 추가하지 않는다.
- [ ] GREEN 명령을 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseSupportTest'
```

예상 결과: key/codec/decimal/TTL/classifier/protocol/redaction unit test가 모두 통과한다.

### 4.3 Commit

- [ ] `git diff --check` 후 commit한다.

```text
Fail closed before fencing lease scripts can mutate Redis

Constraint: Key routing, exact decimals, TTL conversion, and backend classification must agree across three APIs.
Rejected: Lua number decoding and broad Throwable flattening | they lose integer precision and hide programmer failures
Confidence: high
Scope-risk: moderate
Directive: Keep protocol failures distinct from Redis state integrity and backend failures.
Tested: LettuceFencingLeaseSupportTest; git diff --check
Not-tested: real Redis state transitions are introduced next
```

## 5. Task 4 — Lua state machine와 hostile standalone Redis

**파일:**

- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseSupport.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseScriptTest.kt`

### 5.1 RED — operation matrix와 hostile state

- [ ] 실제 singleton Redis fixture로 bootstrap/acquire/inspect/renew/release의 정상·경합·lost·mismatch contract를 먼저 작성한다.
- [ ] 다음 hostile state를 raw Redis command로 만든 뒤 expected public integrity category와 no-mutation을 검증한다.
    - lease/counter wrong type, missing/extra hash field, oversized owner/epoch/sequence/counter.
    - signed/leading-zero/whitespace/non-digit/out-of-range decimal.
    - counter TTL, lease no TTL, counter missing while lease active, counter behind lease.
    - lease absent + counter absent는 acquire `CounterUnavailable`; inspect/renew/release `Lost`.
    - counter `Long.MAX_VALUE`는 `SequenceExhausted`이고 lease를 만들지 않음.
- [ ] `SCRIPT FLUSH` 뒤 EVALSHA→EVAL fallback 결과가 동일함을 검증한다.
- [ ] test-only Lua 두 개를 exact fixture key로 실행해 Redis runtime error의 non-rollback을 실제로 증명한다.
    - `INCR` 직후 deliberate wrong-type command로 실패시켜 counter 증가가 남고 다음 acquire가 그 sequence를 재사용하지 않음을 검증.
    - `HSET` 직후 `PEXPIRE` 전에 deliberate wrong-type command로 실패시켜 TTL 없는 partial lease가 남고 다음 production operation이 `MALFORMED_LEASE`를 반환함을 검증.
- [ ] structural assertion으로 script source에 `KEYS`, `SCAN`, `HGETALL`, stored cardinality loop가 없고, `HLEN == 3`, fixed `HSTRLEN`/`HMGET`, write 전 preflight, `INCR`→`GET`→`HSET`→`PEXPIRE` 순서가 있음을 검증한다. test-only non-rollback script는 production script inventory에서 제외한다.
- [ ] command spy로 정상 경로는 `EVALSHA=1, EVAL=0`, `NOSCRIPT` 경로만 `EVALSHA=1, EVAL=1`, validation/constructor rejection은 Redis dispatch 0임을 검증한다. facade가 script 앞에 `GET`/`TYPE` 같은 client-side preflight를 보내면 실패한다.
- [ ] RED 명령을 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseScriptTest'
```

예상 결과: script runner/decoder operation이 없어 test compilation 또는 assertion이 실패한다.

### 5.2 GREEN — fixed O (1) Lua protocol

- [ ] script 공통 preflight는 두 exact key만 사용하고 mutation 전에 다음을 모두 판정한다.
    - acquire/renew TTL argument의 canonical positive decimal과 `2^53 - 1` 상한.
    - counter `TYPE`, `PTTL`, `STRLEN`, canonical decimal과 범위.
    - lease `TYPE`, `PTTL`, `HLEN`, fixed field `HSTRLEN`, fixed `HMGET`, stored epoch와 counter relation.
    - lease `PTTL == -2`는 absent로 재분류하고 `-1`은 `MALFORMED_LEASE`.
- [ ] bootstrap은 lease가 없고 counter도 없을 때만 counter `0`을 생성한다. active lease + missing counter와 existing malformed counter는 initialize하지 않는다.
- [ ] acquire는 같은 owner에게 stored token을 그대로 반환하고, 다른 owner에게 TTL만 반환한다. absent lease에서는 missing counter/overflow를 write 없이 반환하고, 정상일 때만 아래 mutation order를 사용한다.

```lua
local nextSequence = redis.call('INCR', KEYS[2])
local nextSequenceText = redis.call('GET', KEYS[2])
redis.call('HSET', KEYS[1],
  'owner', ARGV[1],
  'epoch', ARGV[2],
  'sequence', nextSequenceText)
redis.call('PEXPIRE', KEYS[1], ARGV[3])
return {'ACQUIRED', ARGV[2], nextSequenceText, '-1'}
```

- [ ] renew/release는 stored owner와 exact `(epoch, sequence)`를 모두 비교한다. renew는 `PEXPIRE`만, release는 lease key `DEL`만 수행하며 counter를 변경하지 않는다.
- [ ] 모든 정상/분류 reply는 fixed field count의 string vector다. expected validation branch 이후 write가 없어야 하며 script source에 raw secret을 포함한 `error(...)`를 만들지 않는다.
- [ ] 모든 operation은 exact 4-field reply `[status, value1, value2, ttl]`을 사용한다. unused token field는 `0`, unused TTL은 `-1` sentinel로 고정한다.

| Status family                                                                                                                          | `value1`       | `value2`         |             `ttl` | Arity |
|----------------------------------------------------------------------------------------------------------------------------------------|----------------|------------------|------------------:|------:|
| `INITIALIZED`, `ALREADY_INITIALIZED`, `COUNTER_UNAVAILABLE`, `SEQUENCE_EXHAUSTED`, `RENEWED`, `RELEASED`, `LOST`, `OWNERSHIP_MISMATCH` | `0`            | `0`              |              `-1` |     4 |
| `ACQUIRED`                                                                                                                             | epoch decimal  | sequence decimal |              `-1` |     4 |
| `ALREADY_OWNED`, `OWNED`                                                                                                               | epoch decimal  | sequence decimal | non-negative PTTL |     4 |
| `CONTENDED`                                                                                                                            | `0`            | `0`              | non-negative PTTL |     4 |
| `INTEGRITY_FAILURE`                                                                                                                    | enum wire code | `0`              |              `-1` |     4 |

- [ ] production script의 최대 Redis command inventory를 source-level structural test로 고정한다. common lease preflight는 `TYPE`, `PTTL`, `HLEN`, `HSTRLEN` ×3, fixed `HMGET` 최대 7회; counter preflight는 `TYPE`, `PTTL`, `STRLEN`, `GET` 최대 4회다. mutation 상한은 bootstrap `SET` 1회, acquire `INCR`/`GET`/`HSET`/`PEXPIRE` 4회, renew `PEXPIRE` 1회, release `DEL` 1회, inspect 0회다. 따라서 operation당 최대 internal command는 acquire 15, bootstrap/renew/release 12, inspect 11이며 stored cardinality에 따라 증가하지 않는다.
- [ ] standalone test를 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseScriptTest'
```

예상 결과: 정상 state machine, hostile state fail-closed, overflow, NOSCRIPT, structural O (1) test가 통과한다.

### 5.3 Commit

- [ ] `git diff --check` 후 commit한다.

```text
Issue ordered lease generations only from validated Redis state

Constraint: Redis scripts do not roll back writes after every runtime error and Lua numbers cannot represent all Long values exactly.
Rejected: implicit counter bootstrap and numeric token replies | they hide history loss and precision boundaries
Confidence: high
Scope-risk: moderate
Directive: Keep all expected failures before the first write and never reduce or delete the counter.
Tested: LettuceFencingLeaseScriptTest against standalone Redis; git diff --check
Not-tested: facade parity, Cluster routing, and cancellation follow
```

## 6. Task 5 — Sync, future, suspend facade와 shared contract

**파일:**

- 생성: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLease.kt`
- 생성: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceSuspendFencingLease.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseContract.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseTest.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceSuspendFencingLeaseTest.kt`

### 6.1 RED — 공통 adapter contract

- [ ] `FencingLeaseAdapter`를 test internal interface로 만들고 sync, future, suspend adapter가 동일 scenario table을 실행하게 한다.

```kotlin
internal interface FencingLeaseAdapter {
    val name: String
    suspend fun bootstrap(): FencingBootstrapResult
    suspend fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult
    suspend fun inspect(ownerId: FencingOwnerId): FencingInspectResult
    suspend fun renew(ownerId: FencingOwnerId, token: FencingToken, leaseTime: Duration): FencingRenewResult
    suspend fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult
}
```

- [ ] scenario table에 다음을 넣는다.
    - bootstrap initialized/replay.
    - strictly increasing acquire→release generations.
    - same-owner acquire replay가 같은 token이며 TTL을 연장하지 않음.
    - inspect owned/contended/lost, renew token 유지, release counter 보존.
    - wrong owner와 stale token renew/release가 newer lease를 변경하지 않음.
    - expiry 뒤 takeover가 더 큰 token.
    - 다른 epoch token renew/release가 dispatch 전에 `IllegalArgumentException`.
    - invalid TTL/config/owner가 세 API에서 dispatch 전에 동일 exception type.
    - connection/timeout/command failure가 세 API에서 같은 backend kind.
    - cancellation/decoder/unknown exception은 backend result가 아님.
- [ ] public facade shape가 설계 9절과 정확히 일치하는 compile-time usage test를 작성한다. public constructor, bootstrap, 모든 sync/future/suspend operation을 빠짐없이 호출한다.
- [ ] RED 명령을 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseTest' --tests '*LettuceSuspendFencingLeaseTest'
```

예상 결과: facade type이 없어 compilation이 실패한다.

### 6.2 GREEN — facade는 validation과 support에만 위임

- [ ] `LettuceFencingLease` primary constructor는 private이고 standalone/Cluster public secondary constructor가 config를 필수로 받는다. constructor에서 config invariant, derived key wire slot invariant를 완료한다.
- [ ] sync method와 async method는 같은 validated input을 만들며 async validation failure도 future 생성 전에 동기 throw한다.
- [ ] future method는 Task 2의 cancellation-propagating `RedisScriptRunner.runAsync()`를 통해 얻은 future를 operation decoder/classifier와 연결하되 caller cancellation을 failed result로 바꾸지 않는다.
- [ ] public `CompletableFuture<Fencing*Result>`를 만드는 decode/classifier mapping도 source runner future의 cancellation target을 잃지 않는 `decodeCancellable` shared helper를 사용한다. `thenApply`, `handle`, `exceptionally` chain을 public future로 직접 반환하지 않는다. helper는 success frame과 failure를 모두 mapping하고, allowlisted Redis failure만 operation-specific `BackendFailure` result factory에 전달한다. decoder/protocol/unknown exception은 exceptional completion이다.

```kotlin
internal fun <R> CompletableFuture<List<String>>.decodeCancellable(
    operation: FencingLeaseOperation,
    decode: (List<String>) -> R,
    backendFailure: (FencingLeaseBackendFailure) -> R,
): CompletableFuture<R> {
    val source = this
    val mapped = object : CompletableFuture<R>() {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            val cancelled = super.cancel(mayInterruptIfRunning)
            if (cancelled) source.cancel(mayInterruptIfRunning)
            return cancelled
        }
    }
    source.whenComplete { frame, error ->
        when {
            source.isCancelled -> mapped.cancel(false)
            error != null -> try {
                val failure = classifyFencingBackendFailure(operation, error.unwrapCompletionCause())
                mapped.complete(backendFailure(failure))
            } catch (_: CancellationException) {
                mapped.cancel(false)
            } catch (failure: Throwable) {
                mapped.completeExceptionally(failure)
            }
            else -> try {
                mapped.complete(decode(frame))
            } catch (failure: Throwable) {
                mapped.completeExceptionally(failure)
            }
        }
    }
    return mapped
}
```

- [ ] public future cancel test는 decode/classifier가 연결된 뒤 EVALSHA pending과 NOSCRIPT EVAL pending 각각에서 현재 upstream future까지 취소되는지 검증한다.
- [ ] connection/timeout/command exception은 public future가 exceptional completion이 아니라 matching `BackendFailure` result로 정상 완료되는지 sync/suspend와 parity assertion을 둔다. decoder/unknown exception은 exceptional completion을 유지한다.
- [ ] `LettuceSuspendFencingLease`는 같은 async commands/codec/config/support를 사용하고 `RedisFuture.awaitSuspending()` cancellation을 그대로 보존한다.
- [ ] 두 execution class는 Redis connection을 보유하므로 `Serializable`을 구현하지 않는다.
- [ ] facade public signature는 다음 complete contract를 그대로 유지한다.

```kotlin
class LettuceFencingLease private constructor(
    syncCommands: RedisScriptingCommands<String, String>,
    asyncCommands: RedisScriptingAsyncCommands<String, String>,
    codec: RedisCodec<String, String>,
    config: LettuceFencingLeaseConfig,
) {
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ) : this(connection.sync(), connection.async(), connection.codec, config)

    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ) : this(connection.sync(), connection.async(), connection.codec, config)

    fun bootstrap(): FencingBootstrapResult
    fun bootstrapAsync(): CompletableFuture<FencingBootstrapResult>
    fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult
    fun acquireAsync(ownerId: FencingOwnerId, leaseTime: Duration): CompletableFuture<FencingAcquireResult>
    fun inspect(ownerId: FencingOwnerId): FencingInspectResult
    fun inspectAsync(ownerId: FencingOwnerId): CompletableFuture<FencingInspectResult>
    fun renew(ownerId: FencingOwnerId, token: FencingToken, leaseTime: Duration): FencingRenewResult
    fun renewAsync(
        ownerId: FencingOwnerId,
        token: FencingToken,
        leaseTime: Duration,
    ): CompletableFuture<FencingRenewResult>
    fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult
    fun releaseAsync(ownerId: FencingOwnerId, token: FencingToken): CompletableFuture<FencingReleaseResult>
}

class LettuceSuspendFencingLease private constructor(
    asyncCommands: RedisScriptingAsyncCommands<String, String>,
    codec: RedisCodec<String, String>,
    config: LettuceFencingLeaseConfig,
) {
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ) : this(connection.async(), connection.codec, config)

    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ) : this(connection.async(), connection.codec, config)

    suspend fun bootstrap(): FencingBootstrapResult
    suspend fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult
    suspend fun inspect(ownerId: FencingOwnerId): FencingInspectResult
    suspend fun renew(
        ownerId: FencingOwnerId,
        token: FencingToken,
        leaseTime: Duration,
    ): FencingRenewResult
    suspend fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult
}
```

- [ ] targeted contract를 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseTest' --tests '*LettuceSuspendFencingLeaseTest'
```

예상 결과: sync/future/suspend가 동일 result와 validation/backend failure boundary를 보인다.

### 6.3 Commit

- [ ] `git diff --check` 후 commit한다.

```text
Expose one fencing contract across blocking, future, and suspend callers

Constraint: All execution styles must share validation, scripts, decoding, and failure classification.
Rejected: separate per-facade implementations | they would drift on cancellation and hostile Redis state
Confidence: high
Scope-risk: moderate
Directive: Keep new behavior in shared support and prove every public result through FencingLeaseContract.
Tested: LettuceFencingLeaseTest; LettuceSuspendFencingLeaseTest; shared contract; git diff --check
Not-tested: bounded contention and Cluster stress follow
```

## 7. Task 6 — Cancellation과 모호한 완료를 결정적으로 재현

**파일:**

- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseSupport.kt`
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLease.kt`
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceSuspendFencingLease.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseCancellationTest.kt`

### 7.1 RED — apply 전/후 fault injection

- [ ] public API를 늘리지 않는 internal executor seam을 support에 정의하고, test fake가 operation별로 `BEFORE_APPLY`와 `AFTER_APPLY_BEFORE_REPLY`를 inject하게 한다.

```kotlin
internal interface FencingScriptExecutor {
    fun run(
        operation: FencingLeaseOperation,
        keys: FencingLeaseKeys,
        args: List<String>,
    ): List<String>

    fun runAsync(
        operation: FencingLeaseOperation,
        keys: FencingLeaseKeys,
        args: List<String>,
    ): CompletableFuture<List<String>>

    suspend fun runSuspending(
        operation: FencingLeaseOperation,
        keys: FencingLeaseKeys,
        args: List<String>,
    ): List<String>
}
```

- [ ] bootstrap/acquire/renew/release 각각에 apply 전 failure와 apply 후 reply loss를 inject하고 operation-specific reconciliation을 검증한다.
    - bootstrap: 승인된 fresh epoch에서 replay는 initialized/already initialized.
    - acquire: 같은 owner replay가 같은 token.
    - renew: inspect same token 후 재시도 가능.
    - release: `Lost`를 ownership 폐기로만 해석.
- [ ] release ambiguity는 두 branch를 모두 검증한다.
    - BEFORE_APPLY 뒤 `inspect(ownerId)`가 exact same token의 `Owned`면 같은 owner/token으로 release를 한 번 다시 시도할 수 있다.
    - AFTER_APPLY 뒤 `Lost`면 local ownership을 폐기하고 성공/expiry를 구분하지 않는다.
    - inspect가 `Contended`, 다른/newer token, integrity/backend failure를 보이면 release retry와 local success 처리를 모두 금지한다.
- [ ] returned `CompletableFuture.cancel(true)`의 cancelled 상태, EVALSHA/fallback current upstream cancellation, server apply 전/후 state를 검증한다.
- [ ] 실제 coroutine job cancel이 `CancellationException`으로 끝나고 backend result/retry로 변하지 않음을 `runSuspendIO`로 검증한다.
- [ ] sleep/network timing 없이 latch/fake로 아래 operation × phase × signal matrix를 모두 실행한다. 모든 BEFORE cell은 Redis mutation 0, 모든 AFTER cell은 적용된 ambiguous state와 operation-specific reconcile을 확인한다.

| Operation | BEFORE_APPLY backend failure | AFTER_APPLY reply loss | Future cancel | Coroutine cancel | Reconcile                                |
|-----------|-----------------------------:|-----------------------:|--------------:|-----------------:|------------------------------------------|
| bootstrap |                     required |               required |      required |         required | fresh higher epoch replay                |
| acquire   |                     required |               required |      required |         required | same owner replay, same token            |
| renew     |                     required |               required |      required |         required | inspect same token, optional renew retry |
| release   |                     required |               required |      required |         required | `Lost` means local ownership discarded   |

- [ ] RED 명령을 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseCancellationTest'
```

예상 결과: fault seam과 cancellation reconciliation이 없어 실패한다.

### 7.2 GREEN — cancellation 우선 전파

- [ ] default executor는 기존 runner에만 위임하고 test seam은 internal test access로 제한한다.
- [ ] 두 facade의 public constructor는 default executor를 private primary constructor로 전달한다. `internal companion object createForTesting(...)`만 controllable executor를 같은 private constructor에 주입하며 public test constructor나 public executor type은 만들지 않는다.
- [ ] suspend wrapper의 catch ordering은 반드시 아래 형태를 유지한다.

```kotlin
try {
    return decode(executor.runSuspending(operation, keys, args))
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    return backendFailureOrThrow(operation, e)
}
```

- [ ] future completion도 `CancellationException`/cancelled 상태를 먼저 보존하고 backend classifier는 실제 Redis exception만 받는다.
- [ ] cancellation test를 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseCancellationTest' --rerun-tasks
```

예상 결과: before/after apply와 EVALSHA/EVAL 전환 모든 case가 deterministic하게 통과한다.

### 7.3 Commit

- [ ] `git diff --check` 후 commit한다.

```text
Make ambiguous fencing outcomes recoverable without hiding cancellation

Constraint: Client cancellation and transport loss cannot prove whether a Redis script executed.
Rejected: cancellation as BackendFailure | it breaks structured concurrency and caller ownership recovery
Confidence: high
Scope-risk: moderate
Directive: Reconcile mutations with the same owner and token after any post-dispatch ambiguity.
Tested: LettuceFencingLeaseCancellationTest before and after apply; git diff --check
Not-tested: external topology promotion remains opt-in
```

## 8. Task 7 — Bounded contention과 Redis Cluster routing

**파일:**

- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseConcurrencyTest.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseClusterTest.kt`

### 8.1 RED — duplicate generation과 wrong-slot 방어

- [ ] `@Timeout(30)`을 붙인 standalone `MultithreadingTester`/`SuspendedJobTester` fixture로 16 caller × 25 generation을 수행한다.
    - 각 round마다 16개의 distinct owner를 barrier에서 동시에 시작한다.
    - 정확히 1 `Acquired`, 나머지 15 `Contended` → winner same-owner replay가 동일 token → winner release → 다음 round 순서를 강제한다.
    - 발급 token 중복 0, natural ordering regression 0, unexpected failure 0.
    - 같은-owner retry는 generation을 증가시키지 않음.
- [ ] `@Timeout(30)` Cluster fixture도 round마다 8 distinct owner를 barrier에서 동시에 시작해 정확히 1 `Acquired`, 7 `Contended`, same-owner replay, release 후 다음 round로 진행한다. standalone은 25개, Cluster는 10개의 중복 없는 strictly increasing winner token을 검증하고 sync/future/suspend result parity와 script routing을 함께 고정한다.
- [ ] custom codec이 두 logical key를 다른 wire slot으로 encode하는 fixture에서 constructor가 node dispatch 전에 stable `IllegalArgumentException`을 던짐을 command spy로 검증한다.
- [ ] RED 명령을 Testcontainers 순차로 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseConcurrencyTest'
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseClusterTest'
```

예상 결과: contention/Cluster contract 중 누락된 동작이 실패한다.

### 8.2 GREEN — 발견된 race/routing만 최소 수정

- [ ] 실패가 있다면 shared support/Lua에만 최소 수정하고 facade별 workaround를 만들지 않는다.
- [ ] hot resource는 한 slot/event loop에서 직렬화된다는 경계를 test 이름과 KDoc evidence에 남기고 latency SLA나 새 benchmark dependency는 추가하지 않는다.
- [ ] 두 test를 다시 순차 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseConcurrencyTest' --rerun-tasks
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseClusterTest' --rerun-tasks
```

예상 결과: bounded contention과 Cluster test가 각각 30초 안에 통과한다.

### 8.3 Commit

- [ ] `git diff --check` 후 commit한다.

```text
Prove fencing generations stay unique under bounded contention

Constraint: One logical resource is serialized by one Redis slot and custom codecs define routing bytes.
Rejected: string-only slot checks and latency SLA gates | they ignore wire encoding and add unstable environment coupling
Confidence: high
Scope-risk: narrow
Directive: Keep contention bounded and run standalone and Cluster Testcontainers checks sequentially.
Tested: LettuceFencingLeaseConcurrencyTest; LettuceFencingLeaseClusterTest; git diff --check
Not-tested: production-scale latency is intentionally outside this issue
```

## 9. Task 8 — 외부 resilience, downstream CAS, epoch recovery 예제

**파일:**

- 수정: `infra/lettuce/build.gradle.kts`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseResilience4jTest.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseRecoveryTest.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseTopologyRecoveryTest.kt`

### 9.1 RED — caller-layer policy contract

- [ ] 실제 Redis acquire를 `SuspendDecorators`로 감싸고 다음 exact order를 검증한다.

```kotlin
SuspendDecorators.ofSupplier {
    lease.acquire(ownerId, leaseTime)
}
    .withRetry(retry)
    .withCircuitBreaker(circuitBreaker)
    .withBulkhead(bulkhead)
    .invoke()
```

- [ ] `RetryConfig.retryOnResult`와 `CircuitBreakerConfig.recordResult`는 `FencingAcquireResult.BackendFailure`만 failure로 센다.
- [ ] Retry의 exception predicate는 항상 false여서 exception을 재시도하지 않는다. CircuitBreaker는 exception을 모두 ignore해 caller/validation/cancellation/internal exception을 success나 failure로 기록하지 않는다. exception은 decorator 밖으로 그대로 전파한다.

```kotlin
val retry = Retry.of(
    name,
    RetryConfig.custom<FencingAcquireResult>()
        .maxAttempts(2)
        .waitDuration(Duration.ZERO)
        .retryOnResult(::isBackendFailure)
        .retryOnException { false }
        .build(),
)
val circuitBreaker = CircuitBreaker.of(
    name,
    CircuitBreakerConfig.custom()
        .slidingWindowSize(2)
        .minimumNumberOfCalls(2)
        .failureRateThreshold(50.0F)
        .recordResult { result -> result is FencingAcquireResult.BackendFailure }
        .ignoreException { true }
        .build(),
)
```

- [ ] 한 bulkhead permit이 전체 retry를 감싸고 circuit breaker가 retry 종료 후 final result 하나만 관찰함을 metrics와 controlled attempts로 검증한다.
- [ ] `IntegrityFailure`, `CounterUnavailable`, `SequenceExhausted`, `OwnershipMismatch`, validation은 retry하지 않는다.
- [ ] `CallNotPermittedException`, `BulkheadFullException`, `CancellationException`은 primitive result로 변환하지 않는다.
- [ ] supplier 내부 `IllegalArgumentException`, `CancellationException`, internal protocol exception은 supplier attempt=1, retry attempt=0, Retry `failedWithoutRetryAttempt=1`, CircuitBreaker success/failure=0인지 각각 독립 instance로 검증한다.
- [ ] open CircuitBreaker의 `CallNotPermittedException`과 포화 Bulkhead의 `BulkheadFullException`은 supplier attempt=0이며 inner Retry metric이 변하지 않는다. CircuitBreaker는 open rejection을 recorded success/failure로 세지 않고, Bulkhead rejection은 CircuitBreaker와 Retry 모두에 도달하지 않는다.
- [ ] same owner를 decorator 밖에서 capture해 ambiguous acquire replay가 같은 token을 반환함을 실제 Redis로 검증한다.
- [ ] PostgreSQL-style in-memory fixture는 `(resourceId, epoch, sequence)`를 저장하고 `affectedRows == 1`만 accepted로 처리한다. same/stale token replay는 0이며 business idempotency key는 별도 field/fixture로 둔다.
- [ ] deterministic control-plane harness가 다음 exact transition을 검증한다.

```text
pause -> block old acquire -> drain lease and downstream writer -> CAS bump epoch ->
bootstrap -> verify readiness and tuple guard -> rollout -> confirm old absence -> resume
```

- [ ] mixed epoch 탐지 시 abort, lower-epoch rollback 거절, higher epoch 유지, Redis-only rollback detection 한계와 downstream stale rejection을 검증한다.
- [ ] concurrent durable epoch allocator 두 개 이상을 barrier에서 시작해 CAS 성공이 정확히 1개이고 한 higher epoch만 활성화됨을 검증한다. application-local config와 Redis counter를 authority로 넣는 fake는 allocation 권한이 없어야 한다.
- [ ] bootstrap 후 readiness PASS는 exact counter `TYPE=string`, `PTTL=-1`, canonical non-negative decimal, downstream strict tuple guard enabled를 모두 만족할 때만 발생한다.
- [ ] event trace로 mixed epoch 발견 뒤 `ROLLOUT`과 `RESUME` event가 0임을 검증한다. lower-epoch binary rollback 요청은 rejected event를 남기고 current higher epoch를 유지한다.
- [ ] README의 marker-delimited diagnostic Lua를 실제 standalone Redis에서 `evalReadOnly`/`EVAL_RO`로 실행한다.
    - input은 derived lease/counter exact 2 keys만 허용하고 output은 bounded stable classification code와 boolean뿐이다.
    - clean, missing counter, malformed lease, TTL 없는 partial lease를 각각 분류한다.
    - lease-only delete eligibility 4조건을 각 하나씩 false로 만드는 table과 all-true case를 검증한다. 하나라도 false면 delete 금지다.
    - repair 전후 counter value와 `PTTL=-1`은 같아야 하며 counter delete/decrement/TTL 설정/same-epoch bootstrap command가 없어야 한다.
- [ ] `LettuceFencingLeaseTopologyRecoveryTest`는 `@Tag("fencing-topology")`를 사용하고 기본 `test` task에서는 제외한다. 전용 `fencingLeaseTopologyRecoveryTest` task만 해당 tag를 include하며 Testcontainers primary/replica와 Toxiproxy를 순차 실행한다.
    - primary/replica가 같은 baseline offset에 도달한 뒤 Toxiproxy로 replication path를 차단하고 old epoch write를 primary에만 acknowledge시킨다.
    - primary를 중지하고 stale replica를 실제 `REPLICAOF NO ONE`으로 승격한 뒤 external incident signal을 발생시킨다.
    - signal 이전 old acquire 1회가 존재하고, signal 뒤 traffic gate가 닫혀 old acquire/downstream write가 0인지 event trace와 barrier로 검증한다.
    - durable CAS allocator가 higher epoch를 정확히 한 번 발급하고, promoted Redis에 bootstrap/readiness를 통과한 뒤 새 token의 epoch가 높으며 downstream strict tuple guard가 old token을 거절하는지 검증한다.
    - restore branch는 known-old RDB fixture로 Redis를 재기동한 뒤 동일 pause→CAS bump→bootstrap→readiness→resume contract를 반복하고 same/lower epoch bootstrap이 없음을 검증한다.
    - `finally`에서 traffic gate, Toxiproxy toxic/proxy, Lettuce connection/client, primary/replica/restore container, network를 역순으로 정리하고 thread/future가 남지 않는지 검증한다.
- [ ] RED 명령을 순차 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseResilience4jTest'
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseRecoveryTest'
./gradlew :bluetape4k-lettuce:fencingLeaseTopologyRecoveryTest
```

예상 결과: caller policy/recovery fixture와 tagged topology task가 없어 실패한다.

### 9.2 GREEN — test/example layer에서만 policy 구현

- [ ] resilience/testcontainers dependency는 이미 있으므로 dependency를 추가하지 않는다. `build.gradle.kts`는 기본 `test`의 `excludeTags("performance", "fencing-topology")`와 `fencingLeaseTopologyRecoveryTest` 전용 task만 추가한다.
- [ ] result predicate는 acquire result에 대해 다음 exact shape를 사용하고, 다른 operation 예제도 각 `BackendFailure` variant만 failure로 센다.

```kotlin
private fun isBackendFailure(result: FencingAcquireResult): Boolean =
    result is FencingAcquireResult.BackendFailure
```

- [ ] production primitive에 Retry/CircuitBreaker/Bulkhead type, 설정, retry loop를 추가하지 않는다.
- [ ] recovery harness는 test-only durable authority/CAS로 유지하고 production PostgreSQL adapter를 만들지 않는다.
- [ ] test를 다시 순차 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseResilience4jTest' --rerun-tasks
./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseRecoveryTest' --rerun-tasks
./gradlew :bluetape4k-lettuce:fencingLeaseTopologyRecoveryTest --rerun-tasks
```

예상 결과: decorator topology, exception boundary, strict tuple acceptance, simulated recovery와 실제 tagged promotion/restore contract가 통과한다.

### 9.3 Commit

- [ ] `git diff --check` 후 commit한다.

```text
Demonstrate caller-owned resilience and durable fencing rejection

Constraint: Retry policy and durable epoch authority belong outside the Redis primitive.
Rejected: internal retry and PostgreSQL adapter | they would couple service policy and persistence to the lease API
Confidence: high
Scope-risk: narrow
Directive: Retry only BackendFailure with the same owner and keep business idempotency separate from fencing order.
Tested: LettuceFencingLeaseResilience4jTest; LettuceFencingLeaseRecoveryTest; fencingLeaseTopologyRecoveryTest; git diff --check
Not-tested: production-managed Redis topology remains outside the Testcontainers contract
```

## 10. Task 9 — English KDoc, bilingual README, executable documentation

**파일:**

- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseValue.kt`
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseResult.kt`
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLease.kt`
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceSuspendFencingLease.kt`
- 수정: `infra/lettuce/README.md`
- 수정: `infra/lettuce/README.ko.md`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseDocumentationTest.kt`

### 10.1 RED — KDoc와 locale parity contract

- [ ] public class/interface/object/function/property에 English KDoc이 있는지 source/reflection contract로 검증한다.
- [ ] README locale 양쪽에 같은 marker 순서와 다음 decision fragment를 요구한다.
    - opaque lease와 fencing lease 선택 기준.
    - config instance가 namespace/resource/epoch를 고정.
    - explicit bootstrap, counter loss 시 same-epoch bootstrap 금지.
    - `(epoch, sequence)` ordering과 resource-bound storage.
    - PostgreSQL `NOT NULL DEFAULT 0` tuple 및 `affectedRows == 1` strict acceptance.
    - retry/CB/bulkhead exact chain, BackendFailure-only result predicate, caller-layer exceptions.
    - pause/drain/CAS bump/bootstrap/readiness/rollout/resume, lower epoch rollback 금지.
    - O (1) `EVAL_RO` fixed-key diagnostic과 lease-only manual repair 4조건.
    - metric 허용 dimension은 operation/result/backend-or-integrity kind뿐이고 namespace/resource/owner/token/fingerprint는 label 금지.
    - `CounterUnavailable`/integrity/overflow/backend/external restore signal별 pause→diagnose→cutover action mapping.
    - exactly-once/business idempotency/durable correctness 비보장.
- [ ] documentation test에서 README 양 locale의 marker-delimited Kotlin/SQL/Lua snippet을 추출·정규화해 같은 helper/config/source와 비교하고 Kotlin resilience 예제, downstream CAS helper, diagnostic Lua를 실제 호출한다. SQL contract는 `affectedRows == 1` acceptance, `0` stale/same-token rejection, 별도 business idempotency key를 함께 검증한다.
- [ ] 양 README와 table-driven docs test에 아래 exhaustive caller action 표를 동일하게 넣는다. token을 저장하는 정상 경로는 stable resource/domain identity와 tuple을 함께 저장해야 한다.

| Result                                         | Required caller action                                                               |
|------------------------------------------------|--------------------------------------------------------------------------------------|
| `Initialized`, `AlreadyInitialized`            | readiness 확인 후 승인된 epoch rollout 계속                                          |
| `Acquired`, `AlreadyOwned`, `Owned`, `Renewed` | ownership 정상 경로 계속; token을 resource/domain identity와 함께 저장               |
| `Released`                                     | local ownership 폐기, downstream write 금지                                          |
| acquire `Contended`                            | TTL 이후 새 owner attempt 또는 bounded backoff; backend retry로 취급 금지            |
| inspect `Contended`                            | local ownership 폐기, downstream write 금지                                          |
| `Lost`, `OwnershipMismatch`                    | local ownership 폐기, downstream write 금지                                          |
| `CounterUnavailable`                           | acquire 중지, 최초 배포/history-loss 운영 판정; 결과만 보고 bootstrap 금지           |
| `SequenceExhausted`                            | retry 금지, higher-epoch cutover alert; max epoch면 domain freeze/migration          |
| `IntegrityFailure`                             | retry/mutation 중지, read-only diagnosis와 runbook 실행                              |
| `BackendFailure`                               | ambiguous completion으로 operation-specific reconcile; 정책 retry면 same owner/token |

- [ ] RED 명령을 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*FencingLeaseDocumentationTest'
```

예상 결과: README section/marker가 없어 실패한다.

### 10.2 GREEN — 문서와 KDoc를 구현에 맞춤

- [ ] public KDoc에 owner capability, token domain identity 부재, cross-epoch pre-dispatch rejection, same-owner recovery, ambiguous completion, same-slot codec validation, hot resource serialization, overflow/terminal epoch, downstream strict tuple guard를 설명한다.
- [ ] `README.md`에 English `<!-- fencing-lease:* -->` marker section을 추가하고 `README.ko.md`에 같은 marker/heading 구조의 자연스러운 Korean section을 추가한다.
- [ ] PostgreSQL 예제는 다음 exact SQL과 `affectedRows == 1` 판단을 포함한다.

```sql
ALTER TABLE guarded_resource
    ADD COLUMN fence_epoch BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN fence_sequence BIGINT NOT NULL DEFAULT 0;

UPDATE guarded_resource
SET fence_epoch = :epoch,
    fence_sequence = :sequence,
    payload = :payload
WHERE id = :id
  AND (fence_epoch, fence_sequence) < (:epoch, :sequence);
```

- [ ] EVAL_RO 진단은 파생된 exact lease/counter key만 입력하고 stable code/boolean만 출력한다. raw owner/token/value, `HGETALL`, `KEYS`, `SCAN` 예제를 문서에 넣지 않는다.
- [ ] README 양쪽에서 existing multi-key lease 설명을 유지하고 fencing이 이를 자동 대체한다고 쓰지 않는다.
- [ ] docs test와 diff check를 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test --tests '*FencingLeaseDocumentationTest'
git diff --check
```

예상 결과: bilingual marker/source parity와 executable example이 통과한다.

### 10.3 Commit

- [ ] commit한다.

```text
Document the operational boundary required for fencing safety

Constraint: Redis ordering is safe only when callers persist and compare the resource-bound tuple durably.
Rejected: API-only documentation | it would omit recovery, rollback, and resilience responsibilities
Confidence: high
Scope-risk: narrow
Directive: Keep English and Korean README contracts source-equivalent whenever fencing behavior changes.
Tested: FencingLeaseDocumentationTest; KDoc contract; git diff --check
Not-tested: rendered website is outside this module README change
```

## 11. Task 10 — Regression, static analysis, acceptance 추적

**파일:**

- Verify only; source 변경은 실패 수정에 한정한다.

### 11.1 Targeted suite

- [ ] pure unit부터 Redis integration 순으로 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test \
  --tests '*FencingLeaseValueTest' \
  --tests '*FencingLeaseResultTest' \
  --tests '*LettuceFencingLeaseSupportTest'

./gradlew :bluetape4k-lettuce:test \
  --tests '*LettuceFencingLeaseScriptTest' \
  --tests '*LettuceFencingLeaseTest' \
  --tests '*LettuceSuspendFencingLeaseTest' \
  --tests '*LettuceFencingLeaseCancellationTest' \
  --tests '*LettuceFencingLeaseConcurrencyTest' \
  --tests '*LettuceFencingLeaseResilience4jTest' \
  --tests '*LettuceFencingLeaseRecoveryTest' \
  --tests '*FencingLeaseDocumentationTest'

./gradlew :bluetape4k-lettuce:test --tests '*LettuceFencingLeaseClusterTest'
./gradlew :bluetape4k-lettuce:fencingLeaseTopologyRecoveryTest
```

예상 결과: 모든 targeted test가 통과하고 Testcontainers test가 서로 겹치지 않는다.

### 11.2 기존 API 회귀

- [ ] 기존 script와 lease test를 명시적으로 실행한다.

```bash
./gradlew :bluetape4k-lettuce:test \
  --tests 'io.bluetape4k.redis.lettuce.script.RedisScriptTest' \
  --tests '*LettuceMultiKeyLease*' \
  --tests '*MultiKeyLease*'
```

예상 결과: 기존 RedisScriptRunner와 multi-key lease contract가 그대로 통과한다.

### 11.3 Module gate와 static analysis

- [ ] full module test와 detekt를 실행한다.

```bash
repo-test-summary -- ./gradlew :bluetape4k-lettuce:test
./gradlew :bluetape4k-lettuce:detekt :bluetape4k-lettuce:detektTest
git diff --check
```

예상 결과: `:bluetape4k-lettuce:test` 0 failures, detekt 0 findings, whitespace error 0.

- [ ] changed Kotlin test 전체를 검색해 assertion/style invariant를 확인한다.

```bash
rg -n 'assertThrows|kotlin\.test\.assertFailsWith|\)\.shouldBeFalse\(\)|TO''DO|FIX''ME|T''BD' \
  infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease \
  infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease \
  infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/script/RedisScriptTest.kt
```

예상 결과: 허용되지 않은 assertion/작업 marker 0. 의도적 `shouldBeFalse()`가 있다면 boolean 상태 자체를 검증하는 경우인지 수동 확인하고 equality 대체가 가능하면 intent matcher로 바꾼다.

### 11.4 Acceptance traceability

- [ ] 아래 matrix의 각 행을 fresh test 이름/파일/명령 결과에 연결한다.

| 설계/issue acceptance                           | 구현 evidence                                      | Test evidence                                           |
|-------------------------------------------------|----------------------------------------------------|---------------------------------------------------------|
| config-bound domain, wire-byte same slot        | `FencingLeaseValue.kt`, support key derivation     | value/support/Cluster test                              |
| orderable token, serialization, redaction       | `FencingToken`, owner/result model                 | value/result round-trip, identity, crafted payload test |
| explicit bootstrap, missing counter fail-closed | bootstrap/acquire Lua                              | script hostile-state test                               |
| atomic generation and same-owner replay         | acquire Lua                                        | contract/concurrency/ambiguous completion test          |
| owner+token renew/release                       | renew/release Lua                                  | shared contract stale holder test                       |
| backend/integrity/protocol boundary             | shared decoder/classifier                          | support + three-adapter parity test                     |
| future/coroutine cancellation                   | runner bridge + suspend wrapper                    | RedisScript/cancellation test                           |
| fixed O(1) preflight/reply                      | Lua source and decoder                             | script structural/hostile test                          |
| standalone/Cluster parity                       | two facades + shared support                       | standalone/Cluster test                                 |
| external Retry/CB/Bulkhead only                 | test/example code                                  | resilience + docs test                                  |
| downstream strict tuple guard                   | no production adapter                              | recovery/docs CAS fixture                               |
| epoch recovery/lower rollback prohibition       | docs + test harness                                | recovery/docs test                                      |
| tagged topology promotion/restore recovery      | opt-in Testcontainers task                         | `fencingLeaseTopologyRecoveryTest`                      |
| bilingual docs/KDoc                             | README locale + public source                      | documentation test                                      |
| existing API unchanged                          | no old API edits except shared runner behavior fix | RedisScript + MultiKeyLease regression                  |

- [ ] review 결과 P0=0/P1=0인지 확인하고 P2/P3는 수정하거나 근거와 함께 PR review note에 남긴다.

### 11.5 Final implementation commit

- [ ] verification 중 필요한 수정이 있었다면 하나의 bounded commit으로 마무리한다. 수정이 없으면 빈 commit을 만들지 않는다.

```text
Close the fencing lease verification gaps before review

Constraint: Issue 1068 requires fresh module, static-analysis, serialization, cancellation, Cluster, and documentation evidence.
Rejected: targeted tests alone | they cannot prove existing Lettuce lease compatibility
Confidence: high
Scope-risk: narrow
Directive: Re-run the exact affected gate after any review fix before updating the PR head.
Tested: targeted fencing suite; opt-in topology recovery; existing RedisScript and MultiKeyLease suite; module test; detekt; git diff --check
Not-tested: production-managed Redis topology
```

## 12. 구현 완료 후 delivery gate

- [ ] branch가 `feature/issue-1068-fencing-lease`, base가 latest `origin/develop`인지 확인한다.
- [ ] exact local HEAD를 push하고 public PR title/body는 English로 작성한다. PR은 issue #1068을 연결하고 issue milestone/assignee/labels를 미러링한다.
- [ ] PR body에 acceptance traceability, exact commands/results, tagged topology task 결과, production-managed topology boundary, no new dependency/module을 기록한다.
- [ ] CI, current review, unresolved thread, human-review artifact를 exact head에서 확인한다.
- [ ] merge-ready 상태를 보고한 뒤 별도의 사용자 merge 승인을 기다린다. plan/spec 승인은 merge 승인으로 간주하지 않는다.
- [ ] merge 승인 뒤에만 rebase merge하고 local `develop` fast-forward, merged worktree/branch cleanup을 수행한다.

## 13. Plan 완료 조건

- implementation 시작 전 plan 자체를 performance, stability, security, Ops, API, caller 관점에서 독립 검토한다.
- P0=0, P1=0이 아니면 plan을 수정하고 해당 관점을 새 review lane에서 재검토한다.
- spec 17절 acceptance criteria가 Task 1–10과 traceability matrix에 모두 연결돼야 한다.
- plan file만 commit하고 Kotlin/README 구현은 다음 명시적 승인 전 시작하지 않는다.
