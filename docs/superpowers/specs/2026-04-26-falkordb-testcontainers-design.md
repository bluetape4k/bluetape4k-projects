# FalkorDB Testcontainers Server Spec

- **작성일**: 2026-04-26
- **모듈**: `testing/testcontainers` (`bluetape4k-testcontainers`)
- **브랜치/Worktree**: `feat/falkordb-testcontainers` → `.worktrees/feat-falkordb-testcontainers`
- **저자**: bluetape4k design loop
- **상태**: Draft (Step 1-S, 사용자 승인 대기)

---

## 1. 목적 및 배경

### 1.1 배경

FalkorDB(구 RedisGraph)는 Redis 모듈로 동작하는 그래프 데이터베이스로, OpenCypher 호환 쿼리 언어와
Redis 와이어 프로토콜을 통해 접속한다. bluetape4k 에서는 다음과 같이 두 곳에서 FalkorDB를 사용한다.

1. **`bluetape4k-graph/graph/graph-falkordb`** — 운영 모듈 (jfalkordb 클라이언트 래퍼)
2. **운영 모듈의 `testFixtures`** — 자체 `FalkorDBServer` 컨테이너 (현재 위치)

문제는 자체 testFixtures 의 `FalkorDBServer`가 `testing/testcontainers` 표준 패턴에서 벗어나 있다는 점이다.

### 1.2 현재 갭

| 항목 | 현재 (graph testFixtures) | 표준 패턴 (`testing/testcontainers`) |
|------|---------------------------|--------------------------------------|
| 패키지 | `io.bluetape4k.graph.falkordb` | `io.bluetape4k.testcontainers.graphdb` |
| 인터페이스 | `GenericContainer<FalkorDBServer>` 만 | + `GenericServer`, `PropertyExportingServer` |
| `port` | plain `val` | `override val port: Int` |
| `url` | plain `val` | `override val url: String` |
| `propertyNamespace` | 없음 | `"falkordb"` |
| `propertyKeys()` | 없음 | 필수 구현 |
| `properties()` | 없음 | 필수 구현 |
| `start()` override | 없음 | `writeToSystemProperties()` 호출 필수 |
| `useDefaultPort` | `addFixedExposedPort` 직접 호출 | `exposeCustomPorts(REDIS_PORT)` 호출 |

### 1.3 목적

`testing/testcontainers` 모듈의 `io.bluetape4k.testcontainers.graphdb` 패키지에
**표준 패턴을 따르는 `FalkorDBServer`** 를 추가하여:

1. 다른 모듈(`bluetape4k-spring-boot3-*` 등)에서 `Libs` 가 아닌 `testing-testcontainers` 의존성으로 재사용 가능하도록 한다.
2. `MemgraphServer` / `Neo4jServer` / `PostgreSQLAgeServer` 와 동일한 인터페이스 계약을 만족하여
   향후 contract test (속성 키 검증, 시스템 프로퍼티 등록 동작)에서 일관되게 검증된다.
3. `testcontainers.falkordb.host|port|url` 시스템 프로퍼티를 자동 등록하여
   Spring Boot `application.yml` 에서 placeholder 로 직접 사용 가능하게 한다.

### 1.4 비목표 (Out of Scope)

- `bluetape4k-graph/graph/graph-falkordb/src/testFixtures/` 에 있는 기존 `FalkorDBServer` 제거 (별도 PR에서 마이그레이션)
- `RediSearch` / `RedisJSON` 등 다른 Redis Stack 모듈 컨테이너 추가
- FalkorDB Cluster 구성 지원

---

## 2. 리스크 및 실패 모드

### 2.1 R1 — 포트 6379 충돌 (Redis 와의 혼동)

**현상**: FalkorDB는 Redis 와이어 프로토콜을 사용하므로 포트 `6379`를 노출한다.
같은 JVM 안에서 `RedisServer` 와 `FalkorDBServer` 가 동시에 `useDefaultPort = true` 로 시작되면
호스트 포트 `6379` 가 충돌한다. 또한 시스템 프로퍼티 키가 `testcontainers.redis.*` 와 `testcontainers.falkordb.*`
로 분리되어 있어, FalkorDB를 `RedisServer` 로 오해하여 `Lettuce` 클라이언트가 잘못된 endpoint 로 붙을 가능성이 있다.

**완화책**:
- `useDefaultPort` 기본값은 `false` 로 유지 (랜덤 호스트 포트). 사용자가 명시적으로 `true` 를 줄 때만 6379 고정.
- `propertyNamespace = "falkordb"` 로 키 공간 분리 (`testcontainers.falkordb.host` 등) — `redis` 와 절대 섞이지 않도록 한다.
- KDoc 에 "RedisServer 와 같은 호스트 포트를 동시에 점유할 수 없습니다" 명시.

### 2.2 R2 — Wait Strategy 미흡으로 인한 false-ready

**현상**: FalkorDB 컨테이너는 Redis 데몬 부팅 → FalkorDB 모듈 로드 → 명령 수신 가능 의 3단계가 있다.
`Wait.forListeningPort()` 만 사용하면 포트는 열렸지만 모듈 로드가 끝나기 전에 `start()` 가 반환되어
첫 GRAPH.QUERY 호출이 `ERR Module not yet loaded` 를 반환할 수 있다.

**완화책**:
- `WaitAllStrategy(WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT)` 로 두 전략을 결합:
  1. `Wait.forLogMessage(".*Ready to accept connections.*", 1)` — Redis 부팅 완료 신호
  2. `Wait.forListeningPort()` — TCP 리스닝 확인
- `WITH_OUTER_TIMEOUT` 모드: 각 내부 전략 타임아웃을 outer 값으로 덮어쓰고, 전체 실행도 동일 outer 값으로 감쌈 → **글로벌 상한 = START_TIMEOUT (단일 cap, 합산 아님)**. 두 전략이 순차 실행되므로 실제 허용 시간은 60s (additive 아님).
- CI 콜드 스타트 대비 `START_TIMEOUT = 120s` 로 설정.
- 향후 FalkorDB 가 자체 ready 로그를 추가하면 그 패턴으로 교체.

### 2.3 R3 — 새 의존성 `Libs.jfalkordb` 미정의

**현상**: 리서치 단계에서 `Libs.jfalkordb = "com.falkordb:jfalkordb:0.7.0"` 가 이미 존재한다고 가정했으나,
worktree 의 `buildSrc/src/main/kotlin/Libs.kt` 에 실제로 정의되어 있지 않다.
`compileOnly(Libs.jfalkordb)` 만 추가하면 컴파일 실패.

**완화책**:
- 본 spec 의 구현 단계에서 **`Libs.jfalkordb` 정의를 함께 추가**해야 한다.
- 위치: `buildSrc/src/main/kotlin/Libs.kt` Redis/Graph 섹션.
- 버전 검증: Maven Central `com.falkordb:jfalkordb` 최신 stable (현재 `0.7.0` 이며 Java 11+).
- 만약 graph-falkordb 운영 모듈이 별도 라이브러리(예: Lettuce + 직접 RAW 명령) 를 사용 중이라면, `compileOnly` 가 아닌
  `testImplementation` 으로만 jfalkordb 를 두는 옵션도 고려 (이 spec에서는 `compileOnly` 를 채택; §3.2 비교 참조).

### 2.4 R4 — `propertyNamespace` 정규식 위반 가능성

**현상**: `PropertyExportingServer` 계약상 `propertyNamespace` 는 kebab-case 소문자만 허용된다는 묵시적 규칙이 있다.
`"FalkorDB"` 또는 `"falkor-db"` 로 잘못 지정하면 다른 서버 컨벤션과 어긋난다.

**완화책**:
- `propertyNamespace = "falkordb"` (소문자, 단일 단어). 모든 키는 kebab-case.
- 테스트에서 `propertyKeys()` 가 모두 `^[a-z][a-z0-9-]*$` 정규식을 만족하는지 검증.

### 2.5 R5 — Apple Silicon 이미지 미지원

**현상**: FalkorDB 공식 이미지가 ARM64 멀티 아키 빌드를 제공하는지 확인 필요.
미지원 시 macOS 개발 환경(M-시리즈)에서 `exec format error` 발생.

**완화책**:
- spec 에 "Apple Silicon 호환성 검증" 을 DoD 항목에 포함.
- 미지원 시 `withImagePullPolicy` 또는 `--platform linux/amd64` 강제 옵션을 KDoc 에 명시.
- 현재 시점 (`v4.18.1`) 은 multi-arch (linux/amd64, linux/arm64) 지원되는 것으로 알려짐 — DoD 단계에서 실측.

---

## 3. 설계 접근법 비교

### 3.1 Approach A — 표준 패턴 그대로 (채택)

**개요**: `MemgraphServer` 를 템플릿으로 그대로 따르되, FalkorDB 의 Redis 프로토콜 특수성만 반영.

**장점**:
- 다른 모든 graphdb 컨테이너와 코드 구조가 100% 일치 → 유지보수 비용 최소.
- `PropertyExportingServer` contract test 에 자동 편입.
- `Launcher.falkordb` 싱글턴 패턴으로 다중 테스트 클래스에서 재사용 가능.

**단점**:
- `propertyKeys()` / `properties()` 의 boilerplate 가 약간 중복 (모든 graphdb 컨테이너 공통 문제).

**채택 근거**: "표준 일치" 가 본 spec 의 1차 목적. 일관성 > 약간의 중복.

### 3.2 Approach B — Lettuce 기반 raw Redis 컨테이너 위임

**개요**: `RedisServer` 를 상속/위임하여 단순히 docker image 만 `falkordb/falkordb` 로 바꾸는 방식.

**장점**:
- 코드 라인 수 최소 (~30 LOC).
- Redis 와의 동작 일관성.

**단점**:
- `propertyNamespace = "redis"` 로 충돌하거나, 별도 분리 시 결국 새 클래스 작성 필요.
- FalkorDB 만의 wait strategy / 환경 변수 (`FALKORDB_ARGS` 등) 를 표현하기 어려움.
- Type 안전성 떨어짐: 반환 타입이 `RedisServer` 라 IDE 자동완성에서 FalkorDB 임을 인지할 수 없음.

**기각 근거**: 향후 FalkorDB 전용 옵션(메모리 제한, 모듈 옵션) 추가 시 확장성 부족.
또한 `MemgraphServer`/`Neo4jServer` 계열과 묶어 graphdb 패키지에서 발견되는 편이 사용자 친화적.

### 3.3 Approach C — `org.testcontainers.containers.GenericContainer` 직접 사용 (현재 graph testFixtures 방식)

**개요**: 현재 `graph-falkordb` testFixtures 에 있는 형태를 유지하고 단지 위치만 옮김.

**장점**:
- 기존 코드 거의 그대로 이동 가능.

**단점**:
- `GenericServer` / `PropertyExportingServer` 미구현 → spec §1.3 목적 달성 실패.
- `testing/testcontainers` 모듈의 다른 서버들과 비대칭.

**기각 근거**: 본 작업의 핵심 가치(표준화) 를 충족하지 못함.

---

## 4. 구현 스펙

### 4.1 파일 목록 (변경/생성)

| 경로 | 액션 | 비고 |
|------|------|------|
| `buildSrc/src/main/kotlin/Libs.kt` | 변경 | `jfalkordb` 항목 추가 |
| `testing/testcontainers/build.gradle.kts` | 변경 | `compileOnly(Libs.jfalkordb)` 추가 (Graph DB 섹션) |
| `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/graphdb/FalkorDBServer.kt` | 신규 | 본체 |
| `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/graphdb/FalkorDBServerTest.kt` | 신규 | 테스트 |
| `testing/testcontainers/README.md` | 변경 | "Supported Servers" 표에 FalkorDB 행 추가 |
| `testing/testcontainers/README.ko.md` | 변경 | 동일 (한국어) |

### 4.2 `FalkorDBServer.kt` 전체 설계

```kotlin
package io.bluetape4k.testcontainers.graphdb

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.graphdb.FalkorDBServer.Companion.IMAGE
import io.bluetape4k.testcontainers.graphdb.FalkorDBServer.Companion.TAG
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.wait.strategy.WaitAllStrategy
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * [FalkorDB](https://www.falkordb.com/) 그래프 데이터베이스를 Testcontainers 로 실행합니다.
 *
 * FalkorDB 는 Redis 와이어 프로토콜 위에서 동작하는 OpenCypher 호환 그래프 데이터베이스입니다.
 * 따라서 Lettuce, Jedis 등 Redis 클라이언트로 접속하거나 jfalkordb 라이브러리를 사용할 수 있습니다.
 *
 * 참고: [FalkorDB Docker Hub](https://hub.docker.com/r/falkordb/falkordb)
 *
 * ```kotlin
 * val falkordb = FalkorDBServer().apply { start() }
 *
 * // jfalkordb 사용 예시 (FalkorDB 는 Jedis 기반 — driver.graph() 사용)
 * val driver = FalkorDB.driver(falkordb.host, falkordb.port)
 * val graph = driver.graph("social")
 * graph.query("CREATE (:Person {name: 'Alice'})")
 * ```
 *
 * @param imageName      Docker 이미지 이름 ([DockerImageName])
 * @param useDefaultPort 기본 포트(6379)를 호스트에 고정할지 여부 (RedisServer 와 동시 사용 불가)
 * @param reuse          컨테이너 재사용 여부
 */
class FalkorDBServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = true,
): GenericContainer<FalkorDBServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** FalkorDB 공식 Docker 이미지 이름 */
        const val IMAGE = "falkordb/falkordb"

        /** 기본으로 사용하는 FalkorDB 이미지 태그 */
        const val TAG = "v4.18.1"

        /** 시스템 프로퍼티 접두사 / propertyNamespace */
        const val NAME = "falkordb"

        /** Redis 와이어 프로토콜 기본 포트 */
        const val REDIS_PORT = 6379

        /** Wait strategy timeout — WITH_OUTER_TIMEOUT 은 글로벌 단일 cap. CI 콜드 스타트 대비 120s */
        private val START_TIMEOUT: Duration = Duration.ofSeconds(120)

        /** Redis Ready 로그 패턴 */
        private const val READY_LOG_REGEX = ".*Ready to accept connections.*"

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): FalkorDBServer = FalkorDBServer(imageName, useDefaultPort, reuse)

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): FalkorDBServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return FalkorDBServer(imageName, useDefaultPort, reuse)
        }
    }

    /** 호스트에 매핑된 Redis 포트 번호 */
    override val port: Int get() = getMappedPort(REDIS_PORT)

    /** Redis 프로토콜 접속 URL (`redis://host:port`) */
    override val url: String get() = "redis://$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "host", "port", "url"
    )

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
    )

    // redisUrl 제거 — url 과 동일값이므로 YAGNI (Jedis 는 host/port 직접 사용)

    init {
        addExposedPorts(REDIS_PORT)
        withReuse(reuse)
        // FalkorDB 컨테이너가 Redis 부팅 완료 + 포트 listening 두 조건을 모두 만족할 때까지 대기
        waitingFor(
            WaitAllStrategy(WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT)
                .withStrategy(Wait.forLogMessage(READY_LOG_REGEX, 1))
                .withStrategy(Wait.forListeningPort())
                .withStartupTimeout(START_TIMEOUT)
        )

        if (useDefaultPort) {
            exposeCustomPorts(REDIS_PORT)
        }
    }

    /**
     * FalkorDB 서버를 시작하고 시스템 프로퍼티에 연결 정보를 등록합니다.
     *
     * 등록되는 시스템 프로퍼티:
     * - `testcontainers.falkordb.host`
     * - `testcontainers.falkordb.port`
     * - `testcontainers.falkordb.url`
     * - `testcontainers.falkordb.redis-port`
     * - `testcontainers.falkordb.redis-url`
     */
    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 FalkorDB 서버 싱글턴.
     *
     * ```kotlin
     * val falkordb = FalkorDBServer.Launcher.falkordb
     * val client = RedisClient.create(falkordb.redisUrl)
     * ```
     */
    object Launcher {
        /**
         * 기본 설정으로 시작된 [FalkorDBServer] 싱글턴.
         * JVM 종료 시 자동으로 정지됩니다.
         */
        val falkordb: FalkorDBServer by lazy {
            FalkorDBServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
```

### 4.3 `FalkorDBServerTest.kt` 전체 설계

```kotlin
package io.bluetape4k.testcontainers.graphdb

import com.falkordb.FalkorDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FalkorDBServerTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val GRAPH_NAME = "social"
    }

    private val falkordb = FalkorDBServer.Launcher.falkordb

    @Test
    fun `FalkorDB 서버가 실행 중이어야 한다`() {
        falkordb.isRunning.shouldBeTrue()
    }

    @Test
    fun `Redis 포트가 0보다 커야 한다`() {
        falkordb.port shouldBeGreaterThan 0
    }

    @Test
    fun `url 은 redis 스킴을 사용해야 한다`() {
        falkordb.url shouldContain "redis://"
    }

    @Test
    fun `propertyNamespace 는 falkordb 이어야 한다`() {
        falkordb.propertyNamespace shouldBeEqualTo "falkordb"
    }

    @Test
    fun `propertyKeys 는 host port url 을 포함해야 한다`() {
        val keys = falkordb.propertyKeys()
        keys shouldContain "host"
        keys shouldContain "port"
        keys shouldContain "url"
    }

    @Test
    fun `properties 는 모든 키에 비어있지 않은 값을 반환해야 한다`() {
        val props = falkordb.properties()
        falkordb.propertyKeys().forEach { key ->
            props[key].shouldNotBeNull()
            (props[key]!!.isNotBlank()).shouldBeTrue()
        }
    }

    @Test
    fun `시스템 프로퍼티에 host port url 이 등록되어야 한다`() {
        val host = System.getProperty("testcontainers.falkordb.host")
        val port = System.getProperty("testcontainers.falkordb.port")
        val url = System.getProperty("testcontainers.falkordb.url")
        log.debug { "host=$host, port=$port, url=$url" }
        host.shouldNotBeNull()
        port.shouldNotBeNull()
        url.shouldNotBeNull()
    }

    @Test
    fun `jfalkordb 클라이언트로 그래프 쿼리를 실행할 수 있어야 한다`() {
        // Driver, Graph 모두 Closeable → .use {} 중첩 사용
        // ResultSet 은 Iterable<Record> → toList() 사용
        // Record.getString(key) 존재 — getValue(key) 도 가능
        FalkorDB.driver(falkordb.host, falkordb.port).use { driver ->
            driver.graph(GRAPH_NAME).use { graph ->
                try {
                    graph.query("CREATE (:Person {name: 'Alice'})")
                    val rows = graph.query("MATCH (p:Person) RETURN p.name AS name").toList()
                    rows.shouldNotBeEmpty()
                    rows.first().getString("name") shouldBeEqualTo "Alice"
                } finally {
                    runCatching { graph.deleteGraph() }
                }
            }
        }
    }

    @Test
    fun `blank image 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { FalkorDBServer(image = " ") }
    }

    @Test
    fun `blank tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { FalkorDBServer(tag = " ") }
    }
}
```

### 4.4 `Libs.kt` 변경

`buildSrc/src/main/kotlin/Libs.kt` 의 Neo4j 항목 아래 Graph DB 섹션에 추가:

```kotlin
// Neo4j
const val neo4j_java_driver = "org.neo4j.driver:neo4j-java-driver:5.28.4"

// FalkorDB
const val jfalkordb = "com.falkordb:jfalkordb:0.7.0"  // https://mvnrepository.com/artifact/com.falkordb/jfalkordb
```

> **검증 포인트**: 위 좌표가 Maven Central 에 실재하는지 본 spec 승인 후 plan 단계에서 재확인한다.
> 만약 `0.7.0` 이 publish 되지 않았다면 `0.6.0` 또는 최신 stable 로 조정한다.

### 4.5 `build.gradle.kts` 변경

`testing/testcontainers/build.gradle.kts` Graph DB 섹션 (Neo4j 다음 줄)에 추가:

```kotlin
// Graph DB (Neo4j)
compileOnly(Libs.testcontainers_neo4j)
compileOnly(Libs.neo4j_java_driver)

// Graph DB (FalkorDB - Redis protocol)
compileOnly(Libs.jfalkordb)
```

`testImplementation` 은 명시할 필요 없음 — 파일 상단 `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }` 에서 자동 처리됨.

### 4.6 README 변경

`testing/testcontainers/README.md` 와 `README.ko.md` 양쪽에서:

1. "Supported Servers" 또는 "지원하는 서버" 표의 Graph DB 영역에 `FalkorDB` 행 추가.
2. Mermaid 다이어그램이 graphdb 패키지를 표시한다면 `FalkorDBServer` 노드 추가.
3. 코드 예시 섹션에 `FalkorDBServer.Launcher.falkordb` 사용 스니펫 1개 추가.

---

## 5. 완료 기준 (DoD)

### 5.1 코드/빌드

- [ ] `buildSrc/src/main/kotlin/Libs.kt` 에 `Libs.jfalkordb` 정의 추가 (Maven Central 좌표 실재 검증 완료)
- [ ] `testing/testcontainers/build.gradle.kts` 에 `compileOnly(Libs.jfalkordb)` 추가
- [ ] `FalkorDBServer.kt` 신규 작성 (KDoc 포함, 200 라인 이내)
- [ ] `FalkorDBServerTest.kt` 신규 작성 (10개 이상 테스트 케이스)
- [ ] `./gradlew :bluetape4k-testcontainers:compileKotlin` 성공
- [ ] `./gradlew :bluetape4k-testcontainers:compileTestKotlin` 성공

### 5.2 테스트

- [ ] `./gradlew :bluetape4k-testcontainers:test --tests "*FalkorDBServerTest*"` 모든 케이스 PASS
- [ ] 시스템 프로퍼티 3개 (`host`, `port`, `url`) 가 모두 등록되는지 검증
- [ ] jfalkordb 클라이언트로 CREATE → MATCH 쿼리 라운드트립 성공
- [ ] `MemgraphServerTest` 와 동일한 방식으로 `Launcher` 싱글턴 재사용 검증

### 5.3 정합성

- [ ] `propertyNamespace = "falkordb"` (kebab-case 단일 단어) — 정규식 `^[a-z][a-z0-9-]*$` 통과
- [ ] 모든 propertyKey 가 동일 정규식 통과 (`host`, `port`, `url`)
- [ ] `MemgraphServer` / `Neo4jServer` 와 동일한 클래스 구조 (private ctor + companion `invoke` overload + `Launcher` object)
- [ ] `useDefaultPort = false` (default) 인 경우 호스트 포트 6379 점유하지 않음 (Redis 충돌 회피)

### 5.4 문서

- [ ] `testing/testcontainers/README.md` 갱신 (Supported Servers 표 + 예시)
- [ ] `testing/testcontainers/README.ko.md` 갱신 (동일 내용 한국어)
- [ ] FalkorDBServer 의 모든 public 멤버에 KDoc (Korean OK) 작성
- [ ] FalkorDB 가 RedisServer 와 호스트 포트 충돌할 수 있다는 경고 KDoc 명시

### 5.5 호환성

- [ ] Apple Silicon (M-series) 에서 `falkordb/falkordb:v4.18.1` 이미지 정상 기동 확인
  - 미지원 시 `--platform linux/amd64` 옵션 추가 + KDoc 갱신
- [ ] 기존 `bluetape4k-graph/graph/graph-falkordb` testFixtures 의 `FalkorDBServer` 와 클래스명/패키지가 다르므로 이름 충돌 없음 확인

### 5.6 PR 전 체크리스트

- [ ] `oh-my-claudecode:code-reviewer` 실행 → HIGH/CRITICAL 이슈 0건
- [ ] worktree `.worktrees/feat-falkordb-testcontainers` 에서 모든 작업 수행
- [ ] commit 메시지 한국어 + `feat:` prefix (예: `feat: FalkorDBServer testcontainers 추가`)
- [ ] PR 본문에 테스트 결과 (passing count + duration) 포함
- [ ] `docs/superpowers/index/2026-04.md` 에 entry 추가 + `INDEX.md` count 업데이트

---

## 6. 태스크 목록 (Step 2 plan 의 시드)

| # | 태스크 | 파일/위치 | 의존 | 추정 |
|---|--------|-----------|------|------|
| T1 | `Libs.jfalkordb` 좌표 검증 + 추가 | `buildSrc/src/main/kotlin/Libs.kt` | — | 5분 |
| T2 | `compileOnly(Libs.jfalkordb)` 추가 | `testing/testcontainers/build.gradle.kts` | T1 | 5분 |
| T3 | `FalkorDBServer.kt` 작성 | `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/graphdb/FalkorDBServer.kt` | T2 | 30분 |
| T4 | `FalkorDBServerTest.kt` 작성 | `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/graphdb/FalkorDBServerTest.kt` | T3 | 30분 |
| T5 | 컴파일 + 테스트 실행 | `./gradlew :bluetape4k-testcontainers:test --tests "*FalkorDBServerTest*"` | T3, T4 | 5~10분 |
| T6 | Apple Silicon 이미지 호환성 검증 | docker 실행 | T5 | 5분 |
| T7 | `README.md` / `README.ko.md` 갱신 | `testing/testcontainers/README.md`, `README.ko.md` | T5 | 15분 |
| T8 | `oh-my-claudecode:code-reviewer` 실행 + 이슈 처리 | — | T7 | 15분 |
| T9 | `docs/superpowers/index/2026-04.md` entry 추가 + INDEX 갱신 | `docs/superpowers/` | T8 | 5분 |
| T10 | commit + PR 생성 | git | T9 | 10분 |

---

## 7. 참고 파일 (절대 경로)

- `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/graphdb/MemgraphServer.kt`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/PropertyExportingServer.kt`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/GenericServer.kt`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/graphdb/MemgraphServerTest.kt`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/testing/testcontainers/build.gradle.kts`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-falkordb-testcontainers/buildSrc/src/main/kotlin/Libs.kt`
