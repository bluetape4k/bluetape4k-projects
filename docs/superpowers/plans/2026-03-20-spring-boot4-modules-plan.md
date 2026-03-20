# Spring Boot 4 모듈 구현 계획

> 작성일: 2026-03-20
> Spec: `docs/superpowers/specs/2026-03-20-spring-boot4-modules-design.md`
> 상태: Ready

## 구현 순서 요약

```
Phase 1: 기반 설정 (T01~T02)
Phase 2: core 모듈 구현 (T03~T09) — 병렬 가능 그룹 포함
Phase 3: 하위 모듈 구현 (T10~T15) — 병렬 가능
Phase 4: 검증 및 문서화 (T16~T17)
```

---

## Phase 1: 기반 설정

### T01. Libs.kt에 springBoot4Starter 헬퍼 함수 추가
- **complexity**: low
- **depends_on**: 없음
- **설명**: `Libs.kt`에 `springBoot4Starter()` 함수 추가. `spring_boot4_dependencies` BOM 좌표는 이미 존재.
- **작업 내용**:
  - `fun springBoot4Starter(module: String) = "org.springframework.boot:spring-boot-starter-$module:${Versions.spring_boot4}"` 추가
  - 필요 시 `fun springBoot4(module: String)` 헬퍼도 추가
- **파일**: `buildSrc/src/main/kotlin/Libs.kt`
- **완료 기준**: 빌드 오류 없이 Libs에서 참조 가능

### T02. spring-boot4 모듈 디렉토리 구조 생성
- **complexity**: low
- **depends_on**: 없음
- **설명**: 6개 서브모듈 디렉토리 + `build.gradle.kts` 스켈레톤 생성. `settings.gradle.kts`는 이미 `includeModules("spring-boot4", withBaseDir = true)` 선언됨.
- **작업 내용**:
  - `spring-boot4/.gitkeep` 삭제 (이미 존재)
  - 각 모듈 디렉토리에 `src/main/kotlin`, `src/test/kotlin` 구조 생성
  - 각 모듈에 빈 `build.gradle.kts` 생성 (Phase 2~3에서 내용 채움)
- **파일**: `spring-boot4/{core,data-redis,r2dbc,mongodb,cassandra,cassandra-demo}/`
- **완료 기준**: `./gradlew projects` 에서 6개 모듈 인식

> **T01, T02는 병렬 실행 가능**

---

## Phase 2: core 모듈 구현

### T03. core/build.gradle.kts 작성
- **complexity**: high
- **depends_on**: T01, T02
- **설명**: Boot 4 BOM 오버라이드 + Jackson 3 전환이 핵심. boot3/core의 의존성 구조를 기반으로 하되, 다음 변경 적용:
  - `dependencyManagement { imports { mavenBom(Libs.spring_boot4_dependencies) } }` 추가 (루트 Boot 3 BOM 오버라이드)
  - `bluetape4k-jackson2` → `bluetape4k-jackson3` 전환
  - `jackson_*` → `jackson3_*` 의존성 전환
  - `Libs.springBootStarter()` 호출은 그대로 사용 (BOM 오버라이드로 4.x 버전 해소)
- **파일**: `spring-boot4/core/build.gradle.kts`
- **주요 결정사항**:
  - Retrofit2, OkHttp3, AsyncHttpClient 의존성은 boot3과 동일하게 유지 (호환성 확인은 T09에서)
  - `spring-boot-starter-test` 4.x의 JUnit 5 + MockK 호환성은 T09에서 검증
- **완료 기준**: `./gradlew :bluetape4k-spring-boot4-core:dependencies` 에서 Spring Framework 7.x 해소 확인

### T04. core — boot3 기존 패키지 포팅 (beans, config, core, data, messaging, ui, util)
- **complexity**: medium
- **depends_on**: T03
- **설명**: boot3/core에서 API 변경 없는 7개 패키지를 복사 후 패키지명 변경.
- **작업 내용**:
  - `io.bluetape4k.spring.*` → `io.bluetape4k.spring4.*` 패키지 리네임
  - import 문의 `io.bluetape4k.spring.` → `io.bluetape4k.spring4.` 일괄 치환
  - 각 패키지 파일 수: beans(4), config(1), core(3), data(1), messaging(1), ui(2), util(2) = **14개 파일**
- **파일**: `spring-boot4/core/src/main/kotlin/io/bluetape4k/spring4/{beans,config,core,data,messaging,ui,util}/`
- **완료 기준**: 컴파일 성공

### T05. core — jackson 패키지 포팅 (Jackson 3 전환)
- **complexity**: high
- **depends_on**: T03
- **설명**: `jackson` 패키지의 ObjectMapperBuilder 커스터마이저를 Jackson 3.x API로 전환.
- **작업 내용**:
  - `io.bluetape4k.spring.jackson` → `io.bluetape4k.spring4.jackson` 리네임
  - Jackson 2.x import (`com.fasterxml.jackson.*`) → Jackson 3.x import (`tools.jackson.*`) 전환
  - `ObjectMapper` → Jackson 3.x `ObjectMapper` API 변경 적용
  - `bluetape4k-jackson3` 모듈의 유틸리티 함수 활용
- **파일**: `spring-boot4/core/src/main/kotlin/io/bluetape4k/spring4/jackson/` (1개 파일)
- **완료 기준**: Jackson 3.x ObjectMapper 기반으로 컴파일 성공

### T06. core — rest, webflux 패키지 포팅
- **complexity**: medium
- **depends_on**: T03
- **설명**: REST 에러 응답 + WebFlux 관련 코드 포팅.
- **작업 내용**:
  - `rest` 패키지(3개 파일): `ProblemDetail` API 변경 확인, 패키지 리네임
  - `webflux` 패키지(7개 파일): WebClient 확장, Controller, Filter 포팅, 패키지 리네임
  - Spring Framework 7.x의 WebFlux API 변경사항 반영
- **파일**: `spring-boot4/core/src/main/kotlin/io/bluetape4k/spring4/{rest,webflux}/`
- **완료 기준**: 컴파일 성공

### T07. core — retrofit2, tests 패키지 포팅
- **complexity**: medium
- **depends_on**: T03
- **설명**: Retrofit2 자동 구성 + 테스트 지원 코드 포팅.
- **작업 내용**:
  - `retrofit2` 패키지(8개 파일): 패키지 리네임, Spring Framework 7 호환성 확인
  - `tests` 패키지(3개 파일): WebTestClient 확장, HTTP 클라이언트 테스트 DSL 포팅
  - RestTemplate 참조가 있으면 제거 (Boot 4에서 삭제됨)
- **파일**: `spring-boot4/core/src/main/kotlin/io/bluetape4k/spring4/{retrofit2,tests}/`
- **완료 기준**: 컴파일 성공

### T08. core — 신규 http 패키지 (RestClient Coroutines DSL)
- **complexity**: high
- **depends_on**: T03
- **설명**: Spring Boot 4 전용 신규 기능. RestClient 기반 Coroutines suspend DSL 설계 및 구현.
- **작업 내용**:
  - `RestClientCoroutinesDsl.kt`: `suspendGet<T>()`, `suspendPost<T>()`, `suspendPut<T>()`, `suspendPatch<T>()`, `suspendDelete()` 확장 함수
  - `RestClientBuilderDsl.kt`: `restClientOf(baseUrl) { ... }` DSL 빌더
  - `RestClientExtensions.kt`: `httpGet()`, `httpPost()`, `httpPut()`, `httpPatch()`, `httpDelete()` 동기 확장 (boot3 `tests` 패키지의 RestClient 확장과 유사하되 독립)
  - 모든 suspend 함수는 `withContext(Dispatchers.IO)` 래핑
- **파일**: `spring-boot4/core/src/main/kotlin/io/bluetape4k/spring4/http/`
- **완료 기준**: 컴파일 성공 + 단위 테스트 통과

### T08-1. core — 신규 virtualthread 패키지
- **complexity**: medium
- **depends_on**: T03
- **설명**: Spring Boot 4 전용 Virtual Thread 통합.
- **작업 내용**:
  - `VirtualThreadAutoConfiguration.kt`: `SimpleAsyncTaskExecutor` + `setVirtualThreads(true)` 자동 구성
  - `AbstractVirtualThreadController.kt`: VT Executor 기반 컨트롤러 베이스
  - `@Configuration` + `@ConditionalOnMissingBean` 패턴 사용
- **파일**: `spring-boot4/core/src/main/kotlin/io/bluetape4k/spring4/virtualthread/`
- **완료 기준**: 컴파일 성공

### T09. core — 테스트 작성 및 통합 검증
- **complexity**: high
- **depends_on**: T04, T05, T06, T07, T08, T08-1
- **설명**: boot3/core 테스트를 포팅하고, 신규 기능(http, virtualthread) 테스트 추가.
- **작업 내용**:
  - boot3/core 테스트 24개 파일 → 패키지 리네임하여 복사
  - 신규 `http` 패키지 테스트: MockWebServer 기반 RestClient suspend DSL 테스트
  - 신규 `virtualthread` 패키지 테스트: VT AutoConfiguration 로드 검증
  - Spring Boot 4.x `spring-boot-starter-test`의 JUnit 5 + MockK 호환성 확인
  - `./gradlew :bluetape4k-spring-boot4-core:test` 통과
- **파일**: `spring-boot4/core/src/test/kotlin/io/bluetape4k/spring4/`
- **완료 기준**: 전체 테스트 통과

> **T04, T05, T06, T07, T08, T08-1은 병렬 실행 가능** (모두 T03에만 의존)

---

## Phase 3: 하위 모듈 구현

### T10. data-redis 모듈
- **complexity**: low
- **depends_on**: T01, T02
- **설명**: boot3/data-redis 복사 + BOM 오버라이드 + 패키지명 변경.
- **작업 내용**:
  - `build.gradle.kts`: `dependencyManagement { imports { mavenBom(Libs.spring_boot4_dependencies) } }` 추가
  - 소스 복사: `io.bluetape4k.spring.data.redis` → `io.bluetape4k.spring4.data.redis`
  - 테스트 복사 + 패키지 리네임
  - `./gradlew :bluetape4k-spring-boot4-data-redis:test` 통과
- **파일**: `spring-boot4/data-redis/`
- **완료 기준**: 테스트 통과

### T11. r2dbc 모듈
- **complexity**: low
- **depends_on**: T03 (core 의존)
- **설명**: boot3/r2dbc 복사 + BOM 오버라이드 + core 의존성을 boot4-core로 변경.
- **작업 내용**:
  - `build.gradle.kts`: `api(project(":bluetape4k-spring-boot4-core"))` + BOM 오버라이드
  - 소스/테스트 복사 + 패키지 리네임
  - `./gradlew :bluetape4k-spring-boot4-r2dbc:test` 통과
- **파일**: `spring-boot4/r2dbc/`
- **완료 기준**: 테스트 통과

### T12. mongodb 모듈
- **complexity**: medium
- **depends_on**: T03 (core 의존)
- **설명**: boot3/mongodb 복사 + BOM 오버라이드 + MongoDB Kotlin Driver 5.x 호환성 확인.
- **작업 내용**:
  - `build.gradle.kts`: `api(project(":bluetape4k-spring-boot4-core"))` + BOM 오버라이드
  - 소스/테스트 복사 + 패키지 리네임
  - Spring Data MongoDB 5.x API 변경 확인 (ReactiveMongoOperations 등)
  - `testImplementation(project(":bluetape4k-jackson3"))` (jackson2 → jackson3 전환)
  - `./gradlew :bluetape4k-spring-boot4-mongodb:test` 통과
- **파일**: `spring-boot4/mongodb/`
- **완료 기준**: 테스트 통과

### T13. cassandra 모듈
- **complexity**: medium
- **depends_on**: T03 (core 의존)
- **설명**: boot3/cassandra 복사 + BOM 오버라이드 + Cassandra driver 호환성 확인.
- **작업 내용**:
  - `build.gradle.kts`: `api(project(":bluetape4k-spring-boot4-core"))` + BOM 오버라이드
  - 소스/테스트 복사 + 패키지 리네임
  - Spring Data Cassandra 5.x API 변경 확인
  - kapt 설정 유지 (Cassandra Mapper 어노테이션 프로세서)
  - `./gradlew :bluetape4k-spring-boot4-cassandra:test` 통과
- **파일**: `spring-boot4/cassandra/`
- **완료 기준**: 테스트 통과

### T14. cassandra-demo 모듈
- **complexity**: low
- **depends_on**: T13
- **설명**: boot3/cassandra-demo 복사 + 배포 제외 설정.
- **작업 내용**:
  - `build.gradle.kts`: BOM 오버라이드 + `tasks.withType<PublishToMavenRepository> { enabled = false }` (또는 기존 배포 제외 패턴 사용)
  - 소스/테스트 복사 + 패키지 리네임
  - `./gradlew :bluetape4k-spring-boot4-cassandra-demo:test` 통과
- **파일**: `spring-boot4/cassandra-demo/`
- **완료 기준**: 테스트 통과, Maven 배포 제외 확인

> **T10은 독립 실행 가능** (core 불필요)
> **T11, T12, T13은 T03 완료 후 병렬 실행 가능**
> **T14는 T13에 의존**

---

## Phase 4: 검증 및 문서화

### T15. 전체 빌드 및 통합 테스트
- **complexity**: medium
- **depends_on**: T09, T10, T11, T12, T13, T14
- **설명**: 전체 spring-boot4 모듈 빌드 + 테스트 + BOM 충돌 검증.
- **작업 내용**:
  - `./gradlew :bluetape4k-spring-boot4-core:dependencies` 에서 Spring Framework 7.x 확인
  - `./gradlew :bluetape4k-spring-boot3-core:dependencies` 에서 Spring Framework 6.x 유지 확인 (루트 BOM 무결성)
  - 비-spring 모듈 (`bluetape4k-core`, `bluetape4k-io` 등)의 의존성 버전이 변경되지 않았는지 확인
  - `./gradlew build -x test` 전체 컴파일 성공
  - spring-boot4 전체 테스트: `./gradlew :bluetape4k-spring-boot4-core:test :bluetape4k-spring-boot4-data-redis:test :bluetape4k-spring-boot4-r2dbc:test :bluetape4k-spring-boot4-mongodb:test :bluetape4k-spring-boot4-cassandra:test`
- **완료 기준**: 전체 빌드 성공 + 테스트 통과 + BOM 충돌 없음

### T16. CLAUDE.md 및 README 업데이트
- **complexity**: low
- **depends_on**: T15
- **설명**: 루트 CLAUDE.md의 Architecture > Module Structure에 spring-boot4 섹션 추가.
- **작업 내용**:
  - `CLAUDE.md` — Spring Modules 섹션에 `spring-boot4/` 하위 모듈 기술
  - `spring-boot4/` 하위 각 모듈에 간단한 README.md 생성 (모듈 목적, 사용법, boot3 대비 변경점)
- **파일**: `CLAUDE.md`, `spring-boot4/README.md`
- **완료 기준**: 문서가 실제 모듈 구조와 일치

---

## 병렬 실행 그룹 요약

| 그룹 | 태스크 | 선행 조건 |
|------|--------|-----------|
| G1 | T01, T02 | 없음 |
| G2 | T04, T05, T06, T07, T08, T08-1 | T03 |
| G3 | T10 | T01, T02 |
| G4 | T11, T12, T13 | T03 |
| G5 | T15 | 모든 모듈 완료 |

---

## 태스크 목록 (ID순)

| ID | 이름 | complexity | depends_on |
|----|------|-----------|------------|
| T01 | Libs.kt 헬퍼 함수 추가 | low | - |
| T02 | 모듈 디렉토리 구조 생성 | low | - |
| T03 | core/build.gradle.kts (BOM 오버라이드 + Jackson 3) | high | T01, T02 |
| T04 | core — 기존 패키지 포팅 (beans~util, 14개 파일) | medium | T03 |
| T05 | core — jackson 패키지 (Jackson 3 전환) | high | T03 |
| T06 | core — rest, webflux 패키지 | medium | T03 |
| T07 | core — retrofit2, tests 패키지 | medium | T03 |
| T08 | core — 신규 http 패키지 (RestClient DSL) | high | T03 |
| T08-1 | core — 신규 virtualthread 패키지 | medium | T03 |
| T09 | core — 테스트 작성 및 통합 검증 | high | T04~T08-1 |
| T10 | data-redis 모듈 | low | T01, T02 |
| T11 | r2dbc 모듈 | low | T03 |
| T12 | mongodb 모듈 | medium | T03 |
| T13 | cassandra 모듈 | medium | T03 |
| T14 | cassandra-demo 모듈 | low | T13 |
| T15 | 전체 빌드 및 통합 테스트 | medium | T09~T14 |
| T16 | CLAUDE.md 및 README 업데이트 | low | T15 |

---

## 리스크 체크리스트

| 리스크 | 영향 태스크 | 대응 |
|--------|-----------|------|
| Jackson 3.x API 호환성 (ObjectMapper 변경) | T05 | `bluetape4k-jackson3` 모듈의 기존 유틸 활용, 필요 시 어댑터 작성 |
| Retrofit2가 Spring Framework 7과 비호환 | T07 | `compileOnly`로 유지, 비호환 시 해당 패키지 제외 |
| RestTemplate 참조 잔존 | T04, T06, T07 | Boot 4에서 삭제됨 → grep으로 참조 찾아 제거 |
| Reactor 2025.x 바이너리 비호환 | T03 | BOM 오버라이드로 모듈별 버전 고정 |
| MongoDB/Cassandra driver 메이저 버전 변경 | T12, T13 | Spring Data BOM이 관리, API 변경 시 어댑터 작성 |
| 루트 BOM 오버라이드 순서 | T03 | `dependencyManagement` 블록 순서 검증 (나중 선언 우선) |
