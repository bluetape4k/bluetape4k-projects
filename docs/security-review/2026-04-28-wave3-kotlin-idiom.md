# Wave 3 Kotlin Idiom 검토 — 2026-04-28

전체 5개 그룹 병렬 실행 결과. Tier 4 (Kotlin Idiom) 기준.

## 전체 요약

| 그룹                     | CRITICAL | HIGH   | MEDIUM |
|--------------------------|----------|--------|--------|
| core + testing           | 1        | 3      | 5      |
| io + texts               | 1        | 5      | 6      |
| data modules             | 0        | 4      | 7      |
| aws + infra              | 0        | 2      | 5      |
| spring-boot + utils + vt | 1        | 4      | 3      |
| **합계**                 | **3**    | **18** | **26** |

---

## CRITICAL

### C1 — Networkx.ipToIpBlock NPE (core)

- **파일:** `bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/Networkx.kt:208`
- **이슈:** `val pLen = if (arr.size != 4 && prefixLen == null) arr.size * 8 else prefixLen!!`
  — `arr.size==4 && prefixLen==null` 시 조건이 false → `else` 브랜치에서 `prefixLen!!` NPE 발생. KDoc ("prefixLen이 null이면 자동 결정")과 모순. `cidrToIpBlock("192.168.0.0")` 경로로 재현 가능.
- **수정:** `val pLen = prefixLen ?: (arr.size.coerceAtMost(4) * 8)`

### C2 — Vert.x Future.recover 깨진 구현 (io/vertx)

- **파일:** `io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureSupport.kt:15-19`
- **이슈:**
  ```kotlin
  fun <T> Future<T>.recover(exceptionHandler: (Throwable?) -> T): Future<T> {
      return this.andThen { exceptionHandler(it.cause()) }  // WRONG: andThen은 observe-only
  }
  ```
  `andThen()`은 결과를 관찰만 하고 반환값을 폐기함 → recover가 아무 효과 없음.
- **수정:** `return this.otherwise { cause -> exceptionHandler(cause) }`

### C3 — Kotlin assert () 프로덕션 검증 (~47 call sites) (spring-boot + utils)

- **파일:** `bluetape4k/core/src/main/kotlin/io/bluetape4k/support/AssertSupport.kt:125-132` (근원)
    + 하위 호출처 47+ 개소 (대표 목록):

    - `utils/math/ComparableHistogram.kt:106` — `assert(count() > 0)` 후 `minOrNull()!!`/`maxOrNull()!!`
    - `utils/math/special/Factorials.kt:54,71,131,165`
    - `utils/math/commons/MovingAverage.kt:20,119,274`
    - `utils/javatimes/range/TemporalClosedProgression.kt:59,61,70,72`
    - `utils/javatimes/range/TemporalClosedRange.kt:42,43,49`
    - `utils/javatimes/range/TemporalClosedRangeSupport.kt:26,27,82,328`
    - `utils/javatimes/range/DateGenericProgression.kt:29,31,60,62`
    - `utils/idgenerators/hashids/Hashids.kt:259`
    - `spring-boot3/core/.../beans/BeanUtilsSupport.kt:84,104,121,138,155,200`
    - `spring-boot4/core/.../beans/BeanUtilsSupport.kt` (동일 미러)
- **이슈:** Kotlin `assert()`는 JVM `-ea` 플래그 없으면 no-op. 프로덕션 기본값은 `-ea` 미설정 → 검증이 통째로 사라짐 → 뒤따르는 `!!`에서 NPE 발생 (불명확한 에러).
- **수정:** `assert(...)` / `assertNotBlank(...)` 계열을 `require(...)` / `requireNotBlank(...)` 으로 전수 교체.
  `assert*` 계열은 테스트/벤치마크 코드에서만 허용.

---

## HIGH

### core + testing

#### H1 — requireNotEmpty kotlin.contracts 누락 (core)

- **파일:** `bluetape4k/core/src/main/kotlin/io/bluetape4k/support/RequireSupport.kt:437,454-456,473-475,492-494`
- **이슈:** `CharSequence?` 오버로드에는 `contract { returnsNotNull() }` 있으나
  `Array<T>?`, `Collection<T>?`, `Map<K,V>?` 오버로드에는 없음 → `requireHasKey`/`requireHasValue`/`requireContains`에서 `this!!` 강제 사용.
- **수정:** `Map<K,V>?.requireNotEmpty` 등에 contract 추가, 반환 타입 non-null로 변경.

#### H2 — SingletonHolder.getInstance () `created!!` (core)

- **파일:** `bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/SingletonHolder.kt:48-51`
- **이슈:** `val created = _factory?.invoke()` → `return created!!` — `_factory`가 이미 null이면 NPE.
- **수정:** `val factory = _factory ?: error("SingletonHolder factory already invalidated")`

#### H3 — ReplaySubject Node.value!! (coroutines)

-

**파일:** `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/ReplaySubject.kt:436,550`
- **이슈:** `Node<T>(val value: T?)` 는 nullable인데 consumer에서 `next.value!!` 단언.
- **수정:** sentinel node 도입 또는 `requireNotNull(next.value)` 명시적 메시지 제공.

### io + texts

#### H4 — KoreanChunker.chunk () 내 runBlocking (Dispatchers.Default) 데드락 (texts)

- **파일:** `texts/tokenizer-korean/src/main/kotlin/io/bluetape4k/tokenizer/korean/tokenizer/KoreanChunker.kt:301`
- **이슈:** non-suspend `chunk()` 내부에서 `runBlocking(Dispatchers.Default)` 호출 → Dispatchers.Default 스레드에서 호출 시 데드락.
- **수정:** suspend 함수로 전환 또는 `Dispatchers.IO` 사용.

#### H5 — KoreanDictionaryProvider synchronized VT 핀닝 (texts)

-

**파일:** `texts/tokenizer-korean/src/main/kotlin/io/bluetape4k/tokenizer/korean/utils/KoreanDictionaryProvider.kt:122,142`
- **이슈:** `synchronized {}` 블록 — Virtual Thread carrier thread 핀닝.
- **수정:** `reentrantLock()` + `withLock { }` 교체.

#### H6 — FileSupport createTempDir TOCTOU race (io)

- **파일:** `io/io/src/main/kotlin/io/bluetape4k/io/FileSupport.kt:125-136`
- **이슈:**
  ```kotlin
  val dir = File.createTempFile(prefix, suffix)
  dir.deleteRecursively()   // 삭제
  dir.mkdirs()              // race: 다른 프로세스가 경로 선점 가능
  ```
- **수정:** `Files.createTempDirectory(prefix).toFile()` 사용.

#### H7 — KryoSupport async 완료 전 pool 반환 (io)

- **파일:** `io/io/src/main/kotlin/io/bluetape4k/io/serializer/KryoSupport.kt:107-115`
- **이슈:**
  ```kotlin
  val kryo = KryoProvider.obtainKryo()
  return CompletableFuture.supplyAsync { func(kryo) }
      .whenCompleteAsync { _, _ -> KryoProvider.releaseKryo(kryo) }  // 다른 executor에서 실행
  ```
  `supplyAsync`와 `whenCompleteAsync`가 다른 스레드 실행 → 조기 반환 후 재사용 충돌.
- **수정:** `supplyAsync { try { func(kryo) } finally { releaseKryo(kryo) } }`

#### H8 — OkHttp executeSuspending 취소 race Response 미닫힘 (io)

- **파일:** `io/http/src/main/kotlin/io/bluetape4k/http/okhttp3/OkHttpClientExtensionsCoroutines.kt:54-58`
- **이슈:** coroutine 취소 후 `onResponse` 도착 시 `response.close()` 없음 → 커넥션 풀 누수.
- **수정:** `if (!cont.isActive) { response.close(); return }` 추가.

> *io+texts 에이전트 전체 보고: CRITICAL 1건, HIGH 5건, MEDIUM 6건. 상기 H4~H8 이외 1건 추가 HIGH 및 6건 MEDIUM은 에이전트 전체 리포트 참조.*

### data modules

#### H9 — Hibernate JSON converter 무음 에러 swallow (data)

- **파일:** `data/hibernate/converters/AbstractObjectAsJsonConverter.kt:40-60`
-

**이슈:** `convertToDatabaseColumn`/`convertToEntityAttribute` 모두 `JsonProcessingException` 로그 후 `null` 반환 → 저장 실패가 `NULL` 저장, 읽기 실패가 `null` 엔티티로 silent corruption.
- **수정:** `PersistenceException` / `IllegalStateException` throw → 트랜잭션 롤백 보장.

#### H10 — AuditableR2dbcRepository 누락 (data)

- **파일:** `data/exposed-r2dbc/src/main/kotlin/io/bluetape4k/exposed/r2dbc/repository/` (파일 없음)
-

**이슈:** JDBC 대응 (`AuditableJdbcRepository.kt`)은 있으나 R2DBC `suspend fun auditedUpdateById()` 없음 → R2DBC UPDATE에서 `updatedAt`/`updatedBy` 자동 설정 누락, audit-incomplete rows 생성.
- **수정:** `AuditableR2dbcRepository.kt` 추가 (JDBC 버전 `suspend` 변환 미러).

#### H11 — R2dbcRepository.toEntity () suspend N+1 위험 (data)

- **파일:** `data/exposed-r2dbc/src/main/kotlin/io/bluetape4k/exposed/r2dbc/repository/R2dbcRepository.kt:106`
- **이슈:** `suspend fun ResultRow.toEntity(): E` → 구현에서 다른 suspend DB 쿼리 호출 가능 → Flow 수집 중 N+1, lock 보유 중 suspend.
- **수정:** non-suspend 유지 + N+1 금지 KDoc 명시, 또는 설계 의사결정 문서화.

#### H12 — Repository JDBC/R2DBC DRY 중복 5+ 계층 (data)

- **파일:** `JdbcRepository.kt` (648줄) vs `R2dbcRepository.kt` (585줄) + SoftDeleted + Auditable + Cache 쌍
- **이슈:** 코드 거의 동일, `suspend`/`Flow`만 다름 → AuditableR2dbcRepository 누락 (H10) 이 drift의 실증.
- **수정:** `BaseRepositoryContract<ID, E>` 인터페이스 공통 추출 또는 Kotlin source-set 공유.

### aws + infra

#### H13 — SuspendCacheImpl 빈 catch (Throwable) swallow (infra/resilience4j)

- **파일:** `infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/cache/impl/SuspendCacheImpl.kt:118-120`
-

**이슈:** `rawGetWithHit()`이 모든 `Throwable` 잡아 `null` 반환 → `CancellationException` swallow (structured concurrency 파괴), backing-cache 장애 은닉.
- **수정:**
  ```kotlin
  } catch (e: CancellationException) {
      throw e
  } catch (e: Throwable) {
      log.warn(e) { "rawGetWithHit failed: cache=$name, key=$cacheKey" }
      onError(e); null
  }
  ```

#### H14 — SuspendCacheImpl unconstrained K 에서 cacheKey!! (infra/resilience4j)

- **파일:** `infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/cache/impl/SuspendCacheImpl.kt:166,171`
- **이슈:** `class SuspendCacheImpl<K, V>` — `K` 미바운드 → `cacheKey!!` → nullable K 전달 시 NPE.
- **수정:** `K: Any` 바운드 추가 또는 `requireNotNull(cacheKey) { "..." }` 명시.

### spring-boot + utils + vt

#### H15 — Production !! 비trivial 경로 (utils/spring-boot)

- **파일:**
    - `utils/batch/src/main/kotlin/io/bluetape4k/batch/r2dbc/ExposedR2dbcBatchJobRepository.kt:120` — `.firstOrNull()!!` race-condition 경로
    - `utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/RuleProxy.kt:96,179,228` — `conditionMethod!!`, `methods.find{}!!`, `findRuleAnnotation()!!`
    - `spring-boot4/core/.../http/RestClientCoroutinesDsl.kt:26,53,80,107` — `body(T::class.java)!!`
- **수정:** `requireNotNull(...) { "설명 메시지" }` 전환.

#### H16 — spring-boot3 vs spring-boot4 소스 대규모 중복 (cassandra 확인)

- **파일:** `spring-boot3/cassandra/src/main/kotlin/` vs `spring-boot4/cassandra/src/main/kotlin/` (10+ 파일 diff 확인)
- **이슈:** 설정이 아닌 Kotlin 소스 수준 중복. `OptionsSupport.kt` 이미 drift 발생.
  `exposed-jdbc/r2dbc/hibernate-lettuce` 도 동일 패턴 의심.
- **수정:** 프레임워크 무관 로직을 `spring-cassandra-core` 공통 모듈로 추출, 또는 CI parity-diff 게이트 추가.

#### H17 — OptionsSupport.kt mass !! (boot3 + boot4)

- **파일:**
    - `spring-boot3/cassandra/.../cql/OptionsSupport.kt:117,144,147,171,174,195,212`
    - `spring-boot4/cassandra/.../cql/OptionsSupport.kt:106,132,135,158,161,182,199`
- **이슈:** `var` 지역 변수로 smart-cast 불가 → `writeOptions.ttl!!`, `writeOptions.timestamp!!` 반복.
- **수정:** `val ttl = writeOptions.ttl; if (ttl != null && !ttl.isNegative) { ... }` 패턴으로 교체.

#### H18 — RestClientCoroutinesDsl.kt boot3/boot4 parity 누락

- **파일:** `spring-boot4/core/.../http/RestClientCoroutinesDsl.kt` (boot3에 없음)
- **이슈:** `suspendGet/Post/Put/Patch/Delete` 확장이 boot4에만 존재 → boot3 사용자 불평등.
- **수정:** boot3에 백포트 또는 boot4-only 의도를 README/KDoc에 명시.

---

## MEDIUM (26건 — 그룹별 주요 항목)

### core + testing (M1~M5)

- M1: `throttle.kt:313-401` — `throttleTime` 88줄 (50줄 규칙 위반), 내부 로직 분리 필요
- M2: `groupBy.kt:246-251` — `groupByInternal` CancellationException 조건부 삼킴 (`ensureActive()` 누락)
- M3: 800줄 초과 파일 6개 (`ApacheStringUtils.kt` 1510, `StringSupport.kt` 1047, `ValueConverters.kt` 1045 등)
- M4: `FutureUtils.kt:13` — stale `// TODO: 이건 VirtualThreadUtils 로 이동`
- M5: `FlowSequential.kt:25`, `FlowParallel.kt:26` — 매직 넘버 64/256 (상수로 추출)

### io + texts (M6~M11)

- M6: `ResultCall.kt:87-90` — `Throwable` catch가 `CancellationException` 래핑 (retrofit2)
- M7~M11: 에이전트 전체 리포트 참조 (5건 추가)

### data modules (M12~M18)

- M12: `SuspendedQuery.kt:142-208` — 6단계 중첩 (4단계 규칙 위반), cursor predicate 추출 필요
- M13: `JdbcRepository.kt` / `R2dbcRepository.kt` — `batchInsert`/`batchUpsert` Iterable+Sequence 오버로드 body 중복
- M14: `r2dbc/core/Insert.kt:181-238,336-407` — `InsertValuesSpecImpl`/`InsertValuesKeySpecImpl` SQL 빌딩 DRY 위반
- M15: `jdbc/sql/ConnectionExtensions.kt:116` — public API 오타 `executeUpdateWithIndedexes` → deprecation cycle 필요
- M16: `hibernate/stateless/StatelessSesisonSupport.kt` — 파일명 오타 (Sesison → Session)
- M17: `exposed-postgresql/.../TstzRangeColumnType.kt:171,179` — `catch (e: Exception)` → `CancellationException` swallow 위험
- M18: `hibernate/SessionSupport.kt:40,55`, `EntityManagerFactorySupport.kt:32,39` — `Throwable` catch → `Exception` 좁히기 권장

### aws + infra (M19~M23)

- M19: `resilience4j/cache/CacheCoroutines.kt:17,26` — `synchronized(locksByCache)` 사용 (VT 핀닝, CLAUDE.md 금지)
- M20: `cache-core/.../nearcache/jcache/NearJCache.kt:103,125,126,132,166,253,264,317,321` — `runCatching {}` 9개소 로그 없음
- M21: `resilience4j/cache/CacheExtensions.kt:26,59,97` — unconstrained K에서 `key!!`
- M22: `resilience4j/SupplierSupport.kt:117,121-129` — 공개 API 파라미터 오타 `resultPredicatoe` (deprecation cycle 필요)
- M23: `kafka/.../SuspendKafkaProducerTemplate.kt:31,49` — class-property `AtomicBoolean`이 `java.util.concurrent.atomic` 사용 (project rule: `kotlinx.atomicfu.atomic`)

### spring-boot + utils + vt (M24~M26)

- M24: `spring-boot3/4/.../beans/BeanUtilsSupport.kt:44` — `KotlinDelegates.instantiateClass(...)!!`
- M25: `utils/batch/.../dsl/BatchStepBuilder.kt:139,141` — `requireNotNull` 후 `!!` (smart-cast local 활용으로 교체)
- M26: `utils/science/.../UtmZoneSupport.kt:71`, `utils/math/.../Ranking.kt:80` — `map[key]!!` → `map.getValue(key)`

---

## 수정 우선순위

| 순위 | 심각도   | 건수 |
|------|----------|------|
| 1    | CRITICAL | 3    |
| 2    | HIGH     | 18   |
| 3    | MEDIUM   | 26   |

---

## 주요 긍정 사항

- **coroutine 취소
  처리**: `SuspendSequentialFlow`, `SuspendParallelFlow`, `SuspendRetryFlow` 등 workflow 파일들이 `CancellationException` 우선 rethrow — 모범 사례.
- **atomicfu
  스코프**: `DefaultSequencer.kt`, `GlobalSequencer.kt`, `Flake.kt`, `ULIDStatefulMonotonic.kt` 모두 class-property 레벨, method-local 없음.
- **`@Synchronized` / `GlobalScope` 미사용**: 프로덕션 코드 전체에서 확인.
- **`MongoClientExtensions.inTransaction`**: `CancellationException` 우선 처리 + `[WHY]` 설명 주석 — 교과서적 coroutines 위생.
- **KDoc 한국어 적용**: 공개 API에 `@param`/`@return`/실행 가능 예제 일관 적용.

---

*Wave 1 결과: docs/security-review/2026-04-28-wave1-security.md*
*Wave 2 결과: docs/security-review/2026-04-28-wave2-ops-sre.md*
*Wave 4~5 결과는 동일 디렉토리에 추가 예정*
