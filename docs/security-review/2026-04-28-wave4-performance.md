# Wave 4 Performance & Stability 검토 — 2026-04-28

전체 5개 그룹 병렬 실행 결과. Tier 5 (Performance/Stability) 기준.

## 전체 요약

| 그룹                     | CRITICAL | HIGH   | MEDIUM |
|--------------------------|----------|--------|--------|
| core + testing           | 3        | 6      | 7      |
| io + texts               | 1        | 5      | 3      |
| data modules             | 0        | 3      | 3      |
| aws + infra              | 1        | 7      | 7      |
| spring-boot + utils + vt | 0        | 4      | 4      |
| **합계**                 | **5**    | **25** | **24** |

---

## CRITICAL

### C1 — OutputCapturer.finishCapture () JVM System.out 영구 닫힘 (testing/junit5)

- **파일:** `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/output/OutputCapturer.kt:98-105`
-

**이슈:** `finishCapture()`가 `System.setOut(origin)`으로 복원 후 `captureOut?.close()` 호출 → `CaptureOutputStream.close()`가 복원된 `origin`을 닫음.
`PrintStream.close()`는 FD-1 닫음 → 이후 JVM 전체에서 `System.out.println()`, 로거 콘솔 어펜더 모두 무효화. Gradle `forkEvery=0` 환경에서 첫 `@OutputCapture` 테스트 후 후속 테스트 전체가 무소음으로 깨짐.
- **수정:** `CaptureOutputStream.close()`에서 `copy`(in-memory 버퍼)만 닫고 `origin`은 절대 닫지 않음.

### C2 — Runtimex.compactMemory () 256GB 할당으로 OOM 유발 (core)

- **파일:** `bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/Runtimex.kt:111-122`
-

**이슈:** `repeat(128) { unused.add(ByteArray(2_000_000_000)) }` — 의도적 OOM 유발. 로컬 OOM은 catch되지만, 할당 압력이 JVM 전체에 전파 → 동시 실행 스레드가 uncaught `OutOfMemoryError` 발생 가능.
`ArrayList`가 catch block까지 유지되어 GC 자체를 방해.
- **수정:** 할당 루프 전체 제거. `System.gc()` 만 호출. 강제 압박 테스트는 테스트 픽스처로 이동.

### C3 — InMemoryLogbackAppender 무제한 이벤트 누적 (testing/junit5)

- **파일:** `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/output/InMemoryLogbackAppender.kt:80,91-94,103-105`
- **이슈:** `ConcurrentLinkedDeque<ILoggingEvent>` 크기 제한 없음. `stop()` 미호출 시 Logger에 영원히 부착 → 장기 Gradle JVM에서 힙 무한 누수.
  `ILoggingEvent`당 message + arguments + MDC + caller data 보유. busy-poll `Thread.sleep(1)` (line 73)도 VT carrier 핀닝.
-

**수정:** (1) 최대 이벤트 수 설정 (`maxEvents`) 추가, 초과 시 head 제거. (2) `AutoCloseable` 구현 + `use {}` / `@AfterEach` 필수 KDoc 명시. (3) busy-poll → 단일 check로 교체.

### C4 — LettuceSuspendedLoadedMap.close () scope 취소 중 runBlocking → 데드락 (infra/lettuce)

- **파일:** `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceSuspendedLoadedMap.kt:316-338`
- **이슈:** `writeBehindJob?.cancel()` 후 동일 루프에서 `runBlocking { flushBatch(batch) }` 호출.
  `flushBatch`가 취소된 scope 또는 포화된 IO dispatcher에 의존 시 무한 대기. Lettuce 이벤트 루프/Spring Boot lifecycle 스레드에서 `runBlocking` → 스레드 풀 기아/데드락.
-

**수정:** consumer job 먼저 취소하지 말고 `channel.close()` 후 `writeBehindJob?.join(timeout)` 사용. 별도 단일 스레드 executor에서 `runBlocking(Dispatchers.IO) { flushBatch(...) }` 실행.

### C5 — Vertx resilience4j 매 호출 ScheduledExecutorService 생성, shutdown 없음 (io/vertx)

- **파일:**
    - `io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRetrySupport.kt:22,43`
    - `io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureTimeLimiterSupport.kt:23,42`
    - `io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxDecorators.kt:83,91`
-

**이슈:** `scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()` — 매 호출 평가. 명시적 `scheduler` 없이 호출 시마다 새 플랫폼 스레드 생성, 절대 shutdown 없음 → 스레드 무한 누수 → OOM.
- **수정:** `RetrofitCallSupport.kt:8-12` / `FileSupport.kt:38-47` 패턴 참조:
  ```kotlin
  private val defaultRetryScheduler: ScheduledExecutorService by lazy {
      Executors.newSingleThreadScheduledExecutor { r ->
          Thread.ofVirtual().name("vertx-retry-scheduler").unstarted(r)
      }.also { exec -> Runtimex.addShutdownHook { runCatching { exec.shutdown() } } }
  }
  ```

---

## HIGH

### core + testing

#### H1 — withWorkStealingPool 매 호출 ForkJoinPool 생성 + invokeAll 블로킹 (core)

- **파일:** `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/ExecutorSupport.kt:71-91`
-

**이슈:** 매 호출 `Executors.newWorkStealingPool(parallelism)` → 새 ForkJoinPool 생성. `executor.invokeAll()` → 호출 스레드 블로킹. `CompletableFuture` 반환 타입과 모순, 반환 시 이미 완료됨.
-
**수정:** `CompletableFuture.supplyAsync({ ... }, executor)` 내부에서 `invokeAll` 실행. 공유 `ForkJoinPool.commonPool()` 사용 또는 외부 pool 주입 허용.

#### H2 — ThreadPoolCoroutineScope.close () 원자적 guard 우회 (coroutines)

- **파일:** `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/ThreadPoolCoroutineScope.kt:57-60`
-

**이슈:** `super.close()` / `_closed.compareAndSet` 없이 `clearJobs()`와 `dispatcher.close()` 직접 호출 → `scopeClosed` 영원히 false, 이중 `dispatcher.close()` 호출 가능.
- **수정:** `super.close()` 호출 후 CAS 블록 내에서만 `dispatcher.close()` 실행.

#### H3 — Flow.replay {} 무제한 ReplaySubject 기본값 (coroutines)

- **파일:** `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/replay.kt:22-23`
- **이슈:** 인수 없는 `replay { }` → `UnboundedReplayBuffer` → 장기 실행 스트림에서 모든 값 영원히 보존 → 힙 무한 누수.
- **수정:** 인수 없는 오버로드를 `@Deprecated(replaceWith = "replay(size, transform)")` 처리. `replayUnbounded { }` 명시 opt-in 제공.

#### H4 — FlowGroup (groupBy) 미수집 그룹 → upstream 전체 데드락 (coroutines)

- **파일:** `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/groupBy.kt:275-339`
- **이슈:** `FlowGroup.next(value)`가 `consumerReady.await()` 블로킹 — 다운스트림이 그룹을 collect 안 하면 source 코루틴 영구 차단, 다른 모든 그룹도 중단.
- **수정:** 미수집 그룹 타임아웃/취소 연결 추가. 또는 per-group 버퍼 + overflow 전략 (`DROP_OLDEST` 등) 도입.

#### H5 — ConcurrentReducer.add () pump () 내부 10ms 블로킹 (core)

- **파일:** `bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/ConcurrentReducer.kt:71-80,93-103`
-

**이슈:** `add()`가 `pump()` 동기 호출 → `queue.poll(timeout=10ms)` → 호출 스레드 (HTTP 요청/이벤트 루프) 차단. `close()` 후 `shutdownNow()` 없어 pump task가 종료 중인 executor에서 실행.
- **수정:** `add()`에서 첫 `pump()`를 `pumpExecutor`에 스케줄, 직접 호출 불가. `close()`에서 `awaitTermination` + 미처리 promise 취소.

#### H6 — @Cacheable on suspend function 동작 불확실 (testing)

- **파일:** `testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/ImageLoaderService.kt:40-69`
-

**이슈:** Spring 6.1+ suspend `@Cacheable` 부분 지원. SpEL/condition/return-type 래핑에 알려진 edge case. CPU-bound 이미지 인코딩에 `Dispatchers.IO` 사용 (잘못된 dispatcher).
- **수정:** `@Cacheable` 제거 후 `cacheManager.getCache(...)` 수동 처리. 이미지 인코딩은 `Dispatchers.Default` 사용.

### io + texts

#### H7 — DictionaryProvider InputStream 미닫힘 (texts)

- **파일:** `texts/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/utils/DictionaryProvider.kt:42-76`
-

**이슈:** `readStreamByLine`이 `InputStreamReader(stream).buffered().lineSequence()` 반환 후 Reader 미닫힘. 한국어 사전 30개 파일 로드마다 파일 핸들 누수.
- **수정:** `useLines { it.toList() }` 패턴 또는 consumer 함수 형태로 교체.

#### H8 — JapaneseDictionaryProvider CharArraySet 뮤테이션 비동기화 (texts)

-

**파일:** `texts/tokenizer-japanese/src/main/kotlin/io/bluetape4k/tokenizer/japanese/utils/JapaneseDictionaryProvider.kt:116-159`
-
**이슈:** `addBlockwords`, `removeBlockwords`, `clearBlockwords`가 공유 `CharArraySet` 동기화 없이 뮤테이션. Korean 버전은 `synchronized(mutationLock)` 있으나 Japanese는 없음. 동시 `addAll`/`removeAll` → 내부 배열 corruption.
- **수정:** Korean 패턴 미러 적용. `private val mutationLock = Any()` 추가 + synchronized 블록.

#### H9 — FlowCsvWriterImpl.writeFile 블로킹 I/O + 버퍼 없음 (io/csv)

- **파일:** `io/csv/src/main/kotlin/io/bluetape4k/csv/v2/FlowCsvWriterImpl.kt:51-72`
-

**이슈:** `suspend` 함수 내 `withContext(Dispatchers.IO)` 없이 `FileOutputStream` 직접 사용. `OutputStreamWriter` 버퍼 없음 → write당 OS 시스템콜.
- **수정:** `withContext(Dispatchers.IO)` 내부로 이동 + `.buffered()` 추가.

#### H10 — AbstractGrpcClient ShutdownQueue 미등록 (io/grpc)

- **파일:** `io/grpc/src/main/kotlin/io/bluetape4k/grpc/AbstractGrpcClient.kt:29-40`
-

**이슈:** 서버 (`AbstractGrpcServer`)는 `ShutdownQueue.register { stop() }` 있으나 클라이언트는 없음 → close () 미호출 시 Netty 이벤트 루프/TCP 소켓 누수.
- **수정:** `init { ShutdownQueue.register { runCatching { close() } } }` 추가.

#### H11 — KoreanDictionaryProvider lazy init runBlocking 블로킹 (texts)

-

**파일:** `texts/tokenizer-korean/src/main/kotlin/io/bluetape4k/tokenizer/korean/utils/KoreanDictionaryProvider.kt:101,161,234,257,283,318,343,383`
-
**이슈:** lazy 프로퍼티들이 `runBlocking(Dispatchers.IO)` → WebFlux handler, Vert.x 이벤트 루프에서 첫 접근 시 수 초간 스레드 차단. 단일 스레드 reactor에서 데드락 가능.
-
**수정:** `suspend fun preload()` / `suspend fun nounDictionary()` API 제공. 애플리케이션 시작 시 (`ApplicationRunner` 등) 명시적 preload.

### data modules

#### H12 — 매 호출 R2dbcTransactionManager 생성 (data/r2dbc)

- **파일:** `data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/TransactionSupport.kt:45`
-

**이슈:** `withTransactionSuspend`가 `R2dbcTransactionManager(this.connectionFactory)` 매 호출 생성. 싱글톤이어야 할 객체가 가비지 생성 + 트랜잭션 리스너/에러 translator 설정 우회.
- **수정:** `WeakHashMap<ConnectionFactory, R2dbcTransactionManager>` 캐시 또는 `ReactiveTransactionManager` 파라미터 주입.

#### H13 — R2DBC maxPendingAcquire=-1 (무제한) 기본값 (data/r2dbc)

- **파일:** `data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/R2dbcPoolConfig.kt:60,121`
-

**이슈:** 기본값 `DEFAULT_MAX_PENDING_ACQUIRE = -1` → 무제한 pending 큐 → 부하 급증 시 `MonoSink` 큐 무한 성장 → heap OOM. timeout이 circuit-breaker 역할 불가.
- **수정:** 기본값을 `maxSize × 4` 또는 고정 1000으로 변경. 또는 무제한 사용 시 런타임 경고.

#### H14 — EntityManager.findAll () 전체 테이블 무제한 로드 (data/hibernate)

- **파일:** `data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerSupport.kt:334`
- **이슈:** `newQuery(clazz).resultList` — 페이지네이션 없이 `SELECT * FROM <entity>` 전체. Hibernate 1차 캐시에도 모든 엔티티 핀.
- **수정:** `firstResult`/`maxResults` 파라미터 필수화 또는 기본값 1000 적용.

### aws + infra

#### H15 — Lettuce 데드레터 LPUSH+HSET 비원자적 (infra/lettuce)

- **파일:** `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceSuspendedLoadedMap.kt:303-310`
    + `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceLoadedMap.kt:314-319`
- **이슈:** `LPUSH` 성공 후 `HSET` 실패 시 키 목록이 존재하지 않는 데이터 참조 → recovery 파손.
- **수정:** `MULTI/EXEC` 또는 Lua 스크립트로 두 커맨드 원자화.

#### H16 — LettuceLock.lock () 무한 busy-loop 타임아웃 없음 (infra/lettuce)

- **파일:** `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceLock.kt:132-146`
- **이슈:** `while (true) { Thread.sleep(50) }` — Redis 불가용 시 호출 스레드 무한 차단, VT carrier 핀닝.
  `lockAsync()`는 `maxWaitTime=5분` 있으나 `lock()`은 없음.
- **수정:** `maxWaitTime: Duration = Duration.ofMinutes(5)` 파라미터 추가. 데드라인 초과 시 `IllegalStateException`.

#### H17 — LettuceLoadedMap 쓰기 백로그 무제한 성장 (infra/lettuce)

- **파일:** `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceLoadedMap.kt:91-98,290-323`
- **이슈:** 매 tick `batchSize`만 처리 → 생산자 속도 > `batchSize/delay`이면 `LinkedBlockingDeque` 무한 성장 → OOM.
- **수정:** tick당 큐 소진될 때까지 반복 (max-time-per-tick 상한 적용). 큐 depth 메트릭 제공.

#### H18 — RedissonNearCache DefaultLocalCacheMapOptions 공유 가변 싱글톤 (infra/redisson)

- **파일:** `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/nearcache/RedissonNearCache.kt:51-59`
-

**이슈:** `DefaultLocalCacheMapOptions`가 `lazy` 싱글톤, name=`"default"` → 다른 캐시들이 동일 Redis 키 충돌. `LocalCachedMapOptions`는 mutable → 한 캐시 설정이 다른 캐시에 영향.
- **수정:** 팩토리 함수 `fun defaultLocalCacheOptions(name: String): LocalCachedMapOptions<...>` 로 교체 → 호출마다 새 인스턴스.

#### H19 — SuspendCacheImpl keyLocks Mutex TOCTOU race (infra/resilience4j)

- **파일:** `infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/cache/impl/SuspendCacheImpl.kt:33-42,76-95`
-

**이슈:** `releaseMutex`의 `!mutex.isLocked` 체크와 `keyLocks.remove(key, mutex)` 사이 race → 새 소유자가 획득한 Mutex 삭제 → 직렬화 파괴 → 동시 loader 중복 실행.
- **수정:** Caffeine weak-reference 캐시 또는 striped lock pool (`key.hashCode() % N`) 사용.

#### H20 — BulkIngesterCoroutines Channel.UNLIMITED 무제한 메모리 (infra/elasticsearch)

- **파일:** `infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/coroutines/BulkIngesterCoroutines.kt:272`
- **이슈:** `Channel(Channel.UNLIMITED)` — listener Flow 미수집 또는 느린 수집 시 BulkRequest+context가 힙에 무한 누적.
- **수정:** `Channel(Channel.BUFFERED)` + `BufferOverflow.DROP_OLDEST` 또는 `bufferCapacity` 파라미터 노출.

### spring-boot + utils + vt

#### H21 — 6개 WebFlux 추상 컨트롤러 CoroutineScope 수명주기 누수 (spring-boot3+4)

- **파일:**
    - `spring-boot3/4/core/.../webflux/controller/AbstractCoroutineVTController.kt:21-22`
    - `spring-boot3/4/core/.../webflux/controller/AbstractCoroutineIOController.kt:20-21`
    - `spring-boot3/4/core/.../webflux/controller/AbstractCoroutineDefaultController.kt:20-21`
- **이슈:** `SupervisorJob` 소유 컨트롤러 빈에 `DisposableBean`/`@PreDestroy` 없음 → 컨텍스트 종료 시 in-flight job 취소 안 됨 → 커넥션/스레드 누수.
- **수정:** `DisposableBean.destroy()`에서 `coroutineContext.cancel()` 구현. 또는 Spring managed scope의 `@Bean` 코루틴 스코프 위임.

#### H22 — Snowflake 시퀀서 전역 lock 보유 중 CPU busy-spin (utils/idgenerators)

- **파일:**
    - `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/sequencer/DefaultSequencer.kt:53-58`
    - `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/sequencer/GlobalSequencer.kt:90-95`
-

**이슈:** `lock.withLock` 보유 중 `while (currentTimestamp == lastTimestamp) { currentTimestamp = System.currentTimeMillis() }` → 밀리초 overflow 시 코어 1개 100% CPU + 모든 동시 호출자 lock 대기.
-
**수정:** `LockSupport.parkNanos(1_000)` 또는 `Thread.onSpinWait()` + `parkNanos`. overflow 처리를 lock 밖에서 수행하고 원자적으로 publish.

#### H23 — AbstractJwtProvider JWT signing 전체 ReentrantLock 직렬화 (utils/jwt)

- **파일:** `utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/AbstractJwtProvider.kt:42-46,66-73`
-

**이슈:** `composer(keyChain)` / `compose { }` 전체 DSL 실행 (HMAC/RSA signing 포함)이 `ReentrantLock` 내부. 공유 `JwtProvider` 빈에서 동시 로그인/refresh → contention.
- **수정:** keyChain 읽기만 lock. `AtomicReference<KeyChain>`으로 관리 후 lock 없이 compose 실행.

#### H24 — RetryWorkFlow.execute () 코루틴에서 Thread.sleep 직접 호출 가능 (utils/workflow)

- **파일:** `utils/workflow/src/main/kotlin/io/bluetape4k/workflow/core/RetryWorkFlow.kt:69`
-

**이슈:** `Dispatchers.Default`에서 `execute(ctx)` 호출 시 CPU 워커 스레드를 재시도 딜레이 시간만큼 블로킹. 컴파일/런타임 guard 없음. `SuspendRetryFlow`가 올바른 대안이나 API 구분 불명확.
- **수정:** `RetryWorkFlow` 클래스 KDoc에 경고 추가. `withContext(Dispatchers.IO)` 내부 실행 또는 코루틴 컨텍스트 검출 후 경고 로그.

---

## MEDIUM (24건 — 그룹별 주요 항목)

### core + testing (M1~M7)

- M1: `SuspendRingBuffer.kt:128-135` — `runBlocking` 내부 `iterator()` (Deprecated 있으나 Java 호출자 접근 가능), VT 핀닝
- M2: `Runtimex.kt:176-242` — stdout/stderr 동일 `ByteArrayOutputStream` 동시 쓰기 → prefix 인터리빙
- M3: `Base58.kt:30` — `SecureRandom.getInstanceStrong()` → Linux `/dev/random` 첫 호출 차단 가능
- M4: `interval.kt:97-107` — `delay=ZERO`이면 tight loop, 단일 dispatcher에서 다른 코루틴 기아 가능
- M5: `FlowParallel.kt:26` — 채널 버퍼 256 하드코딩, parallelism=1024 시 256K 슬롯 프리알로케이션
- M6: `InMemoryRepository.kt` (mock-web/webflux-server) — `store.clear()` 후 `forEach` 사이 readers → empty 관측
- M7: `PostgreSQLServer.kt:154-164` — `createStatement()` use-블록 없음 (JDBC spec 의존)

### io + texts (M8~M10)

- M8: `AhoCorasickAutomaton.kt:111-120` — `firstMatch`가 전체 매치 목록 materialization 후 minWith → 불필요한 전체 스캔
- M9: `AhoCorasickFlowExtensions.kt:35-43` — `matchesAsFlow` KDoc "스트리밍 메모리 절약" 주장이나 실제 eager materialization
- M10: `RetrofitCallSupport.kt:8-12` — `retryScheduler` lazy 공유는 올바르나 ShutdownQueue 미등록

### data modules (M11~M13)

- M11: `StatelessSesisonSupport.kt:38-44` — rollback 실패 무음 삼킴 (`addSuppressed` 누락, JDBC쪽은 올바름)
- M12: `JdbcRepository.kt:344,402` / `R2dbcRepository.kt:316,367` — `findAllByIds`/`deleteAllByIds` 무제한 IN 절 (PostgreSQL 32K, Oracle 1000 제한)
- M13: `ConnectionExtensions.kt:194-201` — `executeBatch` 루프 내부 검증, `addBatch` 후 blank 발견 시 Statement 불일치 상태

### aws + infra (M14~M20)

- M14: `SuspendKafkaProducerTemplate.kt:48-52` / `SuspendKafkaConsumerTemplate.kt:69-72` — 미사용 `CoroutineScope` 보유 (내부 launch 없음)
- M15: `ProducerSupport.kt:50-62` — `withProducer` per-call client 생성 시 broker 커넥션 누수 KDoc 경고 없음
- M16: `DynamoDbAsyncClientSupport.kt:31-33` — 매 호출 `ShutdownQueue.register` → per-request 생성 시 큐 무한 증가
- M17: `LettuceLock.kt:103-118` — `tryLock(waitTime=ZERO)` fast-path 없음 (타이밍 의존 정확하나 의도 불명)
- M18: `LettuceClients.kt:269-279` — `isOpen` 체크 후 `shutdown(client)` 예외 시 lock map cleanup 스킵
- M19: `CacheInvalidationStrategy.kt:75-79` — `keySet(pattern)` → Redis HKEYS O (N) 블로킹, `fastRemove(*vararg)` 대량 전파
- M20: (infra 중복 항목) resilience4j CacheCoroutines `synchronized` VT 핀닝 (Wave 3 M19와 연관)

### spring-boot + utils + vt (M21~M24)

- M21: `Hashids.kt:218` — `decodeInternal` 매 decode마다 full re-encode 검증 (비용 2배)
- M22: `MovingAverage.kt:277,284,293-295` — `weightedMovingAverage`가 단일 스레드 sequence 내 `ArrayBlockingQueue` 사용 (불필요한 lock)
- M23: `by lazy {}` SYNCHRONIZED 모드 VT 초기화 핀닝 — `RedisBinarySerializers.kt` (22개), `Snowflakers.kt:25,32`, `Uuid.kt` (7개), `TimebasedUuid.kt` (3개)
- M24: `ParallelWorkFlow.kt:108-111` — timeout 시 이미 완료된 fork 결과 폐기, 운영자 가시성 없음

---

## 수정 우선순위

| 순위 | 심각도   | 건수 |
|------|----------|------|
| 1    | CRITICAL | 5    |
| 2    | HIGH     | 25   |
| 3    | MEDIUM   | 24   |

---

## 주요 긍정 사항

- `FileSupport.kt:38-47` — lazy executor + VT factory + ShutdownHook 패턴 gold standard (Vertx fix 템플릿)
- `SuspendRetryFlow.kt:65,81` — `CancellationException` rethrow + `delay()` 사용 — 교과서적 structured concurrency
- `ExposedKeysetItemReader.kt:87,105` — `reentrantLock()` (VT-friendly) 사용, `synchronized` 없음
- `ULIDStatefulMonotonic.kt` — lock-free CAS ID 생성 — 고처리량 ID 생성 모범
- `LettuceClients.kt` — ReentrantLock (VT-safe) 사용, double-check 패턴 올바름
- `TransactionExtensions.kt:48-54` — `addSuppressed(rollbackEx)` 올바른 rollback 에러 처리 gold standard

---

*Wave 1 결과: docs/security-review/2026-04-28-wave1-security.md*
*Wave 2 결과: docs/security-review/2026-04-28-wave2-ops-sre.md*
*Wave 3 결과: docs/security-review/2026-04-28-wave3-kotlin-idiom.md*
*Wave 5 결과는 동일 디렉토리에 추가 예정*
