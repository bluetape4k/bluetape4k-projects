# FalkorDB Testcontainers Server 구현 Plan

- **작성일**: 2026-04-26
- **모듈**: `testing/testcontainers` (`bluetape4k-testcontainers`)
- **브랜치/Worktree**: `feat/falkordb-testcontainers` → `.worktrees/feat-falkordb-testcontainers`
- **연관 Spec**: [`docs/superpowers/specs/2026-04-26-falkordb-testcontainers-design.md`](../specs/2026-04-26-falkordb-testcontainers-design.md)
- **상태**: Draft (구현 시작 전)

---

## 0. 개요

`testing/testcontainers` 모듈에 FalkorDB(구 RedisGraph) Testcontainers 래퍼를 추가한다.
`MemgraphServer` 구조를 그대로 따르며, FalkorDB 의 Redis 와이어 프로토콜 특수성과
`jfalkordb` (Jedis 기반) 클라이언트 호환성을 반영한다.

### 선행 조건
- worktree `.worktrees/feat-falkordb-testcontainers` 가 이미 존재한다.
- spec 이 승인된 상태이다.
- Docker Desktop 이 실행 중이고 `falkordb/falkordb:v4.18.1` 이미지가 pull 가능하다.

### 일관성 규칙 (모든 태스크 공통)
- **Kotlin Edit 워크플로우**: `.kt` 파일 편집 후 즉시 `ide_diagnostics` → import 오류 정리 → 빌드.
- **표준 패턴 준수**: `MemgraphServer.kt` / `MemgraphServerTest.kt` 와 100% 동일한 클래스 구조.
- **테스트 도구**: JUnit 5 + bluetape4k-assertions + AbstractContainerTest 만 사용 (Kotest/MockK 불필요).
- **`atomicfu` 금지**: 본 작업에는 atomic 상태 없음.
- **로깅**: `companion object: KLogging()` + `log.debug { ... }` 형식.

---

## 1. 태스크 의존 그래프

```
T1 (Libs.kt)
  └─> T2 (build.gradle.kts)
        └─> T3 (FalkorDBServer.kt)
              └─> T4 (FalkorDBServerTest.kt)
                    └─> T5 (compile + test + testlog)
                          └─> T6 (README.md / README.ko.md)
                                └─> T7 (superpowers index + INDEX.md)
```

선행관계 위반 금지 — 각 태스크는 직전 태스크의 검증이 끝난 후 진행한다.

---

## 2. 태스크 상세

### T1 — `Libs.jfalkordb` 추가  <complexity: low>

**목표**: `buildSrc/src/main/kotlin/Libs.kt` 의 Graph DB 섹션(Neo4j 항목 바로 아래)에
`jfalkordb` Maven 좌표 상수를 추가한다.

**파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/buildSrc/src/main/kotlin/Libs.kt`

**위치**: 라인 1217 의 `neo4j_java_driver` 정의 바로 아래에 새 줄 추가.

**변경 내용**:
```kotlin
// Neo4j
const val neo4j_java_driver = "org.neo4j.driver:neo4j-java-driver:5.28.4"  // https://mvnrepository.com/artifact/org.neo4j.driver/neo4j-java-driver

// FalkorDB
const val jfalkordb = "com.falkordb:jfalkordb:0.7.0"  // https://mvnrepository.com/artifact/com.falkordb/jfalkordb
```

**구현 지침**:
- Edit 도구로 `neo4j_java_driver` 라인을 anchor 로 사용하여 그 뒤에 두 줄 추가.
- 다른 `Neo4j` 섹션 주석을 변경하지 않는다 (검색 anchor 보존).
- 좌표 검증: 추가 후 `./gradlew :bluetape4k-testcontainers:dependencies | grep jfalkordb` 또는 빌드 단계에서 dependency resolution 으로 자동 검증된다.

**주의사항**:
- `0.7.0` 이 Maven Central 에 publish 되어 있는지 빌드 단계(T5)에서 확인. 만약 resolution 실패 시 spec §4.4 의 fallback (`0.6.0`) 으로 즉시 조정하고 plan 본 문서에 메모.
- `Versions` object 별도 추가는 불필요 — 단일 라이브러리 단일 버전.

**완료 기준**:
- [ ] `Libs.kt` 에 `const val jfalkordb` 정의 추가됨.
- [ ] 컴파일 영향 없음 (이 단계에서는 호출 site 가 없으므로 빌드 자동 통과).

---

### T2 — `compileOnly(Libs.jfalkordb)` 추가  <complexity: low>

**목표**: `testing/testcontainers/build.gradle.kts` 의 Graph DB 섹션에 `jfalkordb` 의존성을 `compileOnly` 로 추가한다.

**파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/testing/testcontainers/build.gradle.kts`

**위치**: 라인 81~84 의 `// Graph DB (Neo4j)` 섹션 바로 아래.

**변경 내용**:
```kotlin
// Graph DB (Neo4j)
compileOnly(Libs.testcontainers_neo4j)
compileOnly(Libs.neo4j_java_driver)

// Graph DB (FalkorDB - Redis protocol)
compileOnly(Libs.jfalkordb)
```

**구현 지침**:
- Edit 도구로 `compileOnly(Libs.neo4j_java_driver)` 라인 뒤에 빈 줄 + 주석 + `compileOnly(Libs.jfalkordb)` 추가.
- `testImplementation(Libs.jfalkordb)` 는 명시 불필요 — 파일 상단 1~3 라인의
  `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }` 가
  자동으로 `compileOnly` 를 `testImplementation` 으로 확장해 준다.
- `testcontainers_falkordb` 같은 Testcontainers 공식 모듈은 존재하지 않음 — `GenericContainer` 직접 사용하므로 추가 불필요.

**주의사항**:
- 의존성 의도가 명확하도록 주석 `// Graph DB (FalkorDB - Redis protocol)` 를 함께 추가.
- `testRuntimeOnly` 추가 금지 — `compileOnly` 만으로 충분 (테스트가 jfalkordb 를 직접 import 하므로 `extendsFrom` 으로 자동 노출).

**완료 기준**:
- [ ] `build.gradle.kts` 에 `compileOnly(Libs.jfalkordb)` 추가됨.
- [ ] `./gradlew :bluetape4k-testcontainers:dependencies --configuration compileClasspath | grep jfalkordb` 에서 좌표 노출 확인 (T5 단계에서 함께 검증).

---

### T3 — `FalkorDBServer.kt` 작성  <complexity: medium>

**목표**: `MemgraphServer.kt` 와 동일한 구조로 FalkorDB Testcontainers 래퍼를 작성한다.

**파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/graphdb/FalkorDBServer.kt`  (신규)

**구현 지침**:

1. **패키지/임포트**: spec §4.2 의 import 블록을 그대로 사용. `WaitAllStrategy` 와 `java.time.Duration` 추가.

2. **클래스 시그니처** (private 생성자 + 3개 인터페이스):
   ```kotlin
   class FalkorDBServer private constructor(
       imageName: DockerImageName,
       useDefaultPort: Boolean = false,
       reuse: Boolean = true,
   ): GenericContainer<FalkorDBServer>(imageName), GenericServer, PropertyExportingServer
   ```

3. **companion object 상수**:
   - `IMAGE = "falkordb/falkordb"`
   - `TAG = "v4.18.1"` (Apple Silicon multi-arch 지원 확인된 최신)
   - `NAME = "falkordb"`
   - `REDIS_PORT = 6379`
   - `private val START_TIMEOUT: Duration = Duration.ofSeconds(120)` — CI 콜드 스타트 대비 (WITH_OUTER_TIMEOUT 은 글로벌 단일 cap)
   - `private const val READY_LOG_REGEX = ".*Ready to accept connections.*"`

4. **companion `invoke` 두 오버로드** (MemgraphServer 와 시그니처 동일):
   - `(DockerImageName, Boolean, Boolean) -> FalkorDBServer`
   - `(String, String, Boolean, Boolean) -> FalkorDBServer` — `image.requireNotBlank("image")`, `tag.requireNotBlank("tag")` 검증.

5. **인터페이스 구현 멤버**:
   - `override val port: Int get() = getMappedPort(REDIS_PORT)`
   - `override val url: String get() = "redis://$host:$port"`
   - `override val propertyNamespace: String = NAME`
   - `override fun propertyKeys(): Set<String> = setOf("host", "port", "url")`
   - `override fun properties(): Map<String, String> = mapOf("host" to host, "port" to port.toString(), "url" to url)`
   - **`redisUrl` 필드 절대 추가하지 말 것** (spec 결정사항: YAGNI — `url` 과 동일).

6. **`init` 블록**:
   - `addExposedPorts(REDIS_PORT)`
   - `withReuse(reuse)`
   - `waitingFor(WaitAllStrategy(WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT).withStrategy(Wait.forLogMessage(READY_LOG_REGEX, 1)).withStrategy(Wait.forListeningPort()).withStartupTimeout(START_TIMEOUT))`
   - `if (useDefaultPort) exposeCustomPorts(REDIS_PORT)`

7. **`start()` override**: `super.start()` → `writeToSystemProperties()`. KDoc 에 등록되는 시스템 프로퍼티 3개 (`host`, `port`, `url`) 만 명시 — spec 의 잘못된 5개 목록(`redis-port`, `redis-url` 포함)이 아닌, **실제 `propertyKeys()` 에 정의한 3개만** 기록.

8. **`Launcher` object**:
   - `val falkordb: FalkorDBServer by lazy { FalkorDBServer().apply { start(); ShutdownQueue.register(this) } }`

9. **KDoc 작성 규칙**:
   - 클래스 KDoc: spec §4.2 와 동일한 한국어 설명. **`driver.graph()` 사용 예시** 포함 (`selectGraph()` 가 아님).
   - 모든 public 멤버에 KDoc.
   - **경고 KDoc**: "RedisServer 와 호스트 포트 6379 를 동시에 점유할 수 없습니다." — `useDefaultPort` 파라미터 KDoc 에 포함.

**주의사항**:
- 파일 길이 200 라인 이내.
- spec §4.2 코드 블록을 거의 그대로 따르되, **KDoc 의 `start()` 시스템 프로퍼티 목록은 3개로 정정** (spec 의 5개 목록은 redisUrl 제거 결정 전 작성된 잔존물).
- `WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT`: 전체 WaitAllStrategy에 글로벌 단일 cap 적용 (START_TIMEOUT = 120s). 내부 strategy들도 개별적으로 120s 설정됨. 합산이 아닌 단일 상한.
- `java.time.Duration` import 누락 주의.

**검증**:
- `ide_diagnostics` 로 import 오류 0건 확인.
- `ide_optimize_imports` 로 정렬.

**완료 기준**:
- [ ] `FalkorDBServer.kt` 신규 생성, 200 라인 이내.
- [ ] 모든 public 멤버에 KDoc.
- [ ] `MemgraphServer` 와 동형 구조 (private ctor + invoke 오버로드 2개 + Launcher).
- [ ] `redisUrl` 필드 없음.
- [ ] import 오류 0건 (`ide_diagnostics`).

---

### T4 — `FalkorDBServerTest.kt` 작성  <complexity: medium>

**목표**: `MemgraphServerTest.kt` 패턴을 따르되, jfalkordb 클라이언트 통합 테스트를 추가한다.

**파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/graphdb/FalkorDBServerTest.kt`  (신규)

**구현 지침**:

1. **클래스 선언**: `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` + `class FalkorDBServerTest : AbstractContainerTest()`.

2. **companion + 상수**:
   ```kotlin
   companion object: KLogging() {
       private const val GRAPH_NAME = "social"
   }
   ```

3. **싱글턴 사용**: `private val falkordb = FalkorDBServer.Launcher.falkordb`

4. **필수 테스트 케이스** (10개 — 한국어 backtick naming):

   | # | 테스트명 | 검증 |
   |---|---------|------|
   | 1 | ``FalkorDB 서버가 실행 중이어야 한다`` | `falkordb.isRunning.shouldBeTrue()` |
   | 2 | ``Redis 포트가 0보다 커야 한다`` | `falkordb.port shouldBeGreaterThan 0` |
   | 3 | ``url 은 redis 스킴을 사용해야 한다`` | `falkordb.url shouldContain "redis://"` |
   | 4 | ``propertyNamespace 는 falkordb 이어야 한다`` | `shouldBeEqualTo "falkordb"` |
   | 5 | ``propertyKeys 는 host port url 을 포함해야 한다`` | 3개 키 `shouldContain` |
   | 6 | ``properties 는 모든 키에 비어있지 않은 값을 반환해야 한다`` | 각 key 에 대해 `props[key].shouldNotBeNull()` + `(props[key]!!.isNotBlank()).shouldBeTrue()` |
   | 7 | ``시스템 프로퍼티에 host port url 이 등록되어야 한다`` | `System.getProperty("testcontainers.falkordb.host"/"port"/"url").shouldNotBeNull()` |
   | 8 | ``jfalkordb 클라이언트로 그래프 쿼리를 실행할 수 있어야 한다`` | (아래 상세) |
   | 9 | ``blank image 는 허용하지 않는다`` | `assertFailsWith<IllegalArgumentException> { FalkorDBServer(image = " ") }` |
   | 10 | ``blank tag 는 허용하지 않는다`` | `assertFailsWith<IllegalArgumentException> { FalkorDBServer(tag = " ") }` |

5. **테스트 #8 (jfalkordb 통합)** — 검증된 jfalkordb 0.7.0 API 기반:
   ```kotlin
   // Driver extends Closeable, Graph(GraphContextGenerator) extends Closeable → .use {} 중첩
   FalkorDB.driver(falkordb.host, falkordb.port).use { driver ->
       driver.graph(GRAPH_NAME).use { graph ->
           try {
               graph.query("CREATE (:Person {name: 'Alice'})")
               // ResultSet extends Iterable<Record> → toList() 사용
               val rows = graph.query("MATCH (p:Person) RETURN p.name AS name").toList()
               rows.shouldNotBeEmpty()
               // Record.getString(key) 존재 (getValue(key) 도 가능)
               rows.first().getString("name") shouldBeEqualTo "Alice"
           } finally {
               runCatching { graph.deleteGraph() }
           }
       }
   }
   ```
   - `driver.graph(name)` → `GraphContextGenerator` (extends `Graph extends Closeable`)
   - `Record.getString(key)` — jfalkordb 0.7.0 에 실재 확인 (javap 검증 완료)
   - **`deleteGraph()` + `runCatching`** — assert 실패해도 graph 정리 보장.

6. **bluetape4k-assertions matcher 규칙** (CRITICAL — `feedback_bluetape4k_assertions_comparison_matchers`):
   - `> < >= <=` 비교는 `shouldBeGreaterThan` / `shouldBeLessThan` / `shouldBeGreaterOrEqualTo` 등 사용. `(x > y).shouldBeTrue()` 금지.
   - `isNotBlank()` 같은 boolean 술어는 `(...).shouldBeTrue()` 허용.

**필수 import 목록** (누락 시 compile 실패):
```kotlin
import com.falkordb.FalkorDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEmpty   // ← 누락하기 쉬운 import
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertFailsWith
```

**주의사항**:
- **jfalkordb 로만 테스트** — jfalkordb 가 Jedis 를 내부 driver 로 사용하므로, `FalkorDB.driver(host, port)` 로 연결. Lettuce 의존성은 추가하지 않음.
- `@BeforeEach` / `@AfterEach` 에서 graph 정리하지 않음 — 각 테스트는 독립적이며 #8 만 graph 를 사용하고 finally 로 정리.
- `runTest` 사용하지 않음 — 모든 테스트가 동기.

**검증**:
- `ide_diagnostics` 로 import 오류 0건.
- jfalkordb 패키지(`com.falkordb.FalkorDB`, `com.falkordb.Graph`)가 `compileOnly` 의존성 + `extendsFrom` 으로 testCompile classpath 에 노출되는지 확인.

**완료 기준**:
- [ ] `FalkorDBServerTest.kt` 신규 생성.
- [ ] 10개 테스트 케이스 작성.
- [ ] jfalkordb 통합 테스트 (`graph.query` CREATE + MATCH + `deleteGraph`).
- [ ] import 오류 0건.

---

### T5 — 컴파일 + 테스트 실행 + testlog 기록  <complexity: medium>

**목표**: 변경 사항을 컴파일하고 테스트를 실행하여 모든 케이스 PASS 를 확인한 후, testlog 에 기록한다.

**선행**: T1~T4 완료.

**실행 명령** (worktree 루트에서):
```bash
./gradlew :bluetape4k-testcontainers:compileKotlin --no-daemon
./gradlew :bluetape4k-testcontainers:compileTestKotlin --no-daemon
./gradlew :bluetape4k-testcontainers:test --tests "*FalkorDBServerTest*" --info
```

**구현 지침**:
1. **의존성 검증**: 첫 빌드 시 jfalkordb `0.7.0` 좌표 resolution 확인.
   - 실패 시 → Maven Central 에서 최신 stable 버전 검색 → `Libs.jfalkordb` 좌표 갱신 → 재시도.
2. **컴파일 오류 발생 시**:
   - import 누락 → `ide_optimize_imports`.
   - `WaitAllStrategy` 미해결 → `org.testcontainers.containers.wait.strategy.WaitAllStrategy` import 추가.
   - `@Deprecated` 경고 → `lsp_code_actions` 로 Quick Fix.
3. **테스트 실패 시 진단 절차** (환경 탓 금지):
   - `Wait` strategy timeout → 로그에서 `Ready to accept connections` 출력 여부 확인.
     - 출력 없음 → 다른 패턴(`Loaded RedisGraph`, `Module loaded` 등)으로 교체.
   - `ERR Module not yet loaded` → wait strategy 가 module 로드까지 보장하지 못함 → `Wait.forLogMessage` 패턴 보강.
   - jfalkordb `Connection refused` → 컨테이너 헬스체크 실패 → `withStartupTimeout` 증가 (60s → 90s).
   - Apple Silicon `exec format error` → KDoc + `withImagePullPolicy(PullPolicy.alwaysPull())` 추가, 또는 `tag` 변경 검토.

4. **testlog 기록** (테스트 결과 PASS 여부와 무관하게 반드시 기록):
   - 파일: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/docs/testlogs/2026-04.md`
   - 위치: 이달 파일 표 **맨 위 행** (지침→요약→최신순).
   - 컬럼: 날짜, 모듈, 변경 요약, 테스트 결과 (passing count + duration), 비고.
   - 예시 행:
     ```
     | 2026-04-26 | bluetape4k-testcontainers | FalkorDBServer 추가 | 10/10 PASS, ~Xs | feat/falkordb-testcontainers |
     ```

**완료 기준**:
- [ ] `compileKotlin` 성공.
- [ ] `compileTestKotlin` 성공.
- [ ] `*FalkorDBServerTest*` 10개 케이스 모두 PASS.
- [ ] testlog `docs/testlogs/2026-04.md` 갱신 (맨 위 행).
- [ ] 결과(passing count + duration) 를 다음 단계로 인계.

---

### T6 — README.md / README.ko.md 갱신  <complexity: low>

**목표**: `testing/testcontainers/README.md` 와 `README.ko.md` 양쪽에서 FalkorDB 항목을 추가한다.

**파일**:
- `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/testing/testcontainers/README.md`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/testing/testcontainers/README.ko.md`

**선행**: T5 완료 (테스트 PASS 후 문서 작성 — 동작이 검증된 후 예시 작성).

**구현 지침**:

1. **현재 구조 확인**: 두 파일을 Read 하여 "Supported Servers" / "지원하는 서버" 표 위치, Mermaid 다이어그램, 코드 예시 섹션 파악.

2. **변경 항목 (양쪽 동일하게)**:
   - **a) Supported Servers 표 (Graph DB 영역)**: Memgraph 행 아래에 FalkorDB 행 추가.
     - 영문: `| FalkorDB | Graph DB | OpenCypher graph database on Redis wire protocol | `falkordb/falkordb:v4.18.1` |`
     - 한글: `| FalkorDB | 그래프 DB | Redis 와이어 프로토콜 기반 OpenCypher 그래프 DB | `falkordb/falkordb:v4.18.1` |`
   - **b) Mermaid 다이어그램**: graphdb 패키지를 표시하는 다이어그램이 있다면 `FalkorDBServer` 노드 추가. (없다면 건너뛰기.)
   - **c) 코드 예시 섹션**: graphdb 카테고리에 사용 예시 1개 추가.
     ```kotlin
     val falkordb = FalkorDBServer.Launcher.falkordb
     FalkorDB.driver(falkordb.host, falkordb.port).use { driver ->
         val graph = driver.graph("social")
         graph.query("CREATE (:Person {name: 'Alice'})")
         val rows = graph.query("MATCH (p:Person) RETURN p.name AS name").toList()
         println(rows.first().getValue("name"))
         graph.deleteGraph()
     }
     ```

3. **언어 전환 링크 검증**: 두 README 모두 상단에 `[한국어](./README.ko.md) | English` 또는 그 반대 링크가 있어야 함 (기존 유지).

**주의사항**:
- 두 파일 변경 내용을 **동기화 (sync-update)** — 영문/한글 표의 행 개수, 예시 개수가 일치해야 함.
- 코드 블록 backtick fence 와 들여쓰기 보존.
- README 에서 `redisUrl` 언급 금지 (spec 결정).

**완료 기준**:
- [ ] `README.md` 의 Supported Servers 표에 FalkorDB 행 추가.
- [ ] `README.ko.md` 의 동일 표에 한국어 행 추가.
- [ ] 양쪽에 코드 예시 추가 (Launcher 사용).
- [ ] Mermaid 다이어그램이 graphdb 노드를 표시하는 경우 FalkorDBServer 추가.

---

### T7 — superpowers index 추가 + INDEX.md count  <complexity: low>

**목표**: `docs/superpowers/index/2026-04.md` 에 본 작업 entry 를 추가하고, 허브 `docs/superpowers/INDEX.md` 의 month count 를 갱신한다.

**선행**: T6 완료 (모든 코드/문서 변경 종결 후).

**파일**:
- `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/docs/superpowers/index/2026-04.md`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/docs/superpowers/INDEX.md`

**구현 지침**:

1. **2026-04.md entry 추가**:
   - 기존 형식 (다른 entry 들) 확인 후 동일 컬럼/형식으로 작성.
   - **이달 파일 맨 위에 새 행 추가** (지침→요약→최신순; LOG 문서 포맷).
   - entry 내용:
     - spec: `docs/superpowers/specs/2026-04-26-falkordb-testcontainers-design.md`
     - plan: `docs/superpowers/plans/2026-04-26-falkordb-testcontainers-plan.md`
     - 요약: "FalkorDBServer testcontainers 추가 (jfalkordb 통합)"

2. **INDEX.md count 갱신**:
   - 2026-04 항목의 entry count 를 +1.
   - 다른 행 변경 금지.

**주의사항**:
- entry 추가 위치를 다른 entry 와 동일한 표 형식으로 맞춤.
- 두 파일 모두 commit 에 포함.

**완료 기준**:
- [ ] `docs/superpowers/index/2026-04.md` 에 entry 1행 추가.
- [ ] `docs/superpowers/INDEX.md` 에서 2026-04 count 가 +1.

---

## 3. PR 전 최종 체크리스트 (참고)

> 본 plan 의 T1~T7 완료 후 별도 단계로 진행 (plan scope 외이지만 명시).

- [ ] `oh-my-claudecode:code-reviewer` 실행 → HIGH/CRITICAL 이슈 0건.
- [ ] `git status` 깨끗 (의도된 변경만 staged).
- [ ] commit 메시지: `feat: FalkorDBServer testcontainers 추가` (한국어 + prefix).
- [ ] PR 본문에 테스트 결과(`./gradlew :bluetape4k-testcontainers:test --tests "*FalkorDBServerTest*"` 출력 핵심부) + 변경 파일 요약 포함.
- [ ] `gh pr create` 로 PR 생성.

---

## 4. 위험 대응 요약

| 위험 | 트리거 | 대응 |
|------|--------|------|
| jfalkordb `0.7.0` 미존재 | T5 빌드 dependency resolution 실패 | Maven Central 검색 → `Libs.jfalkordb` 좌표 갱신 |
| Wait strategy `Ready to accept connections` 미출력 | T5 컨테이너 startup timeout | 다른 로그 패턴(`Loaded RedisGraph`) 시도 → `withStartupTimeout` 90s 증가 |
| jfalkordb `Module not yet loaded` | T5 통합 테스트 실패 | wait strategy 보강 (다중 패턴 OR로 결합 검토) |
| Apple Silicon `exec format error` | T5 컨테이너 시작 실패 | KDoc + `withImagePullPolicy(PullPolicy.alwaysPull())` 또는 platform 강제 옵션 |
| RedisServer 와 포트 6379 충돌 | 사용자가 `useDefaultPort=true` 동시 사용 | KDoc 경고로 사전 안내 (이미 spec 에 반영) |

---

## 5. 완료 보고 템플릿

T7 완료 후 사용자에게 보고할 형식:

```
완료 태스크:
- T1: Libs.jfalkordb 추가 (0.7.0)
- T2: build.gradle.kts compileOnly 추가
- T3: FalkorDBServer.kt 작성 (XX 라인)
- T4: FalkorDBServerTest.kt 작성 (10 케이스)
- T5: 빌드 + 테스트 PASS (10/10, Xs)
- T6: README.md / README.ko.md 갱신
- T7: superpowers index entry + INDEX count

테스트 결과: 10/10 PASS, X.Xs
변경 파일: 7개
다음 단계: code-reviewer 실행 → commit → PR
```
