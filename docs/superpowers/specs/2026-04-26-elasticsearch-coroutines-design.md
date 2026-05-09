# Elasticsearch Coroutines Module Design Spec

- **Issue**: #146 — `infra/elasticsearch` Elasticsearch Kotlin Coroutines 클라이언트 모듈 신규 추가
- **Worktree**: `.worktrees/issue-146-elasticsearch`
- **Branch**: `feat/issue-146-elasticsearch`
- **작성일**: 2026-04-26
- **작성자**: planner (Opus 4.7)
- **상태**: Draft (Step 1-S, 리뷰 대기)

---

## 1. 문제 재진술 + 제약 + 미지수

### 1.1 문제 재진술

bluetape4k 생태계에 `infra/elasticsearch` 모듈 (artifact: `bluetape4k-elasticsearch`)을 신규 추가하여 **Elasticsearch Java Client 9.x를 Kotlin Coroutines 친화 방식으로 사용**할 수 있도록 한다. 기존에 deprecated 된 `org.elasticsearch.client:elasticsearch-rest-client`(Libs.kt 1036~1037 라인) 를 직접 노출하는 형태가 아니라, **공식 신규 클라이언트 `co.elastic.clients:elasticsearch-java`** 를 base 로 한다.

핵심 산출물:
- `ElasticsearchAsyncClient` 를 감싸는 suspend 확장함수 (`CompletableFuture<T>` → `await()`)
- `BulkIngester` 또는 raw bulk API 를 `Flow` 기반으로 노출 (`bulkAsFlow`, `searchAsFlow`)
- `ElasticsearchClients` factory object — `ElasticsearchServer` testcontainer 와 완전 호환 (SSL + Basic Auth)
- 통합 테스트 (`AbstractElasticsearchTest` 베이스 + 시나리오별 테스트 클래스)

### 1.2 제약 (Research findings 반영)

#### 외부 라이브러리 / 버전
- **`co.elastic.clients:elasticsearch-java`** 는 `Libs.kt` 에 **미등록 상태** → 신규 entry 필요.
- 현재 `Versions.elasticsearch = "9.2.4"` (legacy `rest-client` 용). Research 에서 언급된 `elasticsearch-java:9.3.0` 와 차이가 있으며 testcontainer TAG 는 `9.3.3`.
  - **결정 필요**: `Versions.elasticsearch` 를 `9.3.3` 으로 올려서 통일 (서버 TAG 와 동일) vs. `Versions.elasticsearch_java` 를 `9.3.0` 별도 도입.
  - **권고**: `Versions.elasticsearch = "9.3.3"` 로 통일. legacy `rest-client` 도 같은 메이저/마이너 버전 라인이면 호환 (9.x).
- `co.elastic.clients:elasticsearch-java:9.3.x` 의 `JsonpMapper` **기본값: `JsonpMapper.lookup()` (자동 감지)** — 클래스패스에 Jackson 3 가 있으면 `Jackson3JsonpMapper`, Jackson 2 가 있으면 `JacksonJsonpMapper` 가 자동 선택된다. bluetape4k 는 `bluetape4k-jackson3` / `bluetape4k-jackson` 양쪽 모듈을 보유하므로, 사용자가 원하는 쪽만 runtime 의존성에 추가하면 된다.[^elasticsearch-java-9.3]
- Spring Boot 3.5 BOM 은 ES 8.x 까지 관리 — 9.x 는 **명시적 버전 고정 필수**.

[^elasticsearch-java-9.3]: 공식 문서 — Elasticsearch Java Client 9.3 — Connecting / JsonpMapper: <https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/connecting.html> · BulkIngester API: <https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/usage.html#_bulk_indexing>

#### Logging 클래스 구분

| 클래스 | Logging type | 이유 |
|--------|-------------|------|
| `ElasticsearchClients` | `KLogging()` | factory, 일반 fun (suspend 미사용) |
| suspend 확장 파일 (`*ApiCoroutines.kt`) | `KLoggingChannel()` | suspend 함수 내부에서 호출 |
| Test 클래스 (`AbstractElasticsearchTest` 외) | `KLoggingChannel()` | `runTest { ... }` 안에서 호출 |

#### 코드 컨벤션
- Kotlin 2.3, Java 21 toolchain. `-Xcontext-parameters` 활성화.
- **Virtual Thread 안전**: `synchronized` / `@Synchronized` 금지 → `ReentrantLock` 사용. (Memory: `feedback_virtual_threads_no_synchronized`)
- `atomicfu` 는 클래스 프로퍼티 한정. 메서드 로컬 금지.
- 모든 `Future<T>` 대기는 `bluetape4k.coroutines.support.awaitSuspending()` 재사용.
- Logging: 일반 class 는 `companion object: KLogging()` / `companion object: KLoggingChannel()` 패턴, `object` 는 `object Foo : KLogging()` 처럼 직접 상속 (Kotlin `object` 안에 `companion object` 불가 — compile error).
- Test: JUnit 5 + MockK + bluetape4k-assertions. **ktlint 금지** (IntelliJ formatter + .editorconfig).
- bluetape4k-assertions 비교 matcher 필수 (`shouldBeGreaterOrEqualTo` 등). `(x >= y).shouldBeTrue()` 금지.

#### 모듈 등록
- `settings.gradle.kts` 자동 등록: 디렉터리명 `infra/elasticsearch` → artifact `bluetape4k-elasticsearch`.
- `data/elasticsearch` 가 아닌 `infra/elasticsearch` 로 결정 (Issue 제목 + Memory: infra grouping 에 redis/lettuce/kafka 와 동일 카테고리). `data/` 는 RDB/NoSQL 모델 + repository 패턴 모듈군.

#### 테스트 인프라
- `ElasticsearchServer` (testcontainers/storage) 재사용. SSL on, password = `LibraryName`.
- `createSslContextFromCa()` 는 testcontainer 자체 메서드 (확인됨). 동일 헬퍼로 클라이언트 SSL 구성.
- 컨테이너 reuse + ShutdownQueue 패턴 (`Launcher.elasticsearch` lazy singleton).

### 1.3 미지수 (열린 질문)

- **Q1.** `Versions.elasticsearch` 를 `9.3.3` 으로 바꾸면 기존 `elasticsearch_rest_client` 사용처에 영향이 있는가? → **`testing/testcontainers/build.gradle.kts:87-88` 에서 `compileOnly(Libs.elasticsearch_rest_client)` + `compileOnly(Libs.elasticsearch_rest_client_sniffer)` 를 사용 중**. 9.2.4 → 9.3.3 마이너 업그레이드는 동일 메이저(9.x)이므로 ABI 호환 가능성이 높으나, **testcontainers 모듈 컴파일 검증이 필수**(Task T01 에 명시). 다른 모듈에서 직접 사용처는 grep 결과 없음.
- **Q2.** `BulkIngester<Context>` 헬퍼를 1차 릴리즈에 포함할지, suspend 확장함수만 우선 제공하고 후속 PR 로 분리할지?
  - **결정**: **1차 포함**. `bulkAsFlow` (chunking + Flow back-pressure)와 `BulkIngesterCoroutines` (자동 flush + listener→Channel/Flow) 두 트랙을 모두 1차 릴리즈에 포함한다. 사용자 시나리오(Flow stream → chunk 단위 결과 vs. 자동 flush + 지속 수집)가 명확히 분리되므로 이중 버퍼링 위험은 트랙 분리로 회피한다.
- **Q3.** Spring Data Elasticsearch 통합 클래스 (`ElasticsearchSpringConfig` 등)는 본 모듈에 포함할지?
  - **결정**: Research 결정 사항 그대로 — **거부**. 별도 `spring-boot3/elasticsearch-data` 모듈로 분리 (Auditable/Repository 패턴까지 포함시 별도 설계 필요).
- **Q4.** `JsonpMapper` 의 기본값을 무엇으로 할지?
  - **결정**: **`JsonpMapper.lookup()` (자동 감지)** — 클래스패스에 Jackson 3 있으면 `Jackson3JsonpMapper`, Jackson 2 있으면 `JacksonJsonpMapper` 자동 선택. 사용자가 명시적으로 선택하고 싶을 때를 위한 헬퍼 함수 `jacksonJsonpMapper()` (Jackson 2) / `jackson3JsonpMapper()` (Jackson 3) 도 함께 제공.

---

## 2. 설계 위험 / 실패 모드 (3개+)

### 위험 ↔ Task 매핑 요약

| 위험 | Severity | 관련 Task |
|------|----------|----------|
| R1 클라이언트 라이프사이클 누수 | Critical | T03, T24 |
| R2 SSL/CA 컨텍스트 mismatch | High | T04, T22 (testFixtures + 테스트 헬퍼) |
| R3 BulkIngester / Flow back-pressure | High | T10 |
| R4 Async cancel semantics | Medium | T09 |
| R5 Jackson3 dependency 충돌 | Medium | T02 |
| R6 Virtual Thread 호환성 | Medium | T04 |

### 위험 R1: 클라이언트 라이프사이클 누수 (Critical)
`ElasticsearchClient` / `ElasticsearchAsyncClient` 는 내부적으로 Apache HttpClient transport 를 보유. `close()` 미호출 시 connection pool 누수 + 데몬 스레드 leak. Spring Boot 환경에서 ApplicationContext 종료시 dispose 보장이 필요하며, raw 사용시 사용자가 try-finally 또는 `use {}` 패턴을 인지해야 한다.
- **대응**: `ElasticsearchClient` / `ElasticsearchAsyncClient` 가 `Closeable` 구현 → `use { }` 확장함수 직접 사용 가능. Factory 에서 생성한 인스턴스는 `ShutdownQueue.register()` 등록 옵션 제공. 테스트 베이스에서 `@AfterAll` 정리 헬퍼 제공.

### 위험 R2: SSL/CA 컨텍스트 mismatch (High)
testcontainer 가 자체 self-signed CA 로 부팅하며, 클라이언트는 동일 CA 를 신뢰해야 함. `co.elastic.clients` 의 `RestClientTransport` 는 `HttpHost` + `SSLContext` 주입 방식. ES 9.x는 디폴트 SSL 강제 → `http://` URL 사용 시 부팅은 되더라도 인증 실패.
- **대응**: `ElasticsearchClients` factory 에 `forTestcontainer(server: ElasticsearchServer)` 헬퍼 1급 메서드로 노출. 내부적으로 `server.createSslContextFromCa()` + `usernameAndPassword("elastic", server.password)` 자동 주입.

### 위험 R3: BulkIngester / Flow back-pressure 부정합 (High)
`BulkIngester` 는 자체 내부 큐와 flush interval 보유. Kotlin `Flow` 의 back-pressure (suspend collector) 와 결합시 **이중 버퍼링** + ordering 보장 어려움. `bulkAsFlow` 가 단순히 collect 하면서 add(operation) 만 호출하면, Flow 가 끝나도 BulkIngester 의 flush 가 끝나기 전까지 결과를 받지 못함.
- **대응**: `bulkAsFlow` 는 **BulkIngester 미사용** — 직접 chunk(batchSize) → `BulkRequest` 빌드 → `bulk()` 호출 패턴 채용 (Kafka `sendAsFlow` 와 동형).
- **BulkIngester wrapping (1차 포함)**: 별도 트랙(`BulkIngesterCoroutines.kt`)으로 제공. `bulkIngesterOf(client, maxOperations, flushInterval)` factory + `addSuspend(op, context)` suspend 확장 + listener 콜백(`beforeBulk`/`afterBulk`)을 `Channel`/`Flow` 로 변환해 진행 결과를 coroutines 안에서 관찰 가능하도록 한다. `Flow` 용 chunking 은 `bulkAsFlow`, "fire-and-forget + 자동 flush" 시나리오는 `BulkIngesterCoroutines` 가 책임진다 — 두 트랙이 명확히 분리되어 이중 버퍼링 위험을 회피.

### 위험 R4: Async client 의 `CompletableFuture` 취소 의미 (Medium)
`CompletableFuture` 의 `cancel()` 은 ES 서버측 작업을 실제로 중단하지 못함 — HTTP 요청은 이미 전송된 상태. coroutine cancel 시 클라이언트가 잘못된 결과를 받았다고 가정하면 안 됨.
- **대응**: KDoc 에 "취소는 클라이언트 측 대기만 종료한다" 명시. `awaitSuspending()` 의 `CancellationException` 의미 그대로 전달.

### 위험 R5: Jackson dependency 충돌 (Medium)
사용자 프로젝트가 Jackson 2 만 쓰거나 Jackson 3 만 쓰는 경우, `bluetape4k-elasticsearch` 가 양쪽을 transitive 로 강제하면 의존성 폭증 + 버전 충돌. 반대로 둘 다 안 가져오면 `JsonpMapper.lookup()` 이 디폴트 mapper 를 못 찾아 ClassNotFound.
- **대응 (옵션 C 일관)**: 기본값은 `JsonpMapper.lookup()` (자동 감지). `bluetape4k-jackson` (Jackson 2) 와 `bluetape4k-jackson3` (Jackson 3) 모두 `compileOnly` 로 선언 → 사용자가 원하는 쪽만 runtime 의존성으로 추가하도록 위임. 헬퍼 함수 `jacksonJsonpMapper()` / `jackson3JsonpMapper()` 는 각각 해당 Jackson 라이브러리가 클래스패스에 있을 때만 동작. 클래스패스에 mapper 가 없을 때 `JsonpMapper.lookup()` 이 던지는 ClassNotFound 는 **명시적 에러 메시지로 wrap** — "Add `bluetape4k-jackson3` (또는 `bluetape4k-jackson`) to runtime classpath, or pass an explicit mapper" 안내 포함.

### 위험 R6: Virtual Thread 호환성 (Medium)
`co.elastic.clients` 의 transport 는 Apache HttpAsyncClient (Netty 아님) → Virtual Thread 친화. 그러나 사용자가 sync 클라이언트를 VT 에서 호출시 monitor lock 진입 가능성. (Apache HC5 는 NIO 기반이라 일반적으로 안전하나 검증 필요)
- **대응**: 본 모듈은 **`ElasticsearchAsyncClient` 만 1급 노출**. sync 클라이언트는 advanced 사용처에서만 raw 로 사용 권장. KDoc 명시.

---

## 3. 접근법 비교

### 접근법 A: 순수 suspend 확장함수 방식 (최소 API 표면)

**구성**:
- `ElasticsearchAsyncClient` 에 suspend 확장함수만 추가 (예: `suspend fun ElasticsearchAsyncClient.indexAsync(req): IndexResponse`)
- factory / DSL 없음. 사용자가 raw 빌더 그대로 사용.
- `bulkAsFlow` / `searchAsFlow` 만 신규.

**장점**:
- API 표면 최소. 학습 곡선 낮음.
- Java client 의 모든 기능을 그대로 활용 가능 (래핑 누락 zero).
- 유지보수 비용 최저. ES Java client 가 진화해도 영향 적음.

**단점**:
- 사용자가 클라이언트 생성/SSL/CA/패스워드를 매번 직접 코드 작성. testcontainer 통합이 boilerplate.
- bluetape4k 컨벤션 (`xxxClientOf` factory, `withXxxClient` 확장) 와 일관성 깨짐.
- `LettuceClients`, `kafka producerOf()` 패턴과 결이 다름.

**판정**: **거부**. testcontainer 통합 boilerplate 가 모든 테스트에 중복되고, bluetape4k 의 factory 컨벤션 (Lettuce/Kafka/Cassandra 모두 factory object 보유)과 일관성 부족.

### 접근법 B: Operations 인터페이스 + 구현체 방식

**구성**:
- `ElasticsearchIndexOperations`, `ElasticsearchDocumentOperations`, `ElasticsearchSearchOperations` 인터페이스 정의.
- 각 인터페이스 구현체가 `ElasticsearchAsyncClient` delegate.
- 사용자는 `client.documents.index(...)`, `client.search.search(...)` 형태로 사용.

**장점**:
- 도메인 분리 명확. 큰 API 표면을 카테고리화.
- Spring Data ES `ElasticsearchOperations` 와 유사한 API → 학습 친화.
- Mock 작성 쉬움 (인터페이스 기반).

**단점**:
- **추상화 누수 심각**: ES Java client 는 lambda functional builder 사용 → 인터페이스로 1:1 매핑하면 메서드 폭발 (단순 `IndexRequest` 도 변형 다수). Builder 함수 시그니처를 그대로 노출하면 인터페이스가 거대화.
- ES Java client 의 새 기능 추가 시 매번 인터페이스 업데이트 필요 → **유지보수 비용 폭증**.
- bluetape4k 컨벤션 (`SuspendNearCacheOperations` 등)과 형태는 비슷하나, 그 컨벤션은 **storage abstraction** 영역 — ES 클라이언트는 이미 그 자체로 abstraction. 이중 추상화는 YAGNI.

**판정**: **거부**. ES Java Client 의 builder DSL 을 인터페이스화하면 추상화 누수 + 유지보수 비용 폭증. Spring Data ES 가 이미 그 영역을 커버.

### 접근법 C: Hybrid (확장함수 + 얇은 DSL config) — **채택**

**구성**:
- **suspend 확장함수** (주 API): `ElasticsearchAsyncClient` 의 모든 async 메서드에 동일 시그니처 suspend 변형 + 일부 자주 쓰이는 lambda 짧은형 helper.
- **`ElasticsearchClients` factory object** (Lettuce/Cassandra 패턴): `clientOf(host, port)`, `clientOf(builder)`, `forTestcontainer(server)` 등 생성 팩토리.
- **`bulkAsFlow` / `searchAsFlow`** Flow 어댑터 (Kafka `sendAsFlow` 패턴).
- **DSL builder 최소화**: `elasticsearchClient { host = "..."; port = 9200; ... }` 1개만 추가. 그 외 ES Java client 빌더는 그대로 노출.

**장점**:
- 핵심 boilerplate (testcontainer SSL, factory) 제거하면서 ES Java client 의 표현력 100% 유지.
- bluetape4k 컨벤션 (`xxxClientOf`, factory object, suspend 확장) 일관성 확보.
- API 표면 작음 → 유지보수 부담 적음.
- ES Java client 가 진화해도 factory + suspend 확장만 업데이트.
- `bulkAsFlow` 가 Kafka `sendAsFlow` 와 동형 → 사용자 학습 비용 ↓.

**단점**:
- factory object 가 testcontainer 의존성을 갖게 되면 production 배포에 testcontainer 가 따라옴 → 분리 필요. (해결: `forTestcontainer` 는 별도 testFixtures 또는 testing 모듈에 추가).

**판정**: **채택**. bluetape4k 의 `LettuceClients`, `kafka producerOf()`, `cassandraClientOf()` 와 같은 결의 디자인. ES Java client 의 풍부한 builder DSL 을 그대로 활용하면서 Coroutines / Flow / testcontainer integration boilerplate 만 제거.

---

## 4. 설계 섹션

### 섹션 1: 모듈 개요 및 목표

**모듈명**: `bluetape4k-elasticsearch` (디렉터리: `infra/elasticsearch`)

**목표**:
1. `co.elastic.clients:elasticsearch-java` 9.x 를 Kotlin Coroutines 환경에서 idiomatic 하게 사용.
2. `ElasticsearchAsyncClient` 의 모든 async 메서드를 suspend 변형으로 노출.
3. `Flow<T>` 기반 bulk indexing / search scrolling API 제공.
4. `bluetape4k-testcontainers` 의 `ElasticsearchServer` 와 zero-boilerplate 통합.
5. KDoc + 예제는 한국어 가능. README.md / README.ko.md 양쪽 작성.

**비목표 (Out of scope)**:
- Spring Data Elasticsearch 통합 (별도 `spring-boot3/elasticsearch-data` 모듈 후속 PR).
- ES 7.x / 8.x 호환 (9.x 전용).
- Reactive (Reactor) wrapping. coroutines 만 1급.

**1차 릴리즈 포함 (변경됨)**:
- `BulkIngester<Context>` 의 Coroutines 친화 래핑 — `bulkIngesterOf()` factory + `addSuspend(op, context)` 확장 + listener 콜백을 `Channel`/`Flow` 로 변환하는 패턴. `bulkAsFlow` 와 별개 트랙으로 1차 포함.

### 섹션 2: 기술 스택 및 의존성

| 항목 | 버전 | 용도 |
|------|------|------|
| Kotlin | 2.3 | toolchain |
| JVM | 21 | toolchain |
| coroutines | (Versions.kotlinx_coroutines) | suspend / Flow |
| `co.elastic.clients:elasticsearch-java` | **9.3.3** (`Versions.elasticsearch` 통일) | 메인 클라이언트 |
| `org.apache.httpcomponents.client5:httpclient5` | (transitive) | transport |
| `bluetape4k-coroutines` | project | `awaitSuspending`, Flow util |
| `bluetape4k-logging` | project | `KLoggingChannel` |
| `bluetape4k-jackson` | project, `compileOnly` | `JacksonJsonpMapper` (Jackson 2 옵션) |
| `bluetape4k-jackson3` | project, `compileOnly` | `Jackson3JsonpMapper` (Jackson 3 옵션) |
| `bluetape4k-testcontainers` | project, `testImplementation` | `ElasticsearchServer` |
| `bluetape4k-junit5` | project, `testImplementation` | Fakers, JUnit5 helpers |
| MockK / bluetape4k-assertions | testImplementation | unit test |

**Libs.kt 변경 사항**:
1. `Versions.elasticsearch = "9.2.4"` → `"9.3.3"` (서버 TAG 와 통일).
2. 신규 entry:
```kotlin
// ElasticSearch Java Client (co.elastic.clients)
val elasticsearch_java = "co.elastic.clients:elasticsearch-java:${Versions.elasticsearch}"
```
3. legacy `elasticsearch_rest_client` 는 deprecated 주석 추가 (호환 유지).

### 섹션 3: 패키지 구조 및 파일 목록

**Base package**: `io.bluetape4k.elasticsearch`

```
infra/elasticsearch/
├── build.gradle.kts
├── README.md
├── README.ko.md
├── src/
│   ├── main/
│   │   ├── kotlin/io/bluetape4k/elasticsearch/
│   │   │   ├── ElasticsearchClients.kt              # factory object
│   │   │   ├── ElasticsearchClientDsl.kt            # DSL builder
│   │   │   ├── ElasticsearchDefaults.kt             # const default values
│   │   │   ├── coroutines/
│   │   │   │   ├── DocumentApiCoroutines.kt        # index/get/update/delete suspend
│   │   │   │   ├── SearchApiCoroutines.kt          # search/scroll/count suspend
│   │   │   │   ├── IndicesApiCoroutines.kt         # create/delete/exists suspend
│   │   │   │   ├── BulkApiCoroutines.kt            # bulkAsFlow + suspendBulk
│   │   │   │   ├── BulkIngesterCoroutines.kt       # BulkIngester<Context> 래핑 (factory + addSuspend + listener→Flow/Channel)
│   │   │   │   └── ClusterApiCoroutines.kt         # health/info suspend
│   │   │   └── support/
│   │   │       ├── ElasticsearchSupport.kt         # 공통 helper (host, mapper)
│   │   │       └── JsonpMappers.kt                 # jacksonJsonpMapper / jackson3JsonpMapper
│   │   └── resources/
│   │       └── META-INF/MANIFEST.MF (생성)
│   └── test/
│       ├── kotlin/io/bluetape4k/elasticsearch/
│       │   ├── AbstractElasticsearchTest.kt
│       │   ├── ElasticsearchClientsTest.kt
│       │   ├── coroutines/
│       │   │   ├── DocumentApiCoroutinesTest.kt
│       │   │   ├── SearchApiCoroutinesTest.kt
│       │   │   ├── IndicesApiCoroutinesTest.kt
│       │   │   ├── BulkApiCoroutinesTest.kt
│       │   │   ├── BulkIngesterCoroutinesTest.kt
│       │   │   └── ClusterApiCoroutinesTest.kt
│       │   └── examples/
│       │       └── ProductIndexExample.kt          # 실전 시나리오
│       └── resources/
│           ├── junit-platform.properties           # MANDATORY
│           └── logback-test.xml                    # MANDATORY
```

**파일 라인 수 가이드**: 각 파일 200–400 라인 (coding-style.md 준수). 800 라인 초과 금지.

**`ElasticsearchDefaults.kt` (신규)** — magic literal 제거 + 단일 정의 위치:
```kotlin
package io.bluetape4k.elasticsearch

object ElasticsearchDefaults {
    /** bulk 한 회 호출당 최대 operation 개수. ES 권장 5–15MB 페이로드 / 평균 도큐먼트 크기 기준 절충값. */
    const val DEFAULT_BULK_CHUNK_SIZE = 500

    /** searchAsFlow 한 회 호출당 size. ES 일반 검색 page size 권장값. */
    const val DEFAULT_SEARCH_BATCH_SIZE = 100

    /** BulkIngester 한 회 batch 의 최대 operation 개수. */
    const val DEFAULT_BULK_INGESTER_MAX_OPERATIONS = 1_000

    /** BulkIngester 자동 flush 주기 (default 5초). */
    val DEFAULT_BULK_INGESTER_FLUSH_INTERVAL: java.time.Duration = java.time.Duration.ofSeconds(5)
}
```
근거: ES 공식 가이드 `Tune for indexing speed` — bulk 페이로드는 5–15MB 권장 ([ref](https://www.elastic.co/guide/en/elasticsearch/reference/current/tune-for-indexing-speed.html#_use_bulk_requests)). search size 100 은 페이지네이션 일반 default.

### 섹션 4: 핵심 API 설계

#### 4.1 `ElasticsearchClients` factory

```kotlin
package io.bluetape4k.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.JsonpMapper
import co.elastic.clients.transport.ElasticsearchTransport
import co.elastic.clients.transport.rest_client.RestClientTransport
import io.bluetape4k.logging.KLogging
import org.apache.hc.core5.http.HttpHost                                       // HC5 (HC4 아님)
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.elasticsearch.client.RestClient
import javax.net.ssl.SSLContext

/**
 * `co.elastic.clients` 기반 Elasticsearch 클라이언트 팩토리입니다.
 * (Lettuce/Cassandra 패턴 — singleton object, factory methods)
 *
 * Logging: factory 는 일반 fun 만 가지므로 `KLogging()` 사용 (suspend 호출 없음).
 */
object ElasticsearchClients : KLogging() {

    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_PORT = 9200
    const val DEFAULT_SCHEME = "https"
    const val DEFAULT_USERNAME = "elastic"

    /**
     * 기본 연결 정보로 [ElasticsearchAsyncClient] 생성 (production HTTPS 가정).
     *
     * `mapper` 기본값은 [JsonpMapper.lookup] — 클래스패스에 Jackson 3 가 있으면
     * `Jackson3JsonpMapper`, Jackson 2 가 있으면 `JacksonJsonpMapper` 가 자동 선택된다.
     */
    @JvmStatic
    fun asyncClientOf(
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        scheme: String = DEFAULT_SCHEME,
        username: String? = null,
        password: String? = null,
        sslContext: SSLContext? = null,
        mapper: JsonpMapper = JsonpMapper.lookup(),
    ): ElasticsearchAsyncClient {
        val transport = transportOf(host, port, scheme, username, password, sslContext, mapper)
        return ElasticsearchAsyncClient(transport)
    }

    /** [RestClient] 직접 주입 (advanced). */
    @JvmStatic
    fun asyncClientOf(
        restClient: RestClient,
        mapper: JsonpMapper = JsonpMapper.lookup(),
    ): ElasticsearchAsyncClient =
        ElasticsearchAsyncClient(RestClientTransport(restClient, mapper))

    /** sync [ElasticsearchClient] (raw Java client 가 필요할 때). 동일 시그니처. */
    @JvmStatic
    fun clientOf(
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        scheme: String = DEFAULT_SCHEME,
        username: String? = null,
        password: String? = null,
        sslContext: SSLContext? = null,
        mapper: JsonpMapper = JsonpMapper.lookup(),
    ): ElasticsearchClient {
        val transport = transportOf(host, port, scheme, username, password, sslContext, mapper)
        return ElasticsearchClient(transport)
    }

    /**
     * 공통 [ElasticsearchTransport] 빌드 헬퍼.
     *
     * 핵심 체인 outline:
     * ```
     * RestClient.builder(HttpHost(scheme, host, port))                    // HC5 HttpHost (scheme 첫 번째 — HC4와 순서 다름)
     *     .setHttpClientConfigCallback { http ->
     *         credentials?.let { http.setDefaultCredentialsProvider(it) }
     *         sslContext?.let { http.setSSLContext(it) }
     *         http
     *     }
     *     .build()
     *     .let { rest -> RestClientTransport(rest, mapper) }
     * ```
     */
    @JvmStatic
    fun transportOf(
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        scheme: String = DEFAULT_SCHEME,
        username: String? = null,
        password: String? = null,
        sslContext: SSLContext? = null,
        mapper: JsonpMapper = JsonpMapper.lookup(),
    ): ElasticsearchTransport {
        val credentials = if (username != null && password != null) {
            BasicCredentialsProvider().apply {
                setCredentials(
                    org.apache.hc.client5.http.auth.AuthScope(host, port),
                    UsernamePasswordCredentials(username, password.toCharArray())
                )
            }
        } else null

        val rest = RestClient.builder(HttpHost(scheme, host, port))
            .setHttpClientConfigCallback { http ->
                credentials?.let { http.setDefaultCredentialsProvider(it) }
                sslContext?.let { http.setSSLContext(it) }
                http
            }
            .build()
        return RestClientTransport(rest, mapper)
    }
}
```

**`object` 패턴 주의**: Kotlin `object` 안에 `companion object` 를 또 둘 수 없다 (compile error). 로깅이 필요한 `object` 는 `object Foo : KLogging()` 형태로 직접 상속한다. 본 spec 전반의 `object ... { companion object: KLoggingChannel() }` 패턴은 모두 `object ... : KLogging()` 또는 `object ... : KLoggingChannel()` 로 정정한다.

#### 4.2 testcontainer 통합 헬퍼 (별도 파일)

`bluetape4k-testcontainers` 모듈 측에 추가 (또는 본 모듈 testFixtures 에 추가) — production 배포에 testcontainer 가 따라가지 않도록.

**선택지**:
- (A) `infra/elasticsearch/src/testFixtures/kotlin/.../ElasticsearchTestSupport.kt` — Gradle `java-test-fixtures` plugin.
- (B) `testing/testcontainers` 측에 `ElasticsearchServer.asyncClient()` 확장 메서드.

**채택**: **(A) testFixtures**. testcontainers 모듈은 storage 측 helper 지원에 한정하고, ES Java Client 의존성은 elasticsearch 모듈 안에서만 선언.

```kotlin
package io.bluetape4k.elasticsearch.testfixtures

fun ElasticsearchServer.asyncClient(
    mapper: JsonpMapper = JsonpMapper.lookup(),   // 자동 감지 — Jackson 3/2 어느 쪽이든 OK
): ElasticsearchAsyncClient =
    ElasticsearchClients.asyncClientOf(
        host = host,
        port = port,
        scheme = "https",
        username = "elastic",
        password = password,
        sslContext = createSslContextFromCa(),
        mapper = mapper,
    )
```

#### 4.3 suspend 확장함수 (예시 — DocumentApiCoroutines)

```kotlin
package io.bluetape4k.elasticsearch.coroutines

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.core.*
import io.bluetape4k.coroutines.support.awaitSuspending

suspend fun <TDocument> ElasticsearchAsyncClient.indexSuspending(
    request: IndexRequest<TDocument>,
): IndexResponse = index(request).awaitSuspending()

suspend inline fun <reified TDocument> ElasticsearchAsyncClient.indexSuspending(
    crossinline block: IndexRequest.Builder<TDocument>.() -> IndexRequest.Builder<TDocument>,
): IndexResponse = index { it.block() }.awaitSuspending()

suspend fun <TDocument> ElasticsearchAsyncClient.getSuspending(
    request: GetRequest,
    documentClass: Class<TDocument>,
): GetResponse<TDocument> = get(request, documentClass).awaitSuspending()

// 동일 패턴: updateSuspending, deleteSuspending, existsSuspending, ...
```

**시그니처 규칙**:
- 메서드명: 원본 API + `Suspending` 접미사 (Kafka `suspendSend` 와 결을 맞추되, **명확성 우선**).
- 모든 suspend 확장은 `awaitSuspending()` 통해 cancellation propagation.
- inline reified 변형 제공 → `client.indexSuspending<Product> { it.index("products").document(p) }`.

### 섹션 5: Flow API 설계

#### 5.1 `bulkAsFlow` (BulkApiCoroutines.kt)

```kotlin
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem
import io.bluetape4k.elasticsearch.ElasticsearchDefaults.DEFAULT_BULK_CHUNK_SIZE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
// chunked import 제거 — buffer 를 직접 구현해 외부 의존성 최소화.
// kotlinx.coroutines.flow 에는 `chunked` 가 존재하지 않으며,
// 필요 시 io.bluetape4k.coroutines.flow.extensions.chunked 를 사용한다.

/**
 * [Flow] 형태로 들어오는 [BulkOperation] 들을 [chunkSize] 단위로 묶어 bulk 전송합니다.
 * Kafka `sendAsFlow` 패턴과 동형.
 *
 * ## Partial failure 처리 정책
 *
 * - 기본 정책: chunk 단위 [BulkResponse] 전체를 [Flow] 로 그대로 emit.
 *   사용자가 `BulkResponse.errors()` / `BulkResponse.items()` 를 직접 검사한다.
 * - **partial failure 는 throw 하지 않는다** — Kafka `send` 와 달리, item-level error 는
 *   [BulkResponse] 에 포함되어 응답된다. throw 하지 않음으로써 사용자가 retry / dead-letter 등
 *   custom 처리 전략을 자유롭게 선택할 수 있다.
 * - 옵션 콜백 [onItemError] 를 제공해 실패 item 만 골라 콜백으로 받을 수 있다 (default no-op).
 *
 * @param operations 발행할 [BulkOperation] flow
 * @param indexName  대상 인덱스 (operation 자체에 index 가 있으면 그 값이 우선)
 * @param chunkSize  한 BulkRequest 당 최대 operation 개수 (default [DEFAULT_BULK_CHUNK_SIZE])
 * @param onItemError partial failure 시 호출되는 콜백 (default no-op).
 *                    사용자가 retry / 로깅 / metric 등 처리. 인자는 실패한
 *                    [BulkResponseItem] 만 전달한다 — 원본 [BulkOperation] 은 ES Java
 *                    Client API 가 1:1 매핑을 보장하지 않아(특히 update 의 경우 partial
 *                    재시도를 위해 별도 컨텍스트 추적 필요) 의도적으로 제외.
 *                    사용자가 원본 op 가 필요하면 자체적으로 인덱스/문서 ID 키로 추적.
 * @return 각 BulkRequest 의 [BulkResponse] flow
 */
fun ElasticsearchAsyncClient.bulkAsFlow(
    operations: Flow<BulkOperation>,
    indexName: String? = null,
    chunkSize: Int = DEFAULT_BULK_CHUNK_SIZE,
    onItemError: (BulkResponseItem) -> Unit = { _ -> },
): Flow<BulkResponse> = flow {
    val buffer = ArrayList<BulkOperation>(chunkSize)
    operations.collect { op ->
        buffer.add(op)
        if (buffer.size >= chunkSize) {
            emit(flushBulk(buffer, indexName, onItemError))
            buffer.clear()
        }
    }
    if (buffer.isNotEmpty()) {
        emit(flushBulk(buffer, indexName, onItemError))
    }
}

/** 단일 BulkRequest 발행. */
suspend fun ElasticsearchAsyncClient.suspendBulk(
    request: BulkRequest,
): BulkResponse = bulk(request).awaitSuspending()
```

**Back-pressure**: `Flow` 가 suspend collector 이므로 chunk 빌드 + bulk 전송이 모두 suspend 안에서 일어나 자연스럽게 back-pressure 수렴. R3 위험 회피.

#### 5.2 `searchAsFlow` (SearchApiCoroutines.kt)

```kotlin
/**
 * search-after / PIT(point-in-time) 기반 페이징을 [Flow] 로 노출.
 * (scroll API 는 deprecated → search_after 채택)
 *
 * @param request   초기 [SearchRequest] (sort + tie-breaker 필수)
 * @param documentClass document 타입
 * @param batchSize 한 search 호출당 size (default 100)
 * @return 매 hit 단위의 [Hit] flow (lazy)
 */
fun <TDocument> ElasticsearchAsyncClient.searchAsFlow(
    request: SearchRequest,
    documentClass: Class<TDocument>,
    batchSize: Int = DEFAULT_SEARCH_BATCH_SIZE,
): Flow<Hit<TDocument>>
```

**구현 노트**:
- 1차: search_after pagination. PIT 자동 open/close. flow 종료 시 (`onCompletion` 및 `try-finally`) PIT close 보장.
- 사용자 측 Flow cancel 시 PIT leak 방지 — `try-finally` 가 `CancellationException` 도 포함해 처리. `runCatching` 으로 close 에러는 swallow (이미 leak 만 막으면 됨).
- `keepAlive` 는 batch 처리 시간 + retry 여유를 가질 수 있는 시간 (default 1m).

```kotlin
flow {
    val pit = client.openPointInTime { it.index(indexName).keepAlive(t -> t.time(keepAlive)) }.id()
    try {
        var searchAfter: List<FieldValue>? = null
        while (true) {
            val response = client.searchSuspending(SearchRequest.of { req ->
                req.pit { p -> p.id(pit).keepAlive(t -> t.time(keepAlive)) }
                    .sort(tieBreaker)
                    .searchAfter(searchAfter ?: emptyList())
                    .size(batchSize)
            }, docClass)
            val hits = response.hits().hits()
            if (hits.isEmpty()) break
            hits.forEach { emit(it.source()!!) }
            searchAfter = hits.last().sort()
        }
    } finally {
        // CancellationException 포함 모든 종료 경로에서 PIT close 보장.
        // close 자체가 실패해도 swallow — leak 만 막으면 충분.
        runCatching { client.closePointInTime { it.id(pit) } }
    }
}
```

#### 5.3 `BulkIngesterCoroutines` (BulkIngesterCoroutines.kt)

`BulkIngester<Context>` 는 ES Java Client 의 자동 flush 기반 bulk 수집기로, `maxOperations` / `maxSize` / `flushInterval` 중 하나라도 만족하면 자동 flush 한다. listener 콜백(`beforeBulk` / `afterBulk` / `afterBulk(error)`)을 통해 진행 상황을 관찰하는 구조.

본 모듈은 다음 3종을 제공한다.

```kotlin
package io.bluetape4k.elasticsearch.coroutines

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem
import io.bluetape4k.elasticsearch.ElasticsearchDefaults.DEFAULT_BULK_INGESTER_MAX_OPERATIONS
import io.bluetape4k.elasticsearch.ElasticsearchDefaults.DEFAULT_BULK_INGESTER_FLUSH_INTERVAL
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Duration
import kotlin.coroutines.resume

/**
 * [BulkIngester] factory — 자주 쓰는 옵션을 named parameter 로 노출.
 *
 * @param client          [ElasticsearchAsyncClient]
 * @param maxOperations   batch 한 회 최대 op 개수 (default [DEFAULT_BULK_INGESTER_MAX_OPERATIONS])
 * @param flushInterval   자동 flush 주기 (default [DEFAULT_BULK_INGESTER_FLUSH_INTERVAL])
 * @param listener        진행 상황 listener (optional). null 이면 ingester 측에 listener 미설정.
 */
fun <Context> bulkIngesterOf(
    client: ElasticsearchAsyncClient,
    maxOperations: Int = DEFAULT_BULK_INGESTER_MAX_OPERATIONS,
    flushInterval: Duration = DEFAULT_BULK_INGESTER_FLUSH_INTERVAL,
    listener: BulkListener<Context>? = null,
): BulkIngester<Context> {
    return BulkIngester.of { b ->
        b.client(client).maxOperations(maxOperations).flushInterval(flushInterval.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .also { listener?.let { l -> b.listener(l) } }
        b
    }
}

/**
 * [BulkIngester.add] 의 suspend 변형.
 *
 * BulkIngester 는 내부 capacity 가 가득 차면 add 가 block 한다 (`offer` 와 차이).
 * suspend 컨텍스트에서는 `withContext(Dispatchers.IO)` 로 감싸 caller 스레드를 보호.
 */
suspend fun <Context> BulkIngester<Context>.addSuspend(
    operation: BulkOperation,
    context: Context? = null,
) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    add(operation, context)
}

/**
 * BulkIngester 진행 상황을 [Flow] 로 노출하는 listener 어댑터.
 *
 * 사용법:
 * ```
 * val (listener, progressFlow) = bulkProgressListener<MyContext>()
 * val ingester = bulkIngesterOf(client, listener = listener)
 * launch { progressFlow.collect { event -> ... } }
 * ```
 */
sealed interface BulkProgressEvent<out Context> {
    data class Before<Context>(val executionId: Long, val operations: List<BulkOperation>, val contexts: List<Context?>) : BulkProgressEvent<Context>
    data class After<Context>(val executionId: Long, val items: List<BulkResponseItem>, val contexts: List<Context?>) : BulkProgressEvent<Context>
    data class Error<Context>(val executionId: Long, val cause: Throwable, val contexts: List<Context?>) : BulkProgressEvent<Context>
}

fun <Context> bulkProgressListener(
    capacity: Int = Channel.BUFFERED,
): Pair<BulkListener<Context>, Flow<BulkProgressEvent<Context>>> {
    val channel = Channel<BulkProgressEvent<Context>>(capacity)
    val listener = object : BulkListener<Context> {
        override fun beforeBulk(executionId: Long, request: co.elastic.clients.elasticsearch.core.BulkRequest, contexts: List<Context?>) {
            channel.trySend(BulkProgressEvent.Before(executionId, request.operations(), contexts))
        }
        override fun afterBulk(executionId: Long, request: co.elastic.clients.elasticsearch.core.BulkRequest, contexts: List<Context?>, response: co.elastic.clients.elasticsearch.core.BulkResponse) {
            channel.trySend(BulkProgressEvent.After(executionId, response.items(), contexts))
        }
        override fun afterBulk(executionId: Long, request: co.elastic.clients.elasticsearch.core.BulkRequest, contexts: List<Context?>, failure: Throwable) {
            channel.trySend(BulkProgressEvent.Error(executionId, failure, contexts))
        }
    }
    return listener to channel.receiveAsFlow()
}
```

**자원 정리**: `BulkIngester` 는 `AutoCloseable` — `use { }` 패턴 권장. `bulkProgressListener` 의 channel 은 ingester close 후에 별도 close 되어야 하므로, 사용자 코드에서 `try-finally` 또는 ingester 의 `close()` 후 channel.close() 호출을 KDoc 에 명시.

### 섹션 6: 테스트 전략

#### 6.1 `AbstractElasticsearchTest`

```kotlin
package io.bluetape4k.elasticsearch

import io.bluetape4k.LibraryName
import io.bluetape4k.elasticsearch.testfixtures.asyncClient
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.ElasticsearchServer

abstract class AbstractElasticsearchTest {

    // 일반 class 의 `companion object` 는 정상 — 이 패턴은 OK.
    companion object: KLoggingChannel() {
        const val TEST_INDEX_PREFIX = "$LibraryName.elasticsearch.test"
        const val REPEAT_SIZE = 3

        @JvmStatic
        protected val elasticsearch: ElasticsearchServer by lazy {
            ElasticsearchServer.Launcher.elasticsearch
        }

        @JvmStatic
        protected val asyncClient by lazy { elasticsearch.asyncClient() }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomIndexName(): String =
            "$TEST_INDEX_PREFIX.${Fakers.randomString(8, 16, true).lowercase()}"
    }
}
```

#### 6.2 테스트 클래스 별 시나리오

| 테스트 클래스 | 시나리오 |
|---------------|----------|
| `ElasticsearchClientsTest` | factory 메서드로 client 생성 후 ping/info 응답 검증; 잘못된 password → 예외 |
| `IndicesApiCoroutinesTest` | createIndex / existsIndex / deleteIndex (suspend); 동일 인덱스 중복 생성 시 예외 |
| `DocumentApiCoroutinesTest` | index → get → update → delete 라이프사이클; reified inline 변형 검증 |
| `SearchApiCoroutinesTest` | bulk 로 사전 색인 → match query / term query / range query (각 suspend); `searchAsFlow` 1000건 페이징 |
| `BulkApiCoroutinesTest` | `bulkAsFlow` 로 5000건 색인, chunkSize 500 → 10번 BulkResponse emit; 에러 operation 포함 시 partial 결과 검증 |
| `BulkIngesterCoroutinesTest` | `bulkIngesterOf` factory 검증, `addSuspend` 로 1000건 색인 후 `close()` 호출 시 모두 flush 됨; `bulkProgressListener` 가 `BulkProgressEvent.Before/After` 를 emit 하는지 검증 |
| `ClusterApiCoroutinesTest` | health / info / stats 호출 |
| `examples/ProductIndexExample` | 실전 시나리오 (data class + 색인 + 검색 + 집계) — runnable JUnit 테스트 |

**테스트 원칙**:
- 모든 테스트는 `runTest(timeout = 60.seconds)` (testcontainer + ES 부팅 고려).
- 각 테스트마다 unique index 사용 (`randomIndexName()`) → 격리.
- `@AfterEach` 에서 생성한 인덱스 cleanup (best-effort, 실패 무시).
- bluetape4k-assertions matcher 필수: `response.result() shouldBeEqualTo Result.Created` 등.

#### 6.3 테스트 리소스

`src/test/resources/junit-platform.properties`:
```
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
junit.jupiter.testinstance.lifecycle.default=per_class
```

`src/test/resources/logback-test.xml`: 표준 (다른 infra 모듈 복사 — `infra/kafka` 형식).

### 섹션 7: DoD (Definition of Done)

#### 코드
- [ ] `infra/elasticsearch/build.gradle.kts` 생성 (`api(Libs.elasticsearch_java)`, project 의존성).
- [ ] `Libs.kt` 에 `Versions.elasticsearch = "9.3.3"` + `elasticsearch_java` entry 추가.
- [ ] `ElasticsearchClients` factory + DSL builder 구현.
- [ ] `coroutines/` 6개 파일 (Document/Search/Indices/Bulk/BulkIngester/Cluster) suspend 확장 구현.
- [ ] `bulkAsFlow`, `searchAsFlow` 구현 + PIT 자원 정리.
- [ ] `BulkIngesterCoroutines.kt` 구현 (`bulkIngesterOf` + `addSuspend` + `bulkProgressListener` listener→Flow 어댑터).
- [ ] `support/JsonpMappers.kt` 구현 (`jacksonJsonpMapper()` / `jackson3JsonpMapper()` 헬퍼).
- [ ] `ElasticsearchDefaults.kt` (DEFAULT_BULK_CHUNK_SIZE / DEFAULT_SEARCH_BATCH_SIZE) 작성.
- [ ] `testFixtures` 에 `ElasticsearchServer.asyncClient()` 추가.

#### 테스트
- [ ] `AbstractElasticsearchTest` + 7개 테스트 클래스 작성.
- [ ] 모든 테스트 `runTest` + bluetape4k-assertions matcher 사용.
- [ ] `./gradlew :bluetape4k-elasticsearch:test` 통과 (passing count + duration 보고).
- [ ] 라인 커버리지 80% 이상 (수동 측정 또는 jacoco — 프로젝트 표준 따름).

#### 문서
- [ ] `infra/elasticsearch/README.md` (영어) — Architecture / Mermaid UML / Features / Examples.
- [ ] `infra/elasticsearch/README.ko.md` (한국어) — 동일 구조.
- [ ] 양쪽 README 상단 언어 전환 링크 (`[한국어](./README.ko.md) | English`, `한국어 | [English](./README.md)`).
- [ ] 모든 public 함수/클래스에 KDoc (한국어 가능). 동작/계약 + 사용 예제 포함.
- [ ] 루트 `CLAUDE.md` Module Groups 표 `infra/` 행에 `elasticsearch` 알파벳 순서로 추가.
- [ ] `docs/superpowers/index/2026-04.md` 항목 추가 + `docs/superpowers/INDEX.md` 카운트 갱신.
- [ ] `docs/testlogs/2026-04.md` 표 맨 위에 elasticsearch 테스트 결과 행 추가.

#### 워크플로우
- [ ] 작업은 worktree (`.worktrees/issue-146-elasticsearch`) 안에서.
- [ ] Commit 메시지 한국어 + prefix (`feat: infra/elasticsearch 신규 모듈 추가`).
- [ ] `oh-my-claudecode:code-reviewer` 또는 `pr-review-toolkit:code-reviewer` 실행 → HIGH/CRITICAL 해소.
- [ ] PR 본문에 테스트 결과 + 변경 근거 + 검증 명령 포함.
- [ ] `docs/superpowers/index/2026-04.md` 에 spec/plan 항목 추가.
- [ ] `/wiki-update` 실행 (spec/plan 신규 작성).

#### 검증 명령
```bash
./gradlew :bluetape4k-elasticsearch:build
./gradlew :bluetape4k-elasticsearch:test
./gradlew :bluetape4k-elasticsearch:detekt
./gradlew :bluetape4k-elasticsearch:test --tests "io.bluetape4k.elasticsearch.coroutines.BulkApiCoroutinesTest"
```

---

## 5. 채택 결정 요약

| 결정 항목 | 결정 | 근거 (출처) |
|-----------|------|------------|
| 접근법 | **C: Hybrid (확장함수 + 얇은 DSL config + Flow 어댑터)** | 기존 `infra/lettuce`, `infra/kafka` factory 패턴 (`infra/lettuce/src/main/kotlin/io/bluetape4k/lettuce/LettuceClients.kt`) |
| 모듈 위치 | `infra/elasticsearch` (kafka/lettuce 와 동일 카테고리) | 루트 `CLAUDE.md` Module Groups — `infra/` 행 |
| Base package | `io.bluetape4k.elasticsearch` | 프로젝트 패키지 컨벤션 (`io.bluetape4k.<module>`) |
| 메인 클라이언트 | `ElasticsearchAsyncClient` (sync 는 raw 노출만) | 공식 문서 — Async client 권장 (Reactive/Coroutines 친화) |
| 버전 통일 | `Versions.elasticsearch = "9.3.3"` (서버 TAG 통일) | `testing/testcontainers/storage/ElasticsearchServer.kt` TAG = `9.3.3` |
| Mapper 기본 | **`JsonpMapper.lookup()` (자동 감지)** — Jackson 3/2 헬퍼 함수(`jackson3JsonpMapper()`, `jacksonJsonpMapper()`) 별도 제공 | 공식 문서 — `JsonpMapper.lookup()` API ([elasticsearch-java 9.3 javadoc](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/installation.html)) |
| testcontainer 통합 | `testFixtures` 에 `asyncClient()` 확장 | Gradle `java-test-fixtures` plugin (production 배포에서 testcontainer 분리) |
| Spring Data ES | **Out of scope** (별도 후속 모듈) | 사용자 결정 — `spring-boot3/elasticsearch-data` 별도 PR |
| BulkIngester wrapping | **1차 포함** (`BulkIngesterCoroutines.kt`) — `bulkIngesterOf` + `addSuspend` + listener→Channel/Flow | `bulkAsFlow` (chunking 트랙)와 분리 → 이중 버퍼링 위험 회피. 자동 flush 시나리오 커버 |
| Search pagination | search_after + PIT (scroll API 미사용) | 공식 문서 — scroll deprecated, search_after 권장 ([Paginate search results](https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html)) |

---

## 6. Draft Task List

| # | Task | Complexity | 의존성 |
|---|------|-----------|--------|
| T01 | `Libs.kt` 버전 + `elasticsearch_java` entry 추가 + **`testing/testcontainers` 컴파일 검증**(`./gradlew :bluetape4k-testcontainers:compileKotlin`) | low | — |
| T02 | `infra/elasticsearch/build.gradle.kts` 작성 (testFixtures 포함; `bluetape4k-jackson` / `bluetape4k-jackson3` **양쪽 `compileOnly`** — R5) | low | T01 |
| T03 | 패키지 디렉터리 + `junit-platform.properties` + `logback-test.xml` 부트스트랩 + `ElasticsearchDefaults.kt` (R1: 라이프사이클 헬퍼 위치 마련) | low | T02 |
| T03a | `.claude/lib-sources/elasticsearch-java/` 에 `co.elastic.clients:elasticsearch-java:9.3.3` source jar 추출 (참고용) | low | T01 |
| T04 | `ElasticsearchClients` factory object 구현 + 단위 테스트 (R2 SSL/CA 자동 주입, R6 Virtual Thread 안전성 검증 — `ReentrantLock` 사용 여부 확인) | medium | T03 |
| T05 | `ElasticsearchClientDsl.kt` DSL builder + 단위 테스트 | low | T04 |
| T06 | `support/JsonpMappers.kt` — `jacksonJsonpMapper()` / `jackson3JsonpMapper()` 헬퍼 (옵션 사용; default 는 `JsonpMapper.lookup()`) | low | T04 |
| T07 | `coroutines/DocumentApiCoroutines.kt` suspend 확장 | medium | T04 |
| T08 | `coroutines/IndicesApiCoroutines.kt` suspend 확장 | medium | T04 |
| T09 | `coroutines/SearchApiCoroutines.kt` (suspend + `searchAsFlow` PIT). **task-specific risk**: R4 — Flow cancel 시 PIT close 보장(try-finally) 검증 | **high** | T04, T07 |
| T10 | `coroutines/BulkApiCoroutines.kt` (suspend + `bulkAsFlow`). **task-specific risk**: R3 — chunk 빌드 + bulk 전송이 모두 suspend 안에서 일어나도록 (BulkIngester 미사용) | **high** | T04, T07 |
| T10a | `coroutines/BulkIngesterCoroutines.kt` (`bulkIngesterOf` + `addSuspend` + `bulkProgressListener` listener→Flow). 자동 flush 트랙. | medium | T04, T07 |
| T11 | `coroutines/ClusterApiCoroutines.kt` suspend 확장 | low | T04 |
| T12 | `testFixtures` 에 `ElasticsearchServer.asyncClient()` 확장 | low | T04 |
| T13 | `AbstractElasticsearchTest` 베이스 작성 | low | T12 |
| T14 | `ElasticsearchClientsTest` (factory + ping/info) | medium | T13 |
| T15 | `IndicesApiCoroutinesTest` | medium | T08, T13 |
| T16 | `DocumentApiCoroutinesTest` (CRUD lifecycle) | medium | T07, T13 |
| T17 | `SearchApiCoroutinesTest` (term/match/range + `searchAsFlow` 페이징). **task-specific risk**: PIT leak 검증 — 5000건 강제 cancel 후 cluster `_pit/stats` 조회 | **high** | T09, T13 |
| T18 | `BulkApiCoroutinesTest` (`bulkAsFlow` 5000건 + 에러 partial). **task-specific risk**: partial failure 시 throw 하지 않음을 검증, `onItemError` 콜백 호출 여부 검증 | **high** | T10, T13 |
| T18a | `BulkIngesterCoroutinesTest` (`addSuspend` 1000건 + `bulkProgressListener` event 수신 검증) | medium | T10a, T13 |
| T19 | `ClusterApiCoroutinesTest` | low | T11, T13 |
| T20 | `examples/ProductIndexExample` (실전 시나리오 + 집계) | medium | T07–T11, T13 |
| T21 | `README.md` (영어) Architecture / Mermaid / Features / Examples | medium | T07–T11 |
| T22 | `README.ko.md` (한국어) 동일 구조 (R2 testcontainer 사용 예제 포함) | medium | T21 |
| T23 | 루트 `CLAUDE.md` Module Groups 표 업데이트 — **`infra/` 행에 `elasticsearch` 를 알파벳 순서로 추가** | low | T02 |
| T24 | `code-reviewer` 에이전트 실행 → HIGH/CRITICAL 해소 (R1 close 누수 검토 포함) | medium | T01–T22 |
| T25 | `docs/superpowers/index/2026-04.md` 항목 추가 + `docs/superpowers/INDEX.md` 카운트 갱신 + `/wiki-update` 실행 | low | spec, plan |
| T25a | `docs/testlogs/2026-04.md` 표 맨 위에 elasticsearch 모듈 테스트 결과 행 추가 (passing count + duration) | low | T14–T19 |
| T26 | PR 생성 (테스트 결과 + 검증 명령 포함) | low | T01–T25a |

**Complexity 분포**: low 14, medium 9, high 4 (T09 search_after+PIT, T10 bulkAsFlow chunking, T17/T18 통합 테스트).

**핵심 critical path**: T01 → T02 → T04 → T07 → T09/T10 → T17/T18 → T21/T22 → T24 → T26.

---

## 7. 다음 단계

이 spec 은 **draft (Step 1-S)** 상태이다. 다음 단계:
1. **리뷰 게이트**: critic 에이전트 또는 `oh-my-claudecode:code-reviewer` 로 spec 자체 리뷰. 미지수 Q1–Q4 결정.
2. **Step 2 (Plan 작성)**: 본 spec 의 Task list 를 `docs/superpowers/plans/2026-04-26-elasticsearch-coroutines-plan.md` 로 확장 (각 Task 의 RED/GREEN/REFACTOR + verify 명령 포함).
3. **Step 3 (Implementation)**: complexity high task 는 `model=opus`, medium 은 sonnet, low 는 haiku 에 라우팅.
4. **PR 생성 전**: 로컬 테스트 전수 통과 + code-reviewer 통과 + README 양쪽 작성 + KDoc 완성.
