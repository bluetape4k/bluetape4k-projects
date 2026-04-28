# Wave 5 Tests/Types/SilentFail Review — 2026-04-28

전체 5개 그룹 병렬 실행 결과. Tier 5 (Tests, Type Design, Silent Failures) 기준.

## 전체 요약

| 그룹 | CRITICAL | HIGH | MEDIUM |
|------|----------|------|--------|
| core + testing | 1 | 7 | 6 |
| io + texts | 0 | 3 | 4 |
| data modules | 0 | 3 | 5 |
| aws + infra | 1 | 5 | 5 |
| spring-boot + utils + vt | 0 | 4 | 5 |
| **합계** | **2** | **22** | **25** |

---

## CRITICAL

### C1 — PublishSubject/BehaviorSubject/MulticastSubject emit이 부모 CancellationException 삼킴 (core/coroutines)
- **파일:**
  - `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/PublishSubject.kt:113-121`
  - `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/BehaviorSubject.kt:144-160`
  - `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/MulticastSubject.kt:91-100, 113-122, 132-143`
- **이슈:** `emit/emitError/complete` 내부에서 `collector.next(value)` (suspending) 호출 시 `catch (e: CancellationException) { remove(collector) }` — 부모 컨텍스트 취소를 collector 내부 취소로 오인하여 삼킴. 취소된 producer가 나머지 collector에 계속 publish함.
- **수정:**
  ```kotlin
  catch (e: CancellationException) {
      currentCoroutineContext().ensureActive() // 부모 취소 시 rethrow
      remove(collector)
  }
  ```

### C2 — KafkaCodec deserialization `catch(Throwable) → null` (infra/kafka)
- **파일:** `infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt:99-105`
- **이슈:** 모든 `Throwable`을 잡아 `null` 반환 → offset 자동 commit으로 poison pill 건너뜀.
- **설계 트레이드오프 (사용자 확인):** `null` 반환 방식은 Consumer에서 offset 관리를 단순화하는 의도적 선택. 단, Kafka 압축 시 tombstone(의도적 삭제)과 구분 불가. DLQ 라우팅 필요 시 `SerializationException` throw 방식으로 전환 필요.
- **현재 방향:** `null` 반환 유지 시 최소한 `.onFailure { log.warn(it) { "deserialization failed" } }` 추가하여 poison pill 발생을 로그로 관찰 가능하게 할 것.

---

## HIGH

### core + testing (H1~H7)

**H1 — BehaviorSubject.emitError runCatching 무음 삼킴 (core/coroutines)**
- **파일:** `bluetape4k/coroutines/.../subject/BehaviorSubject.kt:186-191`
- **이슈:** `runCatching { ... }` no `.onFailure` — `CancellationException` 포함 전부 삼킴. 동일 파일 `complete()` (line 217-222)은 로그 있음 — 불일치.
- **수정:** `.onFailure { log.error(it) { "..." } }` 추가 + CancellationException rethrow

**H2 — WebFlux WebContentController.byName: CancellationException → 404 (testing/mock-webflux-server)**
- **파일:** `testing/mock-webflux-server/.../WebContentController.kt:58-62`
- **이슈:** `runCatching { ... }.getOrElse { ResponseEntity.notFound().build() }` — suspend 컨트롤러에서 CancellationException이 404로 응답됨. IO 오류도 404로 마스킹.
- **수정:**
  ```kotlin
  return try {
      ResponseEntity.ok(withContext(Dispatchers.IO) { loader.load(name) })
  } catch (e: CancellationException) { throw e }
  catch (e: IllegalArgumentException) { ResponseEntity.notFound().build() }
  ```

**H3 — MVC WebContentController.byName: 모든 오류 → 404 (testing/mock-web-server)**
- **파일:** `testing/mock-web-server/.../WebContentController.kt:38-40`
- **이슈:** `runCatching { ResponseEntity.ok(loader.load(name)) }.getOrElse { ResponseEntity.notFound().build() }` — IO 오류/캐시 오류가 404로 마스킹됨.
- **수정:** `IllegalArgumentException`만 catch하여 notFound 반환; 나머지는 propagate.

**H4 — withTimeoutOrNull 모든 예외 → null (core)**
- **파일:** `bluetape4k/core/.../support/TimeoutSupport.kt:102-105`
- **이슈:** `runCatching { asyncRunWithTimeout(...).get() }.getOrNull()` — timeout 외 NPE/ISE도 null 반환. KDoc `@throws TimeoutException` 약속이 깨짐.
- **수정:** `TimeoutException`/`ExecutionException(TimeoutException)` 만 null 반환; 나머지 rethrow.

**H5 — CompletableFuture.join(duration) / joinOrNull 모든 예외 → default/null (core)**
- **파일:** `bluetape4k/core/.../concurrent/CompletableFutureSupport.kt:546-547, 560-561`
- **이슈:** `runCatching { ... }.getOrDefault()` / `.getOrNull()` — Future 내부 예외(NPE, 비즈니스 오류)가 default/null로 삼킴.
- **수정:** `TimeoutException`만 null/default 반환; 나머지 rethrow.

**H6 — tryForEach/mapIfSuccess/mapCatching/forEachCatching이 CancellationException 삼킴 (core)**
- **파일:**
  - `bluetape4k/core/.../collections/IterableSupport.kt:330-331, 344-346, 365-366, 380-381`
  - `bluetape4k/core/.../collections/SequenceSupport.kt:301, 312, 330, 346`
  - `bluetape4k/core/.../support/ArraySupport.kt:399-595`
- **이슈:** `runCatching { mapper(it) }` — suspend lambda에서 CancellationException 삼킴. coroutine 사용자의 structured concurrency 파괴.
- **수정:** CancellationException rethrow guard 추가, 또는 "suspend lambda 사용 금지" KDoc 명시.

**H7 — KLoggingChannelTest/StopwatchExtensionTest 어서션 없음 (testing)**
- **파일:**
  - `bluetape4k/logging/.../KLoggingChannelTest.kt:26-69` — 7개 `@Test` 전부 어서션 없음
  - `testing/junit5/.../StopwatchExtensionTest.kt:13-22` — `Thread.sleep(10)` 후 어서션 없음
- **이슈:** 로깅 파이프라인이 모든 이벤트를 drop해도 테스트 통과.
- **수정:** `ListAppender` 주입 후 이벤트 카운트/레벨/메시지 assert. Stopwatch는 `OutputCapturer` 또는 `Clock` 주입으로 측정값 검증.

---

### io + texts (H8~H10)

**H8 — VertxTestSupport.withSuspendTestContext: CancellationException → null 반환 (io/vertx)**
- **파일:** `io/vertx/.../tests/VertxTestSupport.kt:63`
- **이슈:** `catch (e: Throwable) { testContext.failNow(e); null }` — CancellationException 삼켜 null 반환. 부모 scope 취소가 "failed test"로 오인됨.
- **수정:** `catch (e: CancellationException) { throw e }` 선행 catch 추가 (PoolSupport.kt 패턴 참조).

**H9 — VerxTestSupport testWithSuspendTransaction/Rollback: CancellationException → failNow (io/vertx)**
- **파일:** `io/vertx/.../sqlclient/tests/VerxTestSupport.kt:37, 69`
- **이슈:** suspend inline 함수에서 `catch (Throwable)` → `testContext.failNow(e)` — CancellationException을 테스트 실패로 처리.
- **수정:** `catch (e: CancellationException) { throw e }` 선행 추가.

**H10 — AbstractCompressor.decompress: corrupt input → emptyByteArray 무음 반환 (io)**
- **파일:** `io/io/.../compressor/AbstractCompressor.kt:51, 77`
- **이슈:** `catch(Throwable) { return emptyByteArray }` — 손상된 압축 데이터를 빈 배열로 반환. caller는 empty가 "빈 payload"인지 "압축 해제 실패"인지 구분 불가. 데이터 손실 path.
- **수정:** `CompressionException` throw, 또는 `Result<ByteArray>` 반환. 레거시 유지 시 `decompressOrThrow` 엄격 변형 노출.

---

### data modules (H11~H13)

**H11 — EntityManagerSupport.deleteById: 모든 예외 삼킴 (data/hibernate)**
- **파일:** `data/hibernate/.../EntityManagerSupport.kt:219-223`
- **이슈:** `runCatching { em.remove(entity) }` no `.onFailure` — `OptimisticLockException` 포함 모든 DB 예외가 삼켜짐. 삭제 실패가 성공으로 보임.
- **수정:** `.onFailure { log.error(it) { "deleteById failed" } }` 추가 후 rethrow 또는 Result 반환.

**H12 — StatelessSessionSupport.withStateless: rollback 실패 무음 (data/hibernate)**
- **파일:** `data/hibernate/.../stateless/StatelessSesisonSupport.kt:37-43`
- **이슈:** rollback 실패가 완전 무음. 원본 예외에 `addSuppressed` 없음.
- **수정:** `rollback()` 실패 시 `originalException.addSuppressed(rollbackException)` 패턴 적용.

**H13 — MongoClientExtensions.inTransaction: abort 실패 무음 (data/mongodb)**
- **파일:** `data/mongodb/.../MongoClientExtensions.kt:98-113`
- **이슈:** transaction abort 실패가 silently drop됨. addSuppressed 없음.
- **수정:** abort failure → `originalException.addSuppressed(abortException)` 후 rethrow.

---

### aws + infra (H14~H18)

**H14 — DynamoDB existsTable: IAM 거부/네트워크 오류 → false 반환 (aws)**
- **파일:** `aws/aws/.../dynamodb/enhanced/DynamoDbEnhancedClientExtensions.kt:93-97`
- **이슈:** 모든 예외를 catch하여 false 반환 → IAM 권한 오류를 "테이블 없음"으로 오인.
- **수정:** `ResourceNotFoundException`만 false 반환; `AccessDeniedException`/네트워크 오류는 rethrow.

**H15 — LettuceLoadedMap: Redis read 실패 → cache miss 무음 반환 (infra/lettuce)**
- **파일:** `infra/lettuce/.../map/LettuceLoadedMap.kt:117, 168`
- **이슈:** Redis 읽기 실패가 로그 없이 cache miss로 처리됨. Redis 장애 감지 불가.
- **수정:** `.onFailure { log.warn(it) { "Redis read failed, falling back to loader" } }` 추가.

**H16 — Redisson suspend 함수들: runCatching이 CancellationException 삼킴 (infra/redisson)**
- **파일:** `infra/redisson/.../RedissonClientCoroutine.kt:67-69` 외 7개 파일
- **이슈:** `runCatching { ... }` in suspend 함수 — CancellationException 삼켜 structured concurrency 파괴.
- **수정:** `runCatching` 내부에서 `exceptionOrNull()?.let { if (it is CancellationException) throw it }` 또는 try/catch 분리.

**H17 — Micrometer ObservationCoroutinesSupport: CancellationException → SLO 오류 기록 (infra/micrometer)**
- **파일:** `infra/micrometer/.../coroutines/ObservationCoroutinesSupport.kt:191-201`
- **이슈:** `CancellationException`이 Micrometer error로 기록됨 → SLO alert 오염.
- **수정:** `CancellationException` 분기하여 observation error로 기록하지 않음.

**H18 — DeferredValueTest: Thread.sleep(1500)과 runTest virtual time 혼용 (testing)**
- **파일:** `bluetape4k/coroutines/.../DeferredValueTest.kt:99-126`
- **이슈:** `runTest` (virtual time) 내에서 `Thread { Thread.sleep(1_500) }` 사용 → CI 환경에서 flaky.
- **수정:** `currentTime` 진행 방식 또는 `runBlocking` + 결정적 trigger로 교체.

---

### spring-boot + utils + vt (H19~H22)

**H19 — BingAddressFinder.findAddress: runCatching 무음 (utils/geo)**
- **파일:** `utils/geo/.../geocode/bing/BingAddressFinder.kt:48-57`
- **이슈:** Bing API 인증 오류/throttle/네트워크 실패가 `null` 반환으로 무음 처리.
- **수정:** `.onFailure { log.warn(it) { "Bing reverse geocode failed for $geocode" } }.getOrNull()`

**H20 — DefaultRuleEngine listener 예외 무음 삼킴 (utils/rule-engine)**
- **파일:** `utils/rule-engine/.../DefaultRuleEngine.kt:198-208`
- **이슈:** `onBeforeRules`/`onAfterRules` listener 예외를 `runCatching`으로 삼킴. `.onFailure` 없음.
- **수정:** `.onFailure { log.warn(it) { "RuleEngineListener.${method} threw — ${listener.javaClass.name}" } }` 추가.

**H21 — VirtualThreads/StructuredScopes ServiceLoader 오류 무음 (virtualthread/api)**
- **파일:**
  - `virtualthread/api/.../VirtualThreads.kt:35-38`
  - `virtualthread/api/.../StructuredScopes.kt:182-185`
- **이슈:** `runCatching { iterator.next() }.getOrNull() ?: break` — `ServiceConfigurationError` 무음. provider 선택 실패가 로그 없이 fallback.
- **수정:** `.onFailure { log.warn(it) { "ServiceLoader iteration failed" } }` 추가.

**H22 — VirtualThread 테스트: 실제 VT 사용 여부 미검증 (virtualthread)**
- **파일:**
  - `virtualthread/api/.../VirtualThreadsTest.kt:15-64`
  - `virtualthread/jdk21/.../Jdk21StructuredTaskScopeProviderTest.kt`
  - `virtualthread/jdk25/.../Jdk25StructuredTaskScopeProviderTest.kt`
- **이슈:** `Thread.currentThread().isVirtual` 어서션 없음. platform thread fallback 발생해도 테스트 통과.
- **수정:** `executor.submit<Boolean> { Thread.currentThread().isVirtual }.get().shouldBeTrue()` 추가.

---

## MEDIUM (25건 — 주요 항목)

### core + testing (M1~M6)
- M1: `AwaitilityCoroutines.kt:102` — `async { runCatching { block() } }` CancellationException을 exceptionIgnorer로 전달 위험
- M2: `FlowEvent.Value<T>/Error` — hot-path data class → `@JvmInline value class` 후보 (할당 절감)
- M3: 25+ 파일의 `@Suppress("UNCHECKED_CAST")` sentinel 패턴 — `Sentinels` helper로 중앙화 권장
- M4: `MulticastSubject.emit/error/complete` — `Unit` 반환으로 collector 취소 정보 유실. `EmitResult` sealed type 고려
- M5: `SuspendLazyTest:74,94,119` — `runTest` 내 `Thread.sleep(Random.nextLong(100))` → virtual time 무효화, `delay(...)` 교체
- M6: `LockSupportTest/GraphTest/FutureUtilsTest/ShutdownQueueTest` — `Thread.sleep` 기반 순서 보장 → Awaitility 또는 명시적 signaling으로 교체

### io + texts (M7~M10)
- M7: `FlowCsvWriterImpl.kt:75` — `close()` 내 `runCatching { flush(); close() }` 전체 삼킴. flush 실패는 데이터 손실
- M8: `AbstractGrpcClient.kt:45`, `AbstractGrpcInprocessClient.kt:59` — `runCatching` 내 `awaitTermination` throw 시 `shutdownNow` 미실행
- M9: `texts/tokenizer-core/.../CharArrayMap.kt:21` — 클래스 레벨 `@Suppress("UNCHECKED_CAST")` → 사이트 레벨로 좁혀야
- M10: `texts/tokenizer-core/.../model/BlockwordRequest.kt:19` — `text: String` / `mask: String` 혼동 가능 → `@JvmInline value class BlockwordText/BlockwordMask`

### data modules (M11~M15)
- M11: `ConnectionPoolSupportTest.kt:212` — `runBlocking` → `runTest` 교체
- M12: Benchmark teardown `runCatching` — `.onFailure { log.warn }` 없음
- M13: `ExposedJdbcBatchJobRepository.kt:134-156` — `catch(Exception)` in suspend → `catch(CancellationException) { throw it }` 명시 선행
- M14: MongoDB transaction timeout 설정 없음 — abort 없이 무한 대기 가능
- M15: R2DBC `TransactionExtensions.kt:17-18` KDoc vs 실제 CancellationException 처리 불일치

### aws + infra (M16~M20)
- M16: `DynamoDbCoroutineRepository.kt:127-133` — `deleteAll` 무제한 fan-out (최대 64 동시 요청)
- M17: Kafka poison pill null 반환 시 tombstone 구분 불가 (설계 트레이드오프 — 위 C2 참조)
- M18: `SearchApiCoroutines.kt:160-166` — ES PIT close 실패 무음
- M19: `CacheInvalidationStrategy.kt:64-80` — Redis 실패 로그 없음
- M20: `ObservationCoroutinesSupport` CancellationException → `setError` 호출 (SLO 오염, 위 H17)

### spring-boot + utils + vt (M21~M25)
- M21: `Jdk21StructuredTaskScopeProvider.kt:164-168` / `Jdk25StructuredTaskScopeProvider.kt:217-221` — `@Suppress("UNCHECKED_CAST")` 광범위 cast, 최소한 주석으로 안전성 근거 명시
- M22: `utils/geo/.../Geocode.kt:64-71` — delimiter 없을 때 `IndexOutOfBoundsException` → `require(splits.size == 2) { ... }` 교체
- M23: `utils/jwt/.../JwtComposer.kt:39-40,116-127` — custom claims `MutableMap<String, Any>` — 타입 안전성 없음, typed `JwtClaims` 고려
- M24: `utils/jwt/.../JwtReaderExpirationTest.kt` — 서명 불일치 실패 path 테스트 없음 (cross-kid 시나리오)
- M25: `SuspendParallelFlow.kt:116` — `channel.trySend(report)` 반환값 무시 → 채널 닫힘 시 report 유실, 로그 없음

---

## 수정 우선순위

| 순위 | 심각도 | 건수 |
|------|--------|------|
| 1 | CRITICAL | 2 |
| 2 | HIGH | 22 |
| 3 | MEDIUM | 25 |

### 주요 수정 순서
1. **C1** Subject emit CancellationException 구분 (`ensureActive()` 추가)
2. **C2** KafkaCodec — null 반환 유지 시 최소 warn 로그 추가
3. **H2~H3** WebContentController (MVC + WebFlux) CancellationException rethrow
4. **H4~H5** `withTimeoutOrNull` / `CompletableFuture.join` — TimeoutException만 null
5. **H6** `tryForEach`/`mapCatching` 계열 CancellationException guard
6. **H7** KLoggingChannelTest / StopwatchExtensionTest 어서션 추가
7. **H8~H9** Vertx test helper CancellationException rethrow
8. **H10** AbstractCompressor.decompress — CompressionException throw
9. **H11~H13** Hibernate/MongoDB silent rollback/delete failure → addSuppressed
10. **H14** DynamoDB existsTable — ResourceNotFoundException만 false 반환
11. **H15~H17** Lettuce/Redisson/Micrometer silent failure 로그 추가
12. **H19~H21** BingAddressFinder/RuleEngine/ServiceLoader `.onFailure` 로그
13. **H22** VirtualThread 테스트 `isVirtual` 어서션

---

*Wave 1 결과: docs/security-review/2026-04-28-wave1-security.md*
*Wave 2 결과: docs/security-review/2026-04-28-wave2-ops-sre.md*
*Wave 3 결과: docs/security-review/2026-04-28-wave3-kotlin-idiom.md*
*Wave 4 결과: docs/security-review/2026-04-28-wave4-performance.md*
