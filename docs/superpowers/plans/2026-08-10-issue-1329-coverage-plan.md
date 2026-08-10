# Issue #1329 모듈별 Kover Instruction Coverage 개선 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development when executing this plan.

**Goal:** Actions run `31353596034`의 기준선 85.02% 미만으로 측정된 12개 모듈을 저장소의 동일한 method-level Kover 집계 기준으로 85.02% 이상으로 올리고, 전체 aggregate와 기존 CI 계약을 유지한다.

**Architecture:** Kover 측정 경계를 먼저 저장소 정책에 맞춘다. `src/benchmark` source set은 production library coverage에서 제외하고, production class/package exclusion이나 threshold 완화는 추가하지 않는다. 그 다음 각 모듈의 report에서 instruction이 전혀 실행되지 않은 public API 경로를 기존 테스트 fixture와 launcher로 검증한다. 테스트는 모듈별로 작성·검증하되 Testcontainers 기반 모듈은 순차 실행한다. 마지막에 각 모듈 XML을 생성해 `.github/scripts/aggregate-kover-coverage.py`로 하나의 수치를 산출한다.

**Tech Stack:** Kotlin 2.3, Java 25 toolchain, Gradle, Kover XML, JUnit 5, MockK, `io.bluetape4k.assertions`, kotlinx.coroutines test, H2, Cassandra/Testcontainers, Redis/Redisson, Ktor `testApplication`, Apache HttpComponents 5, Okio.

---

## 1. 작업 경계와 기준선 고정

### 1.1 이슈·브랜치·worktree 확인

- 작업 디렉터리는 `.worktrees/issue-1329-coverage`의 `test/issue-1329-coverage`로 고정한다.
- `develop` worktree와 `.worktrees/site-manual-1-12-1` detached worktree는 읽기만 하고 수정하지 않는다.
- 다음 명령으로 기준 커밋, 원격 추적 상태, 기존 spec commit을 확인한다.

```bash
git status --short --branch
git log --oneline -3
gh issue view 1329 --repo bluetape4k/bluetape4k-projects --json number,title,state,body,assignees,labels,milestone,url
```

### 1.2 기준 report와 집계 도구 보존

- 기준 artifact `/var/folders/rg/gt492brj3w3bjqtlsbp6xt340000gn/T/tmp.z88DIPA6zT`를 삭제하지 않는다.
- baseline 수치 12개와 source-set 경계 근거를 PR 증거에 재사용한다.
- 집계 도구의 method-union 동작은 변경하지 않는다. 중복 report를 합칠 때 이미 coverage가 있는 method의 missed instruction을 0으로 만드는 현재 동작은 이 이슈의 baseline 계약이다.

## 2. Kover 측정 경계 보정

### 2.1 benchmark source set을 정책 경계에서 제외

**Modify:** `build.gradle.kts`

- `java-test-fixtures`의 `testFixtures` 제외와 같은 `kover.currentProject.sources` 블록에서 `excludedSourceSets.add("benchmark")`를 추가한다.
- `benchmark` source set을 선언하지 않은 모듈에는 영향이 없음을 Gradle configuration으로 확인한다.
- `instrumentation.excludedClasses`의 기존 `**\$DefaultImpls`만 유지하고, generated class·production class·package·method exclusion은 추가하지 않는다.

### 2.2 설정 검증

```bash
./gradlew :bluetape4k-core:tasks --all --no-configuration-cache
./gradlew :bluetape4k-core:koverXmlReport :bluetape4k-r2dbc:koverXmlReport :bluetape4k-redisson:koverXmlReport :bluetape4k-idgenerators:koverXmlReport --no-configuration-cache --max-workers=1
git diff --check
```

- report XML의 `benchmark` package가 사라지고 production package가 남는지 확인한다.
- 이 설정이 실패하면 설정을 되돌리고 실패 원인을 기록한 뒤 테스트 추가를 계속하지 않는다.

## 3. `bluetape4k/core` 고수익 순수 API 경로

### 3.1 sequence·primitive·collection 경로

**Modify:**

- `bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/SequenceSupportTest.kt`
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/IterableSupportTest.kt`
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/CollectionSupportTest.kt`
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/eclipse/primitives/PrimitiveNumericArrayListExtensionsTest.kt`
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/eclipse/multi/TreeMultimapSupportTest.kt`
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/eclipse/parallel/ParallelIterateSupportTest.kt`

- `mapCatching`, `forEachCatching`, `mapIfSuccess`, `tryForEach`, `toShortArray`, `sliding`, `asByteArray`의 성공·실패·빈 입력을 각각 호출한다.
- `ShortArray`/`LongArray`/`IntArray`의 `as*ArrayList`와 factory lambda 경로, multimap grouping, `parAggregateInPlaceBy` 결과를 검증한다.
- HOF 예외는 `io.bluetape4k.assertions.assertFailsWith`가 아닌 API가 반환하는 결과 계약으로 검증하고, 실제 예외 자체를 검증하는 경우에만 Bluetape `assertFailsWith`를 사용한다.

### 3.2 temporal·concurrency·support 경로

**Modify:**

- `bluetape4k/core/src/test/kotlin/io/bluetape4k/javatimes/TemporalSupportTest.kt`
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/CompletionStageSupportTest.kt`
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/CompletableFutureSupportTest.kt`
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/VirtualThreadSupportTest.kt`
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/support/StringSupportTest.kt`
- `bluetape4k/core/src/test/kotlin/io/bluetape4k/support/ValueConvertersTest.kt`

- `asTemporal`, `startOfMillis`, `toEpochDay`, `dropLast`, `asKotlinUuidOrNull`의 대표 temporal·UUID·경계 입력을 검증한다.
- `CompletionStage`/`CompletableFuture`의 `map`, `mapResult`, `flatMap`, 성공·실패 결합 경로를 `runTest`로 검증하고 blocking wait를 추가하지 않는다.
- `virtualThread`/`platformThread` builder의 이름·daemon·uncaught handler·작업 실행을 검증한다. JDK 조건은 기존 `@EnabledForJreRange` 패턴을 유지한다.

### 3.3 core 검증

```bash
./gradlew :bluetape4k-core:cleanTest :bluetape4k-core:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-core:koverXmlReport --no-configuration-cache --max-workers=1
```

- report를 기준 artifact와 같은 method-level 집계 방식으로 계산해 `bluetape4k/core >= 85.02%`인지 확인한다.

## 4. Redisson cache 모듈

### 4.1 coroutine near-cache·memoizer·JCache 경로

**Modify:**

- `cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/RedissonSuspendNearCacheTest.kt`
- `cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheTest.kt`
- `cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizerTest.kt`
- `cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/memoizer/RedissonMemoizerTest.kt`
- `cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCacheTest.kt`
- `cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/RedissonCachesTest.kt`

- 기존 `RedisServers.redisson` fixture만 사용한다.
- `redissonSuspendNearCacheOf`, `redissonNearCacheOf`, `backCacheSize`, empty-key `removeAll`, `clear`, `stats`, close 경로를 실제 cache lifecycle로 검증한다.
- suspend/real Redis 작업은 `runSuspendIO`와 기존 모듈의 `@Execution(SAME_THREAD)`/launcher 계약을 유지한다.
- JCache companion factory와 `RedissonCaches.jcache`/`suspendJCache`를 새 raw client/container 없이 호출하고 close 상태를 검증한다.

### 4.2 cache 검증

```bash
./gradlew :bluetape4k-cache-redisson:cleanTest :bluetape4k-cache-redisson:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-cache-redisson:koverXmlReport --no-configuration-cache --max-workers=1
```

## 5. Cassandra·Hibernate·JDBC 데이터 모듈

### 5.1 Cassandra public extension overloads

**Modify:**

- `data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt`
- `data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupportTest.kt`
- `data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/StatementSupportTest.kt`
- `data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/data/GettableSupportTest.kt`
- `data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/data/SettableSupportTest.kt`
- `data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/querybuilder/TermSupportTest.kt`
- `data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/mapper/EntitySupportTest.kt`
- `data/cassandra/src/consumerRuntimeTest/kotlin/io/bluetape4k/cassandra/mapper/CassandraMapperConsumerRuntimeClasspathTest.kt`

- `CqlSessionIdentity`의 blank filtering·trim·sort·default keyspace와 deprecated `of` delegation을 검증한다.
- 기존 `CassandraServer.Launcher` session으로 `prepare`/`execute`의 string·statement·positional·named overload를 모두 실제 호출한다.
- `GettableByName`/`GettableByIndex`/`GettableById` 및 대응 `Settable`의 map/list/set/value 경로를 대표 값과 null로 검증한다.
- codec/codec registry literal과 `MapTerm.isIdempotent`를 `TermSupportTest`에서 검증하고, consumer runtime test는 generated mapper API의 entity getter/constructor path를 실제 compile/runtime assertion으로 사용한다.

### 5.2 Hibernate QueryDSL·stateless·generated model contract

**Modify:**

- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/querydsl/core/ExpressionsSupportTest.kt`
- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/querydsl/core/ExpressionUtilsSupportTest.kt`
- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/stateless/StatelessSessionSupportTest.kt`
- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/model/ModelClassesUnitTest.kt`
- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/model/JpaEntityModelTest.kt`
- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/listeners/HibernateEntityListenerTest.kt`

- `ExpressionsSupport`의 array/comparable/date-time/enum/number/time template factory와 `mapPathOf` public overload를 실제 QueryDSL expression으로 만든다.
- stateless `getAs`/lock/transaction error path를 기존 H2 fixture로 검증한다.
- KAPT가 생성한 `QIntJpaTreeEntity`, `QTreeNodePosition`, `QLongJpaEntity`, `QUuidJpaEntity`의 public path/constructor contract를 `ModelClassesUnitTest`에서 참조해 generated output을 임의 제외하지 않고 검증한다.
- `JpaLocalizedEntity.getLocalizedValueOrDefault`의 locale hit/miss와 listener callback path를 검증한다.

### 5.3 JDBC ResultSet·setter·DataSource 경로

**Modify:**

- `data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ResultSetExtensionsTest.kt`
- `data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensionsTest.kt`
- `data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/DataSourceSupportTest.kt`
- `data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensionsTest.kt`

- H2 `Actors` fixture로 `getColumnLabels`, `iterator`, `emptyResultToNull`, nullable primitive getters, `first`/`moveToPrevious`를 빈·단일·다중 row에서 검증한다.
- `ArgumentSetter2`, `ObjectArgumentSetter`, length-aware setter의 `PreparedStatement` binding을 실제 insert/update에 연결해 검증한다.
- DataSource/Connection의 `executeInsert`, `executeUpdate`, `executeBatch` overload는 기존 transaction fixture로 검증하며 새로운 DB dependency를 추가하지 않는다.

### 5.4 data 모듈 순차 검증

```bash
./gradlew :bluetape4k-cassandra:cleanTest :bluetape4k-cassandra:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-cassandra:koverXmlReport --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-hibernate:cleanTest :bluetape4k-hibernate:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-hibernate:koverXmlReport --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-jdbc:cleanTest :bluetape4k-jdbc:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-jdbc:koverXmlReport --no-configuration-cache --max-workers=1
```

- Cassandra/Testcontainers, Hibernate/H2, JDBC/H2는 동시에 실행하지 않는다.
- 각 report에서 목표 모듈이 `85.02%` 이상인지 확인한 뒤 다음 모듈로 이동한다.

## 6. R2DBC production API 경로

**Modify:**

- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/support/DatabaseClientSupportTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/support/ReadableSupportTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/support/ParameterSupportTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/DatabaseClientBuilderTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/DeleteTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/ExecuteTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/InsertTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/UpdateTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionInitTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupportTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderTest.kt`
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderSupportTest.kt`

- 기존 R2DBC database fixture에서 nullable bind/value/setter, `MappingR2dbcConverter.read`, `DatabaseClientSupport.bindNullable`, `Readable` mapping을 null/non-null 양쪽으로 호출한다.
- `InsertValuesKeySpec`, `InsertValuesSpec`, `SetterSpec`, `DeleteValueSpec`의 builder/identifier validation/`then`/`fetch` path를 기존 `InsertTest`·`DeleteTest`로 검증한다.
- `ConnectionFactoryUtils`의 acquire/current/release와 `R2dbcTransactionManager`는 기존 pool/transaction fixture에서 실제 connection lifecycle로 검증한다.
- benchmark source set은 테스트하지 않는다. production source에 no-op branch·coverage-only call·suppression을 추가하지 않는다.

```bash
./gradlew :bluetape4k-r2dbc:cleanTest :bluetape4k-r2dbc:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-r2dbc:koverXmlReport --no-configuration-cache --max-workers=1
```

## 7. HTTP·Okio·Vert.x·Ktor 모듈

### 7.1 Apache HTTP client

**Modify:**

- `io/http/src/test/kotlin/io/bluetape4k/http/hc5/async/AsyncHttpClientCoroutinesTest.kt`
- `io/http/src/test/kotlin/io/bluetape4k/http/hc5/async/MinimalHttpAsyncClientTest.kt`
- `io/http/src/test/kotlin/io/bluetape4k/http/hc5/entity/EntityBuilderTest.kt`
- `io/http/src/test/kotlin/io/bluetape4k/http/hc5/cache/CachingHttpAsyncClientBuilderTest.kt`
- `io/http/src/test/kotlin/io/bluetape4k/http/hc5/classic/MinimalAndVirtualThreadHttpClientTest.kt`
- `io/http/src/test/kotlin/io/bluetape4k/http/okhttp3/mock/MockWebServerExtensionsTest.kt`

- `CloseableHttpAsyncClient.executeSuspending`와 deprecated `execute` overload는 existing HttpComponents client fixture와 deterministic local MockWebServer response로 success·failure·callback/context path를 검증한다.
- `EntityBuilder.httpEntityOf`의 text/byte/stream/content-type overload, multipart builder, cache builder/connection factory의 public overload를 실제 request construction으로 호출한다.
- `runTest`는 virtual-time 단위에만 사용하고 실제 network는 `runSuspendIO` 또는 기존 HTTP helper를 사용한다.

### 7.2 Okio coroutine/adapter 경로

**Modify:**

- `io/okio/src/test/kotlin/io/bluetape4k/okio/coroutines/SuspendedPipeTest.kt`
- `io/okio/src/test/kotlin/io/bluetape4k/okio/coroutines/BufferedSuspendedSinkTest.kt`
- `io/okio/src/test/kotlin/io/bluetape4k/okio/coroutines/BlockingInteropTimeoutTest.kt`
- `io/okio/src/test/kotlin/io/bluetape4k/okio/coroutines/SuspendedFileChannelSourceTest.kt`
- `io/okio/src/test/kotlin/io/bluetape4k/okio/base64/ApacheBase64SinkTest.kt`
- `io/okio/src/test/kotlin/io/bluetape4k/okio/base64/ApacheBase64SourceTest.kt`
- `io/okio/src/test/kotlin/io/bluetape4k/okio/base64/OkioBase64SinkTest.kt`
- `io/okio/src/test/kotlin/io/bluetape4k/okio/base64/OkioBase64SourceTest.kt`
- `io/okio/src/test/kotlin/io/bluetape4k/okio/compress/CompressableSinkAndSourceTest.kt`

- `SuspendedPipe` source/sink close/flush/forward, `RealBufferedSuspendedSink` write/emit/write primitive, `ForwardBlockingSink/Source` close/flush, `ChannelCompletionHandler` success/failure를 실제 pipe/ByteArray/file fixture로 검증한다.
- timeout/close는 `BlockingInteropTimeoutTest`의 existing test dispatcher와 cancellation 계약을 유지하고, blocking IO를 `runTest`에 넣지 않는다.
- Base64/compressor adapter factory는 round-trip과 close propagation을 검증한다.

### 7.3 Vert.x SQL·resilience 경로

**Modify:**

- `io/vertx/src/test/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRetrySupportTest.kt`
- `io/vertx/src/test/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureCircuitBreakerSupportTest.kt`
- `io/vertx/src/test/kotlin/io/bluetape4k/vertx/resilience4j/VertxDecoratorsTest.kt`
- `io/vertx/src/test/kotlin/io/bluetape4k/vertx/sqlclient/PoolSupportTest.kt`
- `io/vertx/src/test/kotlin/io/bluetape4k/vertx/sqlclient/mybatis/H2SqlClientExtensionTest.kt`
- `io/vertx/src/test/kotlin/io/bluetape4k/vertx/sqlclient/templates/TupleMapperSupportTest.kt`
- `io/vertx/src/test/kotlin/io/bluetape4k/vertx/web/VertxRouteExtensionsTest.kt`

- `Future.recover`의 class/iterable/predicate/supplier overload에서 success, matching failure, non-matching failure, fallback failure를 검증한다.
- `TimeLimiter.executeVertxFuture`/`decorateVertxFuture` success·failure path와 `VertxDecorators` suspend policy composition을 existing Vert.x test fixture에서 호출한다.
- H2 Vert.x SQL client로 `SqlClient.suspendQuery` 네 overload와 MyBatis select/count/insert/delete overload를 실제 query로 호출한다.
- `rowMapperAs`, `Row` nullable/array accessors, tuple mapper를 existing schema fixture로 검증한다.
- Vert.x tests는 existing `VertxTestSupport`/H2 fixture를 사용하고 raw container를 추가하지 않는다.

### 7.4 Ktor route overload

**Modify:** `ktor/resilience4j/src/test/kotlin/io/bluetape4k/ktor/resilience4j/KtorResilienceSupportTest.kt`

- `resilientPost`는 POST body handler와 retry policy로 성공 응답을 검증한다.
- `resilientRoute(HttpMethod.PUT, ...)`는 route handler와 empty/non-empty policy를 각각 호출한다.
- 기존 `testApplication`, `StatusPages`, Bluetape assertion을 유지하고 실제 timeout/cancellation mapping 회귀를 보존한다.

### 7.5 ID generator 경로

**Modify:**

- `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/IdSupportTest.kt`
- `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ksuid/KsuidEdgeCasesTest.kt`
- `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ulid/KotlinUuidSupportTest.kt`
- `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/uuid/UuidTest.kt`
- `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/uuid/UuidGeneratorTest.kt`

- `LongIdGenerator` 문자열/positive size contract, `Hashids` hex round-trip, ULID byte conversion, UUID V4/V5/V6/V7 facade 문자열 경로를 deterministic input으로 검증한다.
- `MacAddressNodeIdentifier`는 실제 host interface 존재 여부에 의존하지 않는 byte/string constructor contract만 검증한다.
- benchmark source set 제외 후에도 production API coverage가 유지되는지 report로 확인한다.

### 7.6 통합 검증 명령

```bash
./gradlew :bluetape4k-http:cleanTest :bluetape4k-http:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-http:koverXmlReport --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-okio:cleanTest :bluetape4k-okio:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-okio:koverXmlReport --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-vertx:cleanTest :bluetape4k-vertx:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-vertx:koverXmlReport --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-ktor-resilience4j:cleanTest :bluetape4k-ktor-resilience4j:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-ktor-resilience4j:koverXmlReport --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-idgenerators:cleanTest :bluetape4k-idgenerators:test --no-build-cache --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-idgenerators:koverXmlReport --no-configuration-cache --max-workers=1
```

## 8. 전체 집계와 품질 게이트

### 8.1 대상 report 재집계

- 모든 CI 대상 module report를 `build/reports/kover/report.xml`에서 별도 staging directory로 모은다.
- staging directory를 기존 baseline artifact와 섞지 않고 새 결과만 집계한다.

```bash
python3 -B .github/scripts/aggregate-kover-coverage.py <new-coverage-root>
```

- 12개 대상 모듈 각각 `>= 85.02%`, aggregate `>= 85.02%`인지 표로 기록한다.
- 비대상 모듈은 baseline 대비 의도치 않은 하락이 없는지 같은 report 비교로 확인한다.

### 8.2 정적·테스트 검증

```bash
./gradlew test --no-configuration-cache --max-workers=1
./gradlew detekt --no-configuration-cache --max-workers=1
git diff --check
```

- 실패 시 해당 모듈만 먼저 재실행해 원인을 분리한다. Testcontainers 모듈 간 병렬 재실행은 하지 않는다.
- Kotlin checklist `KT-01` null safety, `KT-02` immutability, `KT-03` coroutine/cancellation, `KT-04` API consistency, `KT-05` test quality와 `KT-FIN` final checks를 review 기록에 남긴다.

## 9. 리뷰·증거·인수인계

### 9.1 diff 자체 검토

- `git diff --stat`, `git diff --check`, 변경된 Kotlin test의 assertion import와 test name을 확인한다.
- production code 변경이 생겼다면 해당 변경을 재현하는 failing test와 최소 수정 이유를 함께 기록한다. coverage-only production change는 제거한다.
- benchmark/generated exclusion이 root source-set boundary 외에 추가되지 않았는지 검색한다.

```bash
rg -n 'excludedClasses|excludedSourceSets|benchmark|suppression|@file:Suppress' build.gradle.kts */*/build.gradle.kts
rg -n 'assertThrows|org\.assertj|org\.junit\.jupiter\.api\.Assertions|kotlin\.test\.assert' '**/src/test/**/*.kt'
```

### 9.2 issue/PR evidence 작성

- 각 모듈의 before/after covered·missed instruction 수와 coverage를 표로 기록한다.
- benchmark 경계 보정 이유, 실제로 추가된 production path, 실행한 Gradle/aggregate 명령, CI job 결과를 한국어로 기록한다.
- PR body는 비어 있지 않게 작성하고 마지막에 `## DoD Status`를 둔다. PR 생성과 merge는 별도 승인 게이트로 유지한다.

### 9.3 완료 조건

- 코드·테스트 변경, module report, aggregate report, CI logs, Kotlin checklist가 모두 fresh evidence를 가진다.
- 미달 모듈·불안정한 외부 service·실행하지 못한 검증은 숨기지 않고 `PENDING`으로 남긴다.
- 완료 전 `verification-before-completion` 체크리스트로 “12 modules pass / aggregate pass / CI pass / no known errors”를 재확인한다.

