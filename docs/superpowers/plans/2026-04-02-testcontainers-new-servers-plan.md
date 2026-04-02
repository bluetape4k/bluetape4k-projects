# Testcontainers 신규 Server 추가 구현 플랜

**날짜**: 2026-04-02
**대상 모듈**: `testing/testcontainers` (`bluetape4k-testcontainers`)
**설계 문서**: `docs/superpowers/specs/2026-04-02-testcontainers-new-servers-design.md`
**브랜치**: `develop`

---

## Context

`bluetape4k-testcontainers` 모듈에 8개 신규 Server 클래스를 추가한다.
- Graph DB: Neo4j, Memgraph, PostgreSQLAge
- Infra: Toxiproxy, Keycloak
- Database: Trino
- HTTP: WireMock
- Storage: InfluxDB

모든 신규 서버는 기존 `VaultServer`, `RabbitMQServer` 등과 동일한 canonical 패턴을 따른다:
- `private constructor` + `companion object`에 `operator fun invoke` 팩토리
- `GenericServer` (또는 `JdbcServer`) 구현
- `writeToSystemProperties()` 호출
- `object Launcher` lazy singleton + `ShutdownQueue.register()`

---

## Work Objectives

1. `Libs.kt` / `build.gradle.kts`에 신규 의존성 상수 추가
2. `graphdb/` 패키지 신설 후 Neo4j / Memgraph / PostgreSQLAge Server 구현
3. `infra/`, `database/`, `http/`, `storage/` 패키지에 Toxiproxy / Trino / WireMock / Keycloak / InfluxDB Server 구현
4. 각 Server에 대한 테스트 작성
5. 기존 서버 일관성 리팩토링 (KDoc, 오탈자)
6. README 업데이트 및 빌드 검증

---

## Guardrails

### Must Have
- 모든 신규 Server는 canonical 패턴 (VaultServer 참조) 준수
- 모든 Server에 한국어 KDoc 작성
- 테스트 기본: `Launcher.xxx` singleton 사용 + `isRunning`, 포트, 시스템 프로퍼티 검증
- **예외 — mutable 상태 서버는 per-test 또는 per-class 컨테이너 직접 생성 허용**:
  - `ToxiproxyServer` — toxic 상태 누수 방지를 위해 테스트별 proxy 초기화 또는 직접 생성
  - `WireMockServer` — stub 상태 누수 방지를 위해 각 테스트 후 `resetAll()` 호출 또는 직접 생성
  - `KeycloakServer` — realm/user 변경이 없는 read-only 검증만 할 경우 singleton 허용
- PostgreSQLAge의 Kotlin `$` 이스케이프 처리 (`"${'$'}user"`)
- InfluxDB는 2.x 모델 (org/bucket/adminToken) 사용

### Must NOT Have
- 아키텍처 변경 없음 (기존 `GenericServer`/`JdbcServer` 인터페이스 수정 금지)
- 기존 서버 클래스의 동작 변경 없음 (KDoc/오탈자만 수정)
- `testcontaiiners_nginx` 오탈자 수정은 참조하는 모든 `build.gradle.kts` 동시 수정 필요

---

## Task Flow

```
Phase 1 (의존성)  -->  Phase 2 (Graph DB)  -->  Phase 3 (Infra/DB)  -->  Phase 4 (리팩토링)
    [T1, T2]         [T3~T8]                  [T9~T18]                [T19~T21]
```

Phase 2, Phase 3 내부 태스크들은 서로 독립적이므로 병렬 실행 가능.

---

## Detailed TODOs

### Phase 1: 사전 준비 (의존성)

#### T1. `buildSrc/Libs.kt`에 신규 상수 추가
- **complexity**: low
- **파일**: `buildSrc/src/main/kotlin/Libs.kt`
- **작업 내용**:
  - Testcontainers 섹션(`:1358` 이후)에 추가:
    - `val testcontainers_neo4j = testcontainersModule("neo4j")`
    - `val testcontainers_trino = testcontainersModule("trino")`
    - `val testcontainers_toxiproxy = testcontainersModule("toxiproxy")`
  - Graph DB 섹션 신설:
    - `const val neo4j_java_driver = "org.neo4j.driver:neo4j-java-driver:5.28.4"`
  - Distributed SQL 섹션:
    - `const val trino_jdbc = "io.trino:trino-jdbc:475"`
  - WireMock Testcontainers:
    - `const val wiremock_testcontainers = "org.wiremock:wiremock-testcontainers-module:1.0-alpha-15"`
  - Keycloak Testcontainers:
    - `const val keycloak_testcontainers = "com.github.dasniko:testcontainers-keycloak:3.7.0"`
- **acceptance criteria**:
  - `./gradlew :bluetape4k-testcontainers:dependencies` 실행 시 resolve 오류 없음
  - 기존 `testcontainers_influxdb`(`:1373`), `influxdb_java`(`:1033`), `wiremock`(`:1434`)은 변경하지 않음

#### T2. `build.gradle.kts`에 신규 의존성 추가
- **complexity**: low
- **파일**: `testing/testcontainers/build.gradle.kts`
- **작업 내용**: 설계 문서 섹션 6 참조. 아래 의존성 추가:
  ```
  compileOnly(Libs.testcontainers_neo4j)
  compileOnly(Libs.neo4j_java_driver)
  compileOnly(Libs.testcontainers_toxiproxy)
  compileOnly(Libs.testcontainers_trino)
  testRuntimeOnly(Libs.trino_jdbc)
  compileOnly(Libs.wiremock_testcontainers)
  compileOnly(Libs.keycloak_testcontainers)
  compileOnly(Libs.testcontainers_influxdb)
  // influxdb_java(1.x)는 제거 — InfluxDB 2.x HTTP API는 TC 컨테이너 헬퍼 + java.net.http.HttpClient 사용
  // 2.x 전용 클라이언트가 필요하면 com.influxdb:influxdb-client-java 별도 추가
  ```
- **acceptance criteria**:
  - `./gradlew :bluetape4k-testcontainers:compileKotlin` 성공

---

### Phase 2: Graph DB 서버

#### T3. `Neo4jServer` 구현
- **complexity**: medium
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/graphdb/Neo4jServer.kt`
- **작업 내용**:
  - `Neo4jContainer` 상속, `GenericServer` 구현
  - 포트: `7474` (HTTP), `7687` (Bolt)
  - 기본값: `withoutAuthentication()`
  - `override val port` = `getMappedPort(BOLT_PORT)`
  - `override val url` = `"bolt://$host:$port"`
  - `boltUrl`, `httpUrl` 프로퍼티 추가
  - `writeToSystemProperties("neo4j", ...)` — bolt.port, http.port, bolt.url, http.url
  - **`invoke` 팩토리에 `image.requireNotBlank("image")` / `tag.requireNotBlank("tag")` 호출 필수**
- **acceptance criteria**:
  - canonical 패턴 준수 (private constructor, invoke 팩토리, Launcher)
  - 한국어 KDoc 포함

#### T4. `Neo4jServerTest` 작성
- **complexity**: medium
- **파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/graphdb/Neo4jServerTest.kt`
- **작업 내용**:
  - `Launcher.neo4j` 사용
  - `isRunning`, 포트, 시스템 프로퍼티 검증
  - Neo4j Java Driver로 Bolt 연결: `Driver.verifyConnectivity()` + `session.run("RETURN 1")` 결과 확인
- **acceptance criteria**:
  - 테스트 통과 (Docker 환경 필요)

#### T5. `MemgraphServer` 구현
- **complexity**: medium
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/graphdb/MemgraphServer.kt`
- **작업 내용**:
  - `GenericContainer<MemgraphServer>` 상속, `GenericServer` 구현
  - 이미지: `memgraph/memgraph:3.2.1`
  - 포트: `7687` (Bolt), `7444` (Log)
  - `addEnv("MEMGRAPH", "--telemetry-enabled=false")`
  - `override val url` = `"bolt://$host:$port"`
  - `writeToSystemProperties("memgraph", ...)` — bolt.port, log.port, bolt.url
  - **`invoke` 팩토리에 `image.requireNotBlank("image")` / `tag.requireNotBlank("tag")` 호출 필수**
- **acceptance criteria**:
  - canonical 패턴 준수, 한국어 KDoc

#### T6. `MemgraphServerTest` 작성
- **complexity**: medium
- **파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/graphdb/MemgraphServerTest.kt`
- **작업 내용**:
  - `Launcher.memgraph` 사용
  - Bolt 연결 성공 검증
  - `CALL mg.procedures() YIELD *` 프로시저 조회 성공 확인
- **acceptance criteria**: 테스트 통과

#### T7. `PostgreSQLAgeServer` 구현
- **complexity**: high
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/graphdb/PostgreSQLAgeServer.kt`
- **작업 내용**:
  - `PostgreSQLContainer<PostgreSQLAgeServer>` 상속, `JdbcServer` 구현
  - 이미지: `apache/age:PG17_latest`, `asCompatibleSubstituteFor("postgres")`
  - DB 기본값: database=test, username=test, password=test
  - `start()` 내 AGE extension 초기화:
    - `CREATE EXTENSION IF NOT EXISTS age`
    - `LOAD 'age'`
    - `SET search_path = ag_catalog, "${'$'}user", public` (Kotlin $ 이스케이프 주의)
  - `writeToSystemProperties("postgresql-age", ...)` — jdbc.url, username, password, database
  - **`invoke` 팩토리에 `image.requireNotBlank("image")` / `tag.requireNotBlank("tag")` 호출 필수**
  - **AGE 초기화 SQL 실패 시**: 예외를 그대로 전파하여 컨테이너 시작 실패로 처리. 단, 각 SQL 단계 전에 `log.info { "AGE 초기화: $sql 실행 중..." }` 로깅을 남겨 어떤 단계에서 실패했는지 추적 가능하게 할 것
- **acceptance criteria**:
  - AGE extension 초기화 SQL 정상 실행
  - `JdbcServer` 인터페이스 메서드 구현 (getDriverClassName, getJdbcUrl 등)
  - Kotlin `$` 이스케이프 올바르게 처리
  - AGE 초기화 실패 시 `start()` 예외 전파 확인

#### T8. `PostgreSQLAgeServerTest` 작성
- **complexity**: medium
- **파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/graphdb/PostgreSQLAgeServerTest.kt`
- **작업 내용**:
  - `Launcher.postgresqlAge` 사용
  - `SELECT * FROM ag_catalog.ag_graph` 쿼리 성공 확인
  - AGE 그래프 생성/조회 테스트
- **acceptance criteria**: 테스트 통과

---

### Phase 3: 신규 인프라/DB 서버

#### T9. `ToxiproxyServer` 구현
- **complexity**: medium
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/ToxiproxyServer.kt`
- **작업 내용**:
  - `ToxiproxyContainer` 상속, `GenericServer` 구현
  - 이미지: `ghcr.io/shopify/toxiproxy:2.9.0`
  - 포트: `8474` (Control API)
  - `Launcher`에 `createProxy(name, upstream)` 헬퍼 추가
  - `writeToSystemProperties("toxiproxy", ...)` — control.port, control.url
  - **`invoke` 팩토리에 `image.requireNotBlank("image")` / `tag.requireNotBlank("tag")` 호출 필수**
- **acceptance criteria**: canonical 패턴 준수, 한국어 KDoc

#### T10. `ToxiproxyServerTest` 작성
- **complexity**: medium
- **파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/infra/ToxiproxyServerTest.kt`
- **작업 내용**:
  - **Singleton 금지** — toxic 상태 누수 방지를 위해 테스트 클래스에서 `ToxiproxyServer()`를 직접 생성
  - upstream 서비스: `HttpbinServer.Launcher.httpbin` 재사용 (이미 모듈에 존재)
  - Toxiproxy → Httpbin 연결 proxy 생성:
    ```kotlin
    val proxy = toxiproxy.getProxy("httpbin", "httpbin:80")
    ```
  - 네트워크 구성: Toxiproxy와 Httpbin을 동일 Docker 네트워크에 연결 (`withNetwork(network)`)
  - 검증 시나리오:
    1. proxy 통해 Httpbin GET 요청 → 200 정상 응답
    2. `LATENCY` toxic 추가 (latency=500ms) → 응답시간 500ms 이상 확인
    3. toxic 제거 → 응답시간 정상 복구
- **acceptance criteria**: 3단계 검증 모두 통과

#### T11. `TrinoServer` 구현
- **complexity**: low
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/database/TrinoServer.kt`
- **작업 내용**:
  - `TrinoContainer` 상속, `JdbcServer` 구현
  - 이미지: `trinodb/trino:475`
  - 포트: `8080`
  - JDBC URL: `jdbc:trino://$host:$port/memory`
  - `writeToSystemProperties("trino", ...)` — jdbc.url, username
  - **`invoke` 팩토리에 `image.requireNotBlank("image")` / `tag.requireNotBlank("tag")` 호출 필수**
- **acceptance criteria**: canonical 패턴 준수, `JdbcServer` 인터페이스 완전 구현

#### T12. `TrinoServerTest` 작성
- **complexity**: low
- **파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/database/TrinoServerTest.kt`
- **작업 내용**:
  - `SELECT 1` JDBC 쿼리 성공
  - `information_schema.tables` 조회 확인
- **acceptance criteria**: 테스트 통과

#### T13. `WireMockServer` 구현
- **complexity**: medium
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/http/WireMockServer.kt`
- **작업 내용**:
  - `WireMockContainer` 상속, `GenericServer` 구현
  - 이미지: `wiremock/wiremock:3.13.2`
  - 포트: `8080` (HTTP), `8443` (HTTPS)
  - `httpsUrl` 추가 프로퍼티
  - `writeToSystemProperties("wiremock", ...)` — http.port, https.port, base.url
  - **`invoke` 팩토리에 `image.requireNotBlank("image")` / `tag.requireNotBlank("tag")` 호출 필수**
- **acceptance criteria**: canonical 패턴 준수, 한국어 KDoc

#### T14. `WireMockServerTest` 작성
- **complexity**: medium
- **파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/http/WireMockServerTest.kt`
- **작업 내용**:
  - stub 등록 후 HTTP GET 요청 -> 기대 응답 반환 확인
- **acceptance criteria**: 테스트 통과

#### T15. `KeycloakServer` 구현
- **complexity**: medium
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/KeycloakServer.kt`
- **작업 내용**:
  - `KeycloakContainer` 상속 (dasniko), `GenericServer` 구현
  - 이미지: `quay.io/keycloak/keycloak:26.2`
  - 포트: `8080`
  - `authServerUrl` 프로퍼티 (기본 context path = `/`)
  - `writeToSystemProperties("keycloak", ...)` — auth.url, admin.username, admin.password
  - **`invoke` 팩토리에 `image.requireNotBlank("image")` / `tag.requireNotBlank("tag")` 호출 필수**
- **acceptance criteria**:
  - context path 주의: 기본 `/` (not `/auth`)
  - canonical 패턴 준수

#### T16. `KeycloakServerTest` 작성
- **complexity**: medium
- **파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/infra/KeycloakServerTest.kt`
- **작업 내용**:
  1. Admin access token 획득:
     ```
     POST /realms/master/protocol/openid-connect/token
     grant_type=password, username=admin, password=admin, client_id=admin-cli
     ```
  2. 획득한 Bearer token으로 `GET /admin/realms` 호출 → 200 응답 + master realm 존재 확인
  - OkHttp 또는 `java.net.http.HttpClient` 사용 (이미 testImplementation 있음)
- **acceptance criteria**:
  - admin token 획득 성공
  - `GET /admin/realms` 200 + master realm JSON 포함 확인

#### T17. `InfluxDBServer` 구현
- **complexity**: low
- **파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/InfluxDBServer.kt`
- **작업 내용**:
  - `InfluxDBContainer` 상속, `GenericServer` 구현
  - 이미지: `influxdb:2.7`
  - 포트: `8086`
  - 2.x 모델: organization, bucket, adminToken, username, password
  - `writeToSystemProperties("influxdb", ...)` — organization, bucket, admin.token, username
  - **`invoke` 팩토리에 `image.requireNotBlank("image")` / `tag.requireNotBlank("tag")` 호출 필수**
- **acceptance criteria**: canonical 패턴 준수, 한국어 KDoc

#### T18. `InfluxDBServerTest` 작성
- **complexity**: low
- **파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/storage/InfluxDBServerTest.kt`
- **작업 내용**:
  - `InfluxDBContainer.getAdminToken()` / `getBucket()` 헬퍼 동작 확인
  - HTTP API로 포인트 write 후 query 조회
- **acceptance criteria**: 테스트 통과

---

### Phase 4: 리팩토링 & 마무리

#### T19. 기존 서버 일관성 리팩토링
- **complexity**: medium
- **파일**: 기존 Server 클래스 전반
- **작업 내용**:
  - KDoc 누락 서버에 한국어 KDoc 추가
  - `writeToSystemProperties` extraProps 키 이름 규칙 점검 (dot-separated lowercase 통일)
  - `Libs.kt:1378` `testcontaiiners_nginx` (double 'i') 오탈자 → **삭제** (`:1400`의 `testcontainers_nginx`가 정상 상수이므로 중복 제거)
    - 주의: `rg "testcontaiiners_nginx"` 로 참조 검색 후 참조 파일도 `testcontainers_nginx`로 교체
- **acceptance criteria**:
  - 오탈자 수정 후 전체 빌드 성공
  - 기존 서버 동작 변경 없음

#### T20. `testing/testcontainers/README.md` 업데이트
- **complexity**: low
- **파일**: `testing/testcontainers/README.md`
- **작업 내용**:
  - Graph DB / HTTP Mock / Auth / 시계열 DB / 카오스 테스트 / 분산 SQL 섹션 추가
  - 전체 서버 목록 테이블 갱신
  - 각 섹션에 Launcher 사용 예시 코드 포함
- **acceptance criteria**: 모든 신규 서버가 README에 문서화됨

#### T21. 빌드 및 테스트 검증
- **complexity**: low
- **작업 내용**:
  1. 컴파일: `./gradlew :bluetape4k-testcontainers:compileKotlin :bluetape4k-testcontainers:compileTestKotlin`
  2. detekt: `./gradlew :bluetape4k-testcontainers:detekt`
  3. **신규 서버 테스트 실행** (Docker 환경 필요):
     ```
     ./gradlew :bluetape4k-testcontainers:test \
       --tests "*.graphdb.*" \
       --tests "*.infra.ToxiproxyServerTest" \
       --tests "*.infra.KeycloakServerTest" \
       --tests "*.database.TrinoServerTest" \
       --tests "*.http.WireMockServerTest" \
       --tests "*.storage.InfluxDBServerTest"
     ```
- **acceptance criteria**:
  - 컴파일 + detekt 통과
  - 신규 8개 Server 테스트 전체 통과 (컨테이너 기동 + 연결 검증 포함)

---

## Success Criteria

1. 8개 신규 Server 클래스 구현 완료 (canonical 패턴 100% 준수)
2. 8개 테스트 클래스 작성 완료 (각 서버별 고유 검증 시나리오 포함)
3. `Libs.kt` 신규 상수 5개 + `build.gradle.kts` 의존성 9개 추가
4. `graphdb/` 패키지 신설 (3개 서버)
5. 기존 서버 KDoc/오탈자 정리 완료
6. README 갱신 완료
7. `compileKotlin` + `compileTestKotlin` 성공

---

## Execution Notes

- Phase 2/3 내부 태스크는 독립적이므로 병렬 에이전트 디스패치 가능
- Docker 필요 태스크(테스트)는 로컬 Docker 데몬 실행 전제
- `testcontaiiners_nginx` 오탈자 수정 시 `rg "testcontaiiners_nginx"` 로 전체 참조 검색 후 일괄 수정
- PostgreSQLAgeServer의 AGE extension 초기화는 `start()` 내에서 `createConnection("")`을 사용하므로, 컨테이너가 완전히 시작된 후 실행됨
