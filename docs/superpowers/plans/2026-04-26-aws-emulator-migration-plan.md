# AWS 에뮬레이터 전환 (LocalStack → floci + ElasticMQ + Mailpit) — 구현 Plan v2 (Step 3)

- **일자**: 2026-04-26
- **이슈**: #155
- **브랜치**: `feat/issue-155-aws-emulator` (worktree: `.worktrees/issue-155-aws-emulator`)
- **상위 Spec**: `docs/superpowers/specs/2026-04-26-aws-emulator-migration-design.md`
- **총 Task 수**: **19** (T0 / T1 / T1a / T2 / T3 / T4 / T5 / T6 / T7 / T8 / T9 / T10 / T11 / T12 / T13 / T13a / T13b / T13c / T14 / T15 / T16 / T17(N/A))
  - active: **18** (T17 제외)
- **복잡도 분포**: **high 4 · medium 7 · low 9** (T17은 N/A, 카운트 제외)
  - high: T1, T4, T9, T10
  - medium: T2, T5, T6, T8, T11, T12, T16
  - low: T0, T1a, T3, T7, T13, T13a, T13b, T13c, T14, T15

> Spec 의 §5 태스크 초안을 기반으로 phase / 종속성 / acceptance criteria / file path 를 보강한다.
> T17 은 Spec §5 에서 명시한 대로 **N/A** (`ci.yml` 에 AWS 전용 job 부재) → T16 만 수행.

---

## 1. Dependency Graph

```
Phase 0 (Prerequisites)
 └─ T0   Libs.kt 에 aws2_regions 상수 확인/추가             [low]

Phase 1 (Foundation)
 ├─ T1   AwsEmulatorServer 인터페이스                       [high]
 └─ T1a  AwsEmulatorServerExtensions.kt (AWS SDK 분리)      [low,    needs T0, T1, T7]

Phase 2 (New Servers)
 ├─ T4   FlociServer                                         [high,   needs T1]
 ├─ T5   ElasticMqServer + Libs.elasticmq                    [medium]
 └─ T6   MailpitServer                                       [medium]

Phase 3 (Deprecation)
 ├─ T2   LocalStackServer 인터페이스 구현 + @Deprecated     [medium, needs T1]
 └─ T3   MinIOServer @Deprecated                            [low]

Phase 4 (Build wiring)
 └─ T7   testcontainers/build.gradle.kts 의존성              [low,    needs T0, T4, T5]

Phase 5 (Migration)
 ├─ T9   aws/aws AbstractAwsTest 마이그레이션               [high,   needs T1, T2, T4]
 └─ T10  aws/aws-kotlin AbstractAwsTest 마이그레이션        [high,   needs T1, T2, T4]

Phase 6 (Tests & Verification)
 ├─ T8   testcontainers 단위 테스트 (Floci/ElasticMq/Mailpit) [medium, needs T4, T5, T6, T7]
 ├─ T11  default(localstack) 회귀 검증                      [medium, needs T9, T10]
 └─ T12  floci profile smoke 검증                            [medium, needs T9, T10]

Phase 7 (Docs & CI)
 ├─ T13  testcontainers README.md / README.ko.md            [low,    needs T4~T6]
 ├─ T13a 루트 CLAUDE.md 갱신                                 [low,    needs T1, T4]
 ├─ T13b aws/aws README.md / README.ko.md                   [low,    needs T9]
 ├─ T13c aws/aws-kotlin README.md / README.ko.md            [low,    needs T10]
 ├─ T16  nightly-tests.yml floci profile matrix             [medium, needs T9, T10]
 ├─ T14  superpowers INDEX 월별 파일 + 카운트 갱신          [low,    needs T13, T13a, T13b, T13c]
 ├─ T15  /wiki-update                                        [low,    needs T14]
 └─ T17  ci.yml 동기화                                       [N/A]
```

---

## 2. 전역 설계 제약 (모든 Task 적용)

Spec §2 / §4 / §8 의 핵심 제약을 한 곳에 모은다. 각 Task 는 이 제약을 위반하지 않아야 한다.

1. **AWS SDK 의존성 격리** (R8): `AwsEmulatorServer` 인터페이스는 AWS SDK 타입을 노출하지 않는다.
   `AwsCredentialsProvider`, `Region` 변환은 `AwsEmulatorServerExtensions.kt` 의 extension function 으로만 제공한다.
2. **Java getter 충돌 회피** (R7): `LocalStackServer` 처럼 Java 부모(`LocalStackContainer`) 를 상속하는 구현체는
   모든 인터페이스 프로퍼티를 `override val xxx: T get() = this.getXxx()` 형태로 명시 위임한다.
3. **이중 초기화 방지** (R6): `AbstractAwsTest.awsEmulator` lazy 는 기존 `LocalStackServer.Launcher.localStack` /
   `FlociServer.Launcher.floci` 싱글턴에 위임하며, 별도 인스턴스를 생성하지 않는다.
4. **floci.withServices 는 logging-only**: floci 컨테이너는 SERVICES env var 가 없으며 모든 지원 서비스가
   항상 활성화된다. `withServices(...)` 는 `activeServices: MutableSet<String>` 에 이름만 보관하고
   `properties()` 의 `services` 키 값으로만 노출한다 (실제 컨테이너 동작에 영향 없음).
5. **floci health check fallback**: `Wait.forListeningPort()` 사용. floci 가 `/_localstack/health` 를 지원함을
   확인하면 `Wait.forHttp(...)` 로 교체 — TODO 주석 필수.
6. **Scala 의존성 격리** (R4): `Libs.elasticmq` 는 testcontainers/build.gradle.kts 에 `compileOnly` +
   `testRuntimeOnly` 로만 등록한다. `api` / `implementation` 금지.
7. **ElasticMqServer ≠ AwsEmulatorServer** (Spec §3 결정): ElasticMqServer 는 인터페이스를 구현하지 않으며
   `AbstractAwsTest` 교체 대상이 아니다. SQS 전용 보조 유틸리티로 한정한다.
8. **Mailpit ≠ SES API**: Mailpit 은 SMTP 캡처 + Web UI 만 제공하며 AWS SES API 는 floci 가 담당한다.
   `MailpitServer.Launcher.wireToFloci(...)` helper 는 본 PR 에서 KDoc + 시그니처만 제시 (실제 구현은 후속 이슈).
9. **한 릴리스 호환 사이클**: LocalStackServer / MinIOServer 는 `@Deprecated(level = WARNING)` 만 부여하고
   본 PR 에서 삭제하지 않는다. 다음 minor 릴리스에서 삭제 (별도 이슈).
10. **JVM 단일 인스턴스**: 기존 `Launcher` 싱글턴 패턴(`val xxx: T by lazy { ... }`) 을 그대로 유지한다.
    `ShutdownQueue.register(...)` 호출 누락 금지.
11. **bluetape4k-patterns 일관성**: 모든 신규/수정 클래스는 다음 체크리스트를 만족해야 한다.
    - `companion object : KLogging()` (factory + logger 통합)
    - factory `operator fun invoke(...)` 에서 `requireNotBlank` / `requireNotNull` 등 boundary validation
    - 모든 public API 에 한국어 KDoc
    - testcontainers 서버는 `GenericServer` + `PropertyExportingServer` 인터페이스 구현 확인

---

## 3. Phase 0 — Prerequisites

### T0 — Libs.kt `aws2_auth`/`aws2_bom` 존재 확인 [low]

> **확인 완료 (2026-04-26)**: `aws2_bom` (line 584), `aws2_auth` (line 587) 모두 존재.
> `aws2_regions` 는 존재하지 않으나 **T1a 에서 `Region` extension을 제거**하였으므로 불필요.
> T0 는 검증 태스크로만 남으며 신규 상수 추가 없음.

**대상 파일**: `buildSrc/src/main/kotlin/Libs.kt` (확인 전용, 변경 없음)

**구현 사항**:
- `rg "aws2_bom|aws2_auth" buildSrc/src/main/kotlin/Libs.kt` 로 두 상수 존재 확인
- 변경 사항 없음 (이미 존재)

**종속**: 없음

**Acceptance**:
- [ ] `rg "aws2_bom\|aws2_auth" buildSrc/src/main/kotlin/Libs.kt` 결과 2건 출력
- [ ] 변경 없음 (`git diff buildSrc/` 클린)

**복잡도 사유**: 사전 확인으로 T1a/T7 컴파일 전 dependency 존재를 보장.

---

## 4. Phase 1 — Foundation

### T1 — `AwsEmulatorServer` 인터페이스 [high]

**대상 파일**:
- 신규: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/AwsEmulatorServer.kt`

**구현 사항**:
- `interface AwsEmulatorServer : GenericServer, PropertyExportingServer`
- 추상 프로퍼티: `endpoint: URI` / `accessKey: String` / `secretKey: String` / `regionName: String`
- 추상 메서드: `fun withServices(vararg services: String): AwsEmulatorServer`
- KDoc 필수 항목:
  - "Java testcontainers 클래스를 상속하는 구현체는 충돌 회피를 위해 모든 추상 프로퍼티를
     명시적 `override get() = this.getXxx()` 위임으로 해소해야 한다" (R7)
  - "본 인터페이스는 의도적으로 AWS SDK 타입을 노출하지 않는다.
     `AwsCredentialsProvider` 변환은 `AwsEmulatorServerExtensions` 의 extension 사용" (R8)
  - "구현체별 서비스 활성화 동작 시점이 다름 (LocalStack: 컨테이너 시작 전 enum, floci: logging-only)"

**Acceptance**:
- [ ] `./gradlew :bluetape4k-testcontainers:compileKotlin` 통과
- [ ] `ide_diagnostics` import 에러 없음
- [ ] 인터페이스가 AWS SDK 타입을 직접 import 하지 않음 (`software.amazon.awssdk.*` 0건)
- [ ] **bluetape4k-patterns 체크**: 인터페이스가 `GenericServer` + `PropertyExportingServer` 를 모두 상속, 모든 멤버에 한국어 KDoc 작성, 인터페이스 자체 KDoc 에 R7/R8 인용 포함

**복잡도 사유**: 두 구현체(LocalStack/floci) 의 모든 호환 포인트와 leaky abstraction 회피 설계가
인터페이스에 응축됨. 이후 모든 Task 의 기반.

---

### T1a — `AwsEmulatorServerExtensions.kt` [low]

**대상 파일**:
- 신규: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/AwsEmulatorServerExtensions.kt`

**구현 사항**:
> **확인 완료 (2026-04-26)**: `aws2_regions` Libs.kt 미존재. `Region` 반환 extension 제외.
> `aws2_auth` (Libs.kt line 587) 는 존재 → `getCredentialProvider()` 만 제공.

```kotlin
fun AwsEmulatorServer.getCredentialProvider(): StaticCredentialsProvider =
    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
// getRegion(): Region 은 제외 — aws2_regions 미존재. 호출부에서 Region.of(server.regionName) 직접 사용.
```
- import: `software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}` 만 필요
- KDoc: "본 파일은 `compileOnly(Libs.aws2_auth)` 를 요구한다.
  AWS SDK 가 classpath 에 없는 모듈은 본 extension 호출 시 `NoClassDefFoundError` 가 발생한다."

**종속**: T0 (aws2_auth 존재 확인), T1 (인터페이스), T7 (build.gradle.kts compileOnly 등록)

**Acceptance**:
- [ ] T1 인터페이스에서 SDK 타입 0건 유지 + 본 파일에서만 SDK import
- [ ] `./gradlew :bluetape4k-testcontainers:compileKotlin` 통과
- [ ] **bluetape4k-patterns 체크**: 모든 extension 에 한국어 KDoc, classpath 누락 시 동작 명시

---

## 5. Phase 2 — New Servers

### T4 — `FlociServer` [high]

**대상 파일**:
- 신규: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/FlociServer.kt`

**구현 사항** (Spec §4.3 그대로):
- `class FlociServer private constructor(...) : GenericContainer<FlociServer>(imageName), AwsEmulatorServer`
- 상수: `IMAGE = "floci/floci"`, `TAG = "1.5.7"`, `PORT = 4566`,
  `DEFAULT_ACCESS_KEY = "test"`, `DEFAULT_SECRET_KEY = "test"`, `DEFAULT_REGION = "us-east-1"`
- companion `operator fun invoke(...)` 2개 (String image / DockerImageName) — `requireNotBlank(image)` 검증
- `init { addExposedPorts(PORT); withReuse(reuse); setWaitStrategy(Wait.forListeningPort()); if (useDefaultPort) exposeCustomPorts(PORT) }`
- `withServices(vararg)` 는 logging-only (Spec §4.3 KDoc 인용 + TODO 주석)
- `withDockerSocket()` (기본 비활성, R3 보안 표면 최소화)
- `getCredentialProvider()` concrete 메서드 (인터페이스 의무 아님)
- `companion object : KLogging() { val services = listOf(10개 서비스); operator fun invoke(...); ... }`
- `object Launcher { val floci by lazy { FlociServer().apply { start(); ShutdownQueue.register(this) } } }`
  - 10개 서비스: `cloudwatch, logs, dynamodb, kinesis, kms, s3, ses, sns, sqs, sts`
  - `ShutdownQueue.register(this)` 호출 누락 금지
- `propertyKeys()` / `properties()` 는 Spec §4.9 표 그대로 (`services` 키 포함)

**종속**: T1

**Acceptance**:
- [ ] `./gradlew :bluetape4k-testcontainers:compileKotlin` 통과
- [ ] `Wait.forListeningPort()` 사용 + `// TODO: floci /_localstack/health 지원 시 forHttp 로 교체` 주석
- [ ] `withServices` 가 컨테이너 env var 를 설정하지 않음 (Spec §2 R3 logging-only 검증)
- [ ] `getCredentialProvider()` 는 인터페이스에 없음 (concrete 메서드 한정)
- [ ] **bluetape4k-patterns 체크**: `companion object : KLogging()` 존재, factory `invoke()` 에 `requireNotBlank` 검증, 모든 public API 에 한국어 KDoc, `GenericServer`(via AwsEmulatorServer) + `PropertyExportingServer` 구현 확인

**복잡도 사유**: Wait strategy fallback + withServices logging-only 의도 + Launcher 싱글턴 + Java 부모와의
프로퍼티 override 정합성을 한 번에 잡아야 함.

---

### T5 — `ElasticMqServer` + `Libs.elasticmq` [medium]

**대상 파일**:
- 신규: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/embedded/ElasticMqServer.kt`
- 수정: `buildSrc/src/main/kotlin/Libs.kt` — `const val elasticmq = "org.elasticmq:elasticmq-rest-sqs_2.13:1.6.12"` 추가

**구현 사항** (Spec §4.4):
- `class ElasticMqServer private constructor(private val requestedPort: Int) : AutoCloseable, PropertyExportingServer`
- **`AwsEmulatorServer` 비-구현** (Spec §3 결정 + KDoc 명시)
- `companion object : KLogging() { operator fun invoke(port: Int = pickFreePort()) = ElasticMqServer(port) }`
  — `require(port in 0..65535) { "port out of range: $port" }`
- `pickFreePort(): Int = ServerSocket(0).use { it.localPort }`
- `start(): ElasticMqServer = apply { server = SQSRestServerBuilder.withPort(...).withInterface("localhost").start() }`
- `close()` → `server?.stopAndWait()`
- `Launcher.elasticmq by lazy { ... }` + `ShutdownQueue.register(AutoCloseable { close() })`
- `propertyKeys` / `properties` 는 Spec §4.9 표 그대로
- KDoc 에 "in-process 서버, Docker 미사용, SQS 전용, AwsEmulatorServer 비-구현" 명시

**종속**: 없음 (Libs.kt 변경은 독립)

**Acceptance**:
- [ ] `./gradlew :bluetape4k-testcontainers:compileKotlin` 통과
- [ ] `ElasticMqServer` 가 `AwsEmulatorServer` 를 구현하지 않음 (`grep -F "AwsEmulatorServer" ElasticMqServer.kt` 0건)
- [ ] `Libs.elasticmq` 상수 추가 확인
- [ ] **bluetape4k-patterns 체크**: `companion object : KLogging()` 존재, factory `invoke()` 에 port 범위 검증, 모든 public API 에 한국어 KDoc, `PropertyExportingServer` 구현 확인 (인터페이스 비구현 결정 KDoc 명시)

**복잡도 사유**: Scala transitive 격리(T7과 결합) + 비표준 JVM 임베드 lifecycle (start/stopAndWait)
+ 인터페이스 비구현 결정 의도가 KDoc/구조 모두에 일관되게 표현되어야 함.

---

### T6 — `MailpitServer` [medium]

**대상 파일**:
- 신규: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/mail/MailpitServer.kt`

**구현 사항** (Spec §4.5):
- `class MailpitServer ... : GenericContainer<MailpitServer>(imageName), GenericServer, PropertyExportingServer`
- `IMAGE = "axllent/mailpit"`, `TAG = "v1.29"`, `SMTP_PORT = 1025`, `UI_PORT = 8025`
- `addExposedPorts(SMTP_PORT, UI_PORT)`
- `setWaitStrategy(Wait.forHttp("/").forPort(UI_PORT).forStatusCode(200))`
- `val smtpPort: Int get() = getMappedPort(SMTP_PORT)` / `val uiPort: Int get() = getMappedPort(UI_PORT)`
- `override val port = smtpPort` / `override val url = "smtp://$host:$smtpPort"`
- `webUiUrl` / `apiUrl` 별도 노출
- `companion object : KLogging() { operator fun invoke(image: String = "$IMAGE:$TAG"): MailpitServer { requireNotBlank(image); return MailpitServer(DockerImageName.parse(image)) } }`
- `object Launcher { val mailpit by lazy { MailpitServer().apply { start(); ShutdownQueue.register(this) } } }`
- KDoc: "AWS SES API 는 구현하지 않음. SES 통합 테스트는 floci → Mailpit SMTP relay 결합 필요" (R5)
- **`wireToFloci(floci: FlociServer)` companion helper 메서드 시그니처를 본 PR 에서 정의** — KDoc + 메서드 선언만 작성하고 본문은 `TODO("후속 이슈 #XXX 에서 구현: floci SES → Mailpit SMTP relay 환경변수 wiring")` 로 남긴다. 페어링 패턴을 API 표면에서 미리 표현해 후속 작업의 진입점을 명확히 한다.

**종속**: 없음

**Acceptance**:
- [ ] `./gradlew :bluetape4k-testcontainers:compileKotlin` 통과
- [ ] KDoc 에 "SES API 비구현" 경고 명시
- [ ] `mail/` 디렉터리 신규 생성 확인 (`fd MailpitServer testing/testcontainers/`)
- [ ] `wireToFloci(floci: FlociServer)` 메서드 시그니처 + KDoc 존재, body 는 `TODO(...)` (실제 구현은 후속 이슈)
- [ ] **bluetape4k-patterns 체크**: `companion object : KLogging()` 존재, factory `invoke()` 에 `requireNotBlank` 검증, 모든 public API 에 한국어 KDoc, `GenericServer` + `PropertyExportingServer` 구현 확인

---

## 6. Phase 3 — Deprecation

### T2 — `LocalStackServer` 인터페이스 구현 + `@Deprecated` [medium]

**대상 파일**:
- 수정: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/LocalStackServer.kt`

**구현 사항** (Spec §4.6):

**기존 코드 컨텍스트**: 현재 `LocalStackServer.kt` line 107-110 에 다음 메서드가 존재한다.
```kotlin
override fun withServices(vararg services: String): LocalStackServer =
    apply { super.withServices(*services.map { it.lowercase() }.toTypedArray()) }
```
**확인 완료 (2026-04-26)**: `LocalStackContainer.withServices(vararg String)` 시그니처를 javap 로 실측 확인.
기존 `super.withServices(*services.map { it.lowercase() }.toTypedArray())` 는 실제로 동작하는 String 오버로드임.
**enum 어댑터 불필요** — 기존 `withServices` 바디 그대로 유지, `AwsEmulatorServer` 구현 추가만 수행.

- 클래스 선언에 `, AwsEmulatorServer` 추가
- **모든 인터페이스 프로퍼티를 명시 override 위임** (R7 — Java getter 충돌 해소):
  ```kotlin
  override val endpoint: URI get() = this.getEndpoint()
  override val accessKey: String get() = this.getAccessKey()
  override val secretKey: String get() = this.getSecretKey()
  override val regionName: String get() = this.getRegion()
  ```
- **기존 `withServices(vararg services: String)` (line 107-110) 바디 유지** — return type 을 `LocalStackServer` → `LocalStackServer` 그대로 유지 (covariant, 컴파일 통과):
  ```kotlin
  override fun withServices(vararg services: String): LocalStackServer =
      apply { super.withServices(*services.map { it.lowercase() }.toTypedArray()) }
  ```
  String→Enum 변환 불필요. 기존 String 오버로드(`vararg String`)가 실측 확인됨.
- 클래스 상단에 `@Deprecated(message=..., replaceWith=ReplaceWith("FlociServer", "io.bluetape4k.testcontainers.aws.FlociServer"), level=DeprecationLevel.WARNING)`
- Deprecation message: "LocalStack 프로젝트가 archived(2026-03-23). 신규 코드는 FlociServer 사용 권장.
  한 릴리스 사이클(다음 minor, v1.8.0 예정) 후 삭제 예정."
- 기존 `getCredentialProvider()` / `Launcher.localStack` 등 외부 API 시그니처 **변경 금지** (외부 호환)

**종속**: T1

**Acceptance**:
- [ ] `./gradlew :bluetape4k-testcontainers:compileKotlin` 통과 (Java getter 충돌 0건)
- [ ] `ide_diagnostics` 결과 `@Deprecated` warning 만 노출 (error 0건)
- [ ] `Launcher.localStack` 시그니처 변경 없음 (외부 호환 검증)
- [ ] **`git diff testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/LocalStackServer.kt` 결과에서 기존 `super.withServices(*services.map { it.lowercase() }.toTypedArray())` 호출이 제거되고, 새 enum 어댑터 구현으로 교체되었음을 확인** (REPLACE 검증)
- [ ] 신규 `withServices` 가 `LocalStackContainer.Service.valueOf(...)` 를 호출하여 enum 변환을 수행하는지 코드 리뷰로 확인
- [ ] **bluetape4k-patterns 체크**: 기존 `companion object : KLogging()` 유지, factory 검증 유지, 모든 신규/변경 public API 에 한국어 KDoc, deprecation message 한글 명시

**복잡도 사유**: Java 부모와 Kotlin 인터페이스 간 시그니처 충돌 해소 + deprecation 정책의 정확한 적용이 필요.
외부 API 호환을 깨면 다운스트림 모듈 빌드가 모두 깨짐. (enum 어댑터는 제거됨 — String 오버로드 실측 확인)

---

### T3 — `MinIOServer` `@Deprecated` [low]

**대상 파일**:
- 수정: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/MinIOServer.kt`

**구현 사항**:
- 클래스 상단에 `@Deprecated(message=..., replaceWith=ReplaceWith("FlociServer", "io.bluetape4k.testcontainers.aws.FlociServer"), level=DeprecationLevel.WARNING)`
- message: "MinIO core 가 archived(2026-04-25), AIStor 로 rebrand. S3 통합 테스트는 FlociServer 사용 권장."
- 그 외 시그니처 / 동작 변경 없음

**종속**: 없음

**Acceptance**:
- [ ] `./gradlew :bluetape4k-testcontainers:compileKotlin` 통과
- [ ] `MinIOServer` 호출부에서 `@Deprecated` warning 발생 확인 (`ide_diagnostics`)

---

## 7. Phase 4 — Build Wiring

### T7 — `testing/testcontainers/build.gradle.kts` 의존성 [low]

**대상 파일**:
- 수정: `testing/testcontainers/build.gradle.kts`

**구현 사항** (Spec §4.8):
```kotlin
// AWS SDK v2 — AwsEmulatorServerExtensions.kt 전용 (인터페이스 자체는 비-의존)
compileOnly(platform(Libs.aws2_bom))
compileOnly(Libs.aws2_auth)
compileOnly(Libs.aws2_regions)

// ElasticMQ (Scala transitive 격리)
compileOnly(Libs.elasticmq)
testRuntimeOnly(Libs.elasticmq)
```
- 기존 `compileOnly(Libs.testcontainers_localstack)` / `compileOnly(Libs.testcontainers_minio)` /
  `compileOnly(Libs.minio)` 유지
- `api` / `implementation` 으로 elasticmq 추가 금지 (Scala transitive 차단)

**종속**: T0 (Libs.aws2_regions 가 정의되어 있어야 컴파일), T4 (FlociServer 가 SDK 미참조 검증), T5 (Libs.elasticmq 등록)

**Acceptance**:
- [ ] `./gradlew :bluetape4k-testcontainers:dependencies --configuration runtimeClasspath | rg -i "scala-library"` → 0건
- [ ] `./gradlew :bluetape4k-testcontainers:dependencies --configuration testRuntimeClasspath | rg -i "scala-library"` → 1건 이상
- [ ] `./gradlew :bluetape4k-testcontainers:build -x test` 통과

---

## 8. Phase 5 — AbstractAwsTest 마이그레이션

### T9 — `aws/aws/AbstractAwsTest.kt` [high]

**대상 파일**:
- 수정: `aws/aws/src/test/kotlin/io/bluetape4k/aws/AbstractAwsTest.kt`

**구현 사항** (Spec §4.7):
- companion 에 `awsEmulator: AwsEmulatorServer by lazy { ... }` 도입
- 분기는 `System.getProperty("bluetape4k.aws.emulator", "localstack").lowercase()` 기준:
  - `"floci"` → `FlociServer.Launcher.floci`
  - else → `LocalStackServer.Launcher.localStack`
- **이중 초기화 방지** (R6): 신규 인스턴스 생성 금지, 기존 Launcher 객체에 위임만
- 기존 `localStackServer: LocalStackServer` 필드는 alias 로 유지 + `@Deprecated("awsEmulator 사용 권장", ReplaceWith("awsEmulator"))`:
  ```kotlin
  val localStackServer: LocalStackServer
      get() = awsEmulator as? LocalStackServer
          ?: error("emulator is not LocalStack: $emulatorKind")
  ```
- SDK helper extension 추가 (companion 내부):
  ```kotlin
  fun AwsEmulatorServer.region(): Region = Region.of(this.regionName)
  val AwsEmulatorServer.credentialsProvider get() = staticCredentialsProviderOf(this.accessKey, this.secretKey)
  ```
- 기존 10개 서비스 리스트 (`services`) 유지

**Gradle system property 전달 — 확인 완료 (2026-04-26)**:
- 루트 `build.gradle.kts` 의 전역 `test {}` 블록에 `systemProperty` 전달 설정 **없음** (`rg "systemProperty" build.gradle.kts` 결과 0건).
- `aws/aws/build.gradle.kts` / `aws/aws-kotlin/build.gradle.kts` 양쪽 모두 `test {}` 블록 **없음**.
- **결론**: `-Dbluetape4k.aws.emulator=floci` 는 Gradle JVM 에만 설정되고 forked test JVM 에는 전달 안 됨.
- **필수 추가**: `aws/aws/build.gradle.kts` 와 `aws/aws-kotlin/build.gradle.kts` 양쪽에:
  ```kotlin
  tasks.test {
      systemProperty("bluetape4k.aws.emulator",
          System.getProperty("bluetape4k.aws.emulator", "localstack"))
  }
  ```

**종속**: T1, T2, T4

**Acceptance**:
- [ ] `./gradlew :bluetape4k-aws:compileTestKotlin` 통과
- [ ] `awsEmulator` 가 Launcher 싱글턴에 위임 (신규 `FlociServer()` / `LocalStackServer()` 호출 0건)
- [ ] `localStackServer` alias 사용 시 `@Deprecated` warning 발생
- [ ] floci profile 분기 로직이 `bluetape4k.aws.emulator` 시스템 프로퍼티만 참조
- [ ] Gradle test task 가 `bluetape4k.aws.emulator` system property 를 자식 JVM 에 전달함을 확인 (수동: `./gradlew :bluetape4k-aws:test -Dbluetape4k.aws.emulator=floci --info | rg "bluetape4k.aws.emulator"`)

**복잡도 사유**: 분기 시점 / 싱글턴 위임 / 기존 alias 호환 / SDK helper extension 의 4개 책임이 한 파일에
응축됨. 잘못 구현하면 모든 AWS 통합 테스트가 시작 단계에서 실패.

---

### T10 — `aws/aws-kotlin/AbstractAwsTest.kt` [high]

**대상 파일**:
- 수정: `aws/aws-kotlin/src/test/kotlin/io/bluetape4k/aws/kotlin/AbstractAwsTest.kt`

**구현 사항** (Spec §4.7 aws-kotlin 섹션):

**기존 코드 컨텍스트 (조사 결과)**:
- 기존 `LocalStackContainer.endpointUrl` extension 은 `aws/aws-kotlin/src/test/kotlin/io/bluetape4k/aws/kotlin/AbstractAwsTest.kt` 의 **companion object 내부 (대략 line 38-51)** 에 inline 정의되어 있음.
- `kotlinCredentialsProvider` 도 같은 파일 내 inline extension 으로 정의되어 있음.
- 별도 extension 파일이 없으므로 본 PR 에서 새 extension 파일을 신규 생성하거나 기존 위치에 추가/병기한다.
- `AbstractAwsTest.kt` 내 `companion object` 의 `localStackServer` 필드는 `LocalStackServer` 타입(= `LocalStackContainer` 서브클래스). T9 와 동일하게 alias 로 유지하므로 기존 32+ caller 는 변경 없이 컴파일된다.

**구현 사항**:
- T9 와 동일하게 `awsEmulator: AwsEmulatorServer by lazy { ... }` 도입
- 신규 extension function 정의 (receiver = `AwsEmulatorServer`) — **신규 파일 `aws/aws-kotlin/src/test/kotlin/io/bluetape4k/aws/kotlin/AwsEmulatorServerKotlinExtensions.kt` 권장** (또는 `AbstractAwsTest.kt` companion 내부에 inline 정의도 가능하지만 가독성을 위해 분리 권장):
  ```kotlin
  val AwsEmulatorServer.endpointUrl: Url
      get() = Url.parse(this.endpoint.toString())

  val AwsEmulatorServer.kotlinCredentialsProvider: AwsCredentialsProvider
      get() = StaticCredentialsProvider {
          accessKeyId = this@kotlinCredentialsProvider.accessKey
          secretAccessKey = this@kotlinCredentialsProvider.secretKey
      }

  val AwsEmulatorServer.kotlinRegion: String get() = this.regionName
  ```
- **기존 `LocalStackContainer.endpointUrl` / `LocalStackContainer.kotlinCredentialsProvider` extension (AbstractAwsTest.kt:38-51 위치) 은 같은 PR 에서 `@Deprecated(level=WARNING, replaceWith=ReplaceWith("(emulator as AwsEmulatorServer).endpointUrl"))` 처리** — 한 릴리스 호환 사이클 동안 유지
- `localStackServer` alias 가 `LocalStackServer`(= `LocalStackContainer` 의 서브클래스) 타입으로 유지되므로 기존 `localStackServer.endpointUrl` 호출 32+ 곳은 컴파일 변경 없이 동작 (단 deprecation warning 만 노출)
- AWS Kotlin SDK 의존성은 **테스트 모듈** 한정이므로 본 모듈 build.gradle.kts 변경 불필요 (확인 필요)
- T9 와 동일한 Gradle system property 전달 검증을 본 모듈에도 적용

**종속**: T1, T2, T4

**Acceptance**:
- [ ] `./gradlew :bluetape4k-aws-kotlin:compileTestKotlin` 통과
- [ ] receiver 가 `AwsEmulatorServer` 인 신규 extension 정의 + 기존 `LocalStackContainer.*` extension `@Deprecated` 동시 적용 (`rg "@Deprecated" aws/aws-kotlin/src/test/.../AbstractAwsTest.kt` ≥ 2건)
- [ ] floci profile 분기 일관성 확인 (T9 와 동일 로직)
- [ ] 기존 `localStackServer.endpointUrl` 호출 사이트가 컴파일 가능 (alias 가 `LocalStackContainer` 서브클래스 타입 유지)

**복잡도 사유**: Kotlin SDK 의 `Url`, `AwsCredentialsProvider` 타입은 Java SDK 와 별도. receiver 변경이
호출부 전체에 전파되므로 `ide_find_references` 로 사전 영향도 분석 필수. extension 회귀 시 컴파일 에러
대량 발생 가능.

---

## 9. Phase 6 — Tests & Verification

### T8 — testcontainers 단위 테스트 [medium]

**대상 파일**:
- 신규: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/FlociServerTest.kt`
- 신규: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/embedded/ElasticMqServerTest.kt`
- 신규: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/mail/MailpitServerTest.kt`

**구현 사항**:
각 서버에 대해 다음 시나리오를 JUnit 5 + bluetape4k-assertions 로 작성한다.
- start/stop 라이프사이클 정상 동작
- `propertyKeys()` 가 Spec §4.9 표와 일치 (Set 비교)
- `properties()` 가 모든 키에 non-blank 값을 채움 (`shouldNotBeNullOrBlank`)
- FlociServer 추가:
  - `withServices("s3", "sqs")` 후 `properties()["services"]` 가 `"s3,sqs"` 또는 동일 set 포함
  - `endpoint.scheme == "http"` / `endpoint.port > 0`
  - `getCredentialProvider()` 가 NPE 없이 생성됨
- ElasticMqServer 추가:
  - `use { it.start() }` 패턴 동작
  - `endpoint.port == requestedPort`
  - `pickFreePort()` 가 매번 다른 포트 반환 가능 (반복 호출 시 충돌 없음)
- MailpitServer 추가:
  - `smtpPort != uiPort`
  - `webUiUrl.startsWith("http://")` / `url.startsWith("smtp://")`
- 각 테스트는 `@Testcontainers` 또는 manual lifecycle 사용 (기존 `KeycloakServerTest` 패턴 참고)

**종속**: T4, T5, T6, T7

**Acceptance**:
- [ ] `./gradlew :bluetape4k-testcontainers:test --tests "*FlociServerTest"` 통과
- [ ] `./gradlew :bluetape4k-testcontainers:test --tests "*ElasticMqServerTest"` 통과
- [ ] `./gradlew :bluetape4k-testcontainers:test --tests "*MailpitServerTest"` 통과
- [ ] 3개 클래스 합산 ≥ 12개 테스트 케이스

---

### T11 — default(localstack) profile 회귀 검증 [medium]

**대상 파일**: 없음 (테스트 실행 + testlog 기록)

**구현 사항**:
- `./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test` 실행
- 시스템 프로퍼티 미지정 → 기본값 `localstack` 으로 fallback 검증
- 기존 통과율 100% 유지 (회귀 0건)
- 결과를 `docs/testlogs/2026-04.md` 표 맨 위에 추가

**종속**: T9, T10

**Acceptance**:
- [ ] aws + aws-kotlin 두 모듈 테스트 모두 PASS
- [ ] **testlog 표준 행 추가**: `docs/testlogs/2026-04.md` 맨 위 행에 다음 형식으로 추가:
  ```
  | 2026-04-26 | AWS emulator migration (default profile) | bluetape4k-aws/aws-kotlin | N passing, M skipped | ✅ | duration | profile=localstack |
  ```

---

### T12 — floci profile smoke test [medium]

**대상 파일**: 없음 (테스트 실행 + testlog 기록)

**구현 사항**:
- `./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test -Dbluetape4k.aws.emulator=floci` 실행
- SQS / SNS / S3 기본 시나리오 1건 이상 통과 확인이 acceptance 기준 (Spec §7)
- 회귀 항목은 후속 이슈 draft 로 분리 (이슈 본문에 항목 리스트 첨부 — Spec §6)
- testlog 에 "SQS pass / SNS pass / S3 pass / 그 외 N건 회귀 → 후속 #XXX" 형식 기록

**종속**: T9, T10

**Acceptance**:
- [ ] floci profile 에서 SQS/SNS/S3 기본 1건 이상 PASS (Spec §7)
- [ ] **testlog 표준 행 추가**: `docs/testlogs/2026-04.md` 맨 위 행에 다음 형식으로 추가:
  ```
  | 2026-04-26 | AWS emulator migration (floci profile smoke) | bluetape4k-aws/aws-kotlin | N passing, M skipped, K regressions | ⚠️ | duration | profile=floci |
  ```
- [ ] 회귀 항목 후속 이슈 draft 작성 + testlog 본문에 후속 이슈 번호 링크 명시

**Open question (구현 단계 결정)**:
- floci 의 SES SMTP relay 가 Mailpit 없이는 fail 할 가능성 → 본 PR smoke 범위에서 SES 제외 권장
- floci wire-protocol 차이로 통과 못 하는 항목이 너무 많으면 smoke 기준을 "S3 1건" 으로 좁힐지 재검토

---

## 10. Phase 7 — Docs & CI

### T13 — testcontainers README 업데이트 [low]

**대상 파일**:
- 수정: `testing/testcontainers/README.md`
- 수정: `testing/testcontainers/README.ko.md`

**구현 사항**:
- 신규 섹션: "AWS Emulators (FlociServer / ElasticMqServer / MailpitServer)"
- LocalStackServer / MinIOServer 항목에 "Deprecated since 1.7.0" 명시 + replacement 안내
- Mermaid 다이어그램 추가: `AwsEmulatorServer` 인터페이스 ← LocalStackServer / FlociServer 구현
- 사용 예시 코드 블록: FlociServer.Launcher.floci, ElasticMqServer().use { ... }, MailpitServer.Launcher.mailpit
- floci ↔ Mailpit 결합 패턴 1단락 (Spec §2 R5 인용)

**종속**: T4, T5, T6

**Acceptance**:
- [ ] README.md / README.ko.md 동시 갱신
- [ ] Mermaid 클래스 다이어그램 1개 이상 포함 (CLAUDE.md "README Diagrams" 규칙)

---

### T13a — 루트 `CLAUDE.md` 갱신 [low]

**대상 파일**:
- 수정: `CLAUDE.md`

**구현 사항**:
- "Key Design Patterns" 섹션에 항목 추가:
  > **AwsEmulatorServer 인터페이스**: `bluetape4k-testcontainers` 의 `LocalStackServer` / `FlociServer`
  > 공통 contract. AWS SDK 의존성은 인터페이스에서 격리되며 `AwsEmulatorServerExtensions` 에 분리.
  > `AbstractAwsTest.awsEmulator` lazy 가 `bluetape4k.aws.emulator` 시스템 프로퍼티로
  > floci/localstack 을 분기 (default = localstack).
- "Module Groups" 표 `testing/` 행에 `aws-emulators(floci/elasticmq/mailpit)` 키워드 보강 검토

**종속**: T1, T4

**Acceptance**:
- [ ] CLAUDE.md diff 에 신규 패턴 1개 항목 추가 확인
- [ ] 기존 NetCDF 등 다른 패턴 항목 변경 없음

---

### T13b — `aws/aws` README 업데이트 [low]

**대상 파일**:
- 수정: `aws/aws/README.md`
- 수정: `aws/aws/README.ko.md`

**구현 사항**:
- "테스트 실행" 또는 "Testing" 섹션에 다음 내용 추가:
  - `bluetape4k.aws.emulator` system property 사용법 (`localstack` | `floci`, default = `localstack`)
  - `AbstractAwsTest.awsEmulator` 필드 사용 패턴 + `localStackServer` alias 가 `@Deprecated` 임을 명시
  - 사용 예:
    ```bash
    # default (localstack)
    ./gradlew :bluetape4k-aws:test
    # floci profile
    ./gradlew :bluetape4k-aws:test -Dbluetape4k.aws.emulator=floci
    ```
- 변경 이유 1단락: LocalStack archived → FlociServer 권장 (간단한 마이그레이션 가이드)

**종속**: T9

**Acceptance**:
- [ ] README.md / README.ko.md 동시 갱신 (CLAUDE.md "README.md and README.ko.md" 규칙)
- [ ] `bluetape4k.aws.emulator` / `awsEmulator` 키워드가 두 파일에 모두 존재

---

### T13c — `aws/aws-kotlin` README 업데이트 [low]

**대상 파일**:
- 수정: `aws/aws-kotlin/README.md`
- 수정: `aws/aws-kotlin/README.ko.md`

**구현 사항**:
- T13b 와 동일 내용 (단, AWS Kotlin SDK 컨텍스트로 작성)
- `AwsEmulatorServer.endpointUrl` / `AwsEmulatorServer.kotlinCredentialsProvider` 신규 extension 사용 예시 1건
- 기존 `LocalStackContainer.endpointUrl` extension 이 `@Deprecated` 임을 명시

**종속**: T10

**Acceptance**:
- [ ] README.md / README.ko.md 동시 갱신
- [ ] `bluetape4k.aws.emulator` / `awsEmulator` / `endpointUrl` (AwsEmulatorServer receiver) 키워드 존재

---

### T16 — `nightly-tests.yml` floci profile matrix [medium]

**대상 파일**:
- 수정: `.github/workflows/nightly-tests.yml` (lines 1012-1084 `test-aws` job)

**현재 상태 (조사 결과)**:
- 현재 `test-aws` job 에는 **`strategy.matrix` 블록이 존재하지 않는다**. 단일 step 에서 `./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test --parallel` 만 호출.
- 본 Task 는 `strategy.matrix` 를 신규 도입하여 `localstack` / `floci` 두 profile 을 nightly 단계에서 병렬 검증한다.

**구현 사항**:
1. `test-aws` job 에 `strategy:` 블록을 신규 추가:
   ```yaml
   strategy:
     fail-fast: false
     matrix:
       emulator: [localstack, floci]
   ```
2. gradle test 호출을 `matrix.emulator` 로 파라미터화:
   ```yaml
   - name: Run AWS tests (${{ matrix.emulator }})
     run: |
       ./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test \
         -Dbluetape4k.aws.emulator=${{ matrix.emulator }} --parallel
   ```
3. 아티팩트 업로드 step 의 `name` 을 matrix 별로 분리하여 충돌 방지:
   ```yaml
   - name: Upload test results
     if: always()
     uses: actions/upload-artifact@v4
     with:
       name: nightly-test-results-aws-${{ matrix.emulator }}
       path: |
         **/build/reports/tests/test/
         **/build/test-results/test/
   ```
4. **`continue-on-error` 정책**:
   - `localstack` matrix run: `continue-on-error: false` (또는 미지정 — 기본 false). 기존 동작 보존, 회귀 시 nightly 실패.
   - `floci` matrix run: `continue-on-error: true`. floci wire-protocol 회귀가 nightly 를 차단하지 않도록 실험 단계로 한정.
   - 구현 방법: `continue-on-error: ${{ matrix.emulator == 'floci' }}` 표현식을 step 또는 job 레벨에 적용.

**종속**: T9, T10

**Acceptance**:
- [ ] **`yq '.jobs.test-aws.strategy.matrix.emulator' .github/workflows/nightly-tests.yml` 가 `[localstack, floci]` (또는 `- localstack\n- floci`) 출력**
- [ ] `yq '.jobs.test-aws.strategy["fail-fast"]' .github/workflows/nightly-tests.yml` 결과가 `false`
- [ ] gradle 호출 라인에 `-Dbluetape4k.aws.emulator=${{ matrix.emulator }}` 포함 (`rg "matrix.emulator" .github/workflows/nightly-tests.yml` ≥ 3건: matrix 정의 1 + gradle 1 + artifact name 1)
- [ ] 아티팩트 업로드 name 에 `${{ matrix.emulator }}` 포함 (충돌 방지)
- [ ] floci matrix run 에 한해 `continue-on-error: true` 적용 (`localstack` 은 strict)
- [ ] yaml lint 통과 (`yq eval '.' .github/workflows/nightly-tests.yml > /dev/null`)

---

### T17 — `ci.yml` 동기화 [N/A]

**판정**: Spec §5 에서 명시한 대로 `ci.yml` 에 AWS 전용 job 이 부재. nightly-tests.yml 의 `test-aws` job
만 존재하므로 T16 으로 완결. 본 Task 는 수행 항목 없음 (PR 본문에만 "T17 N/A 사유: ci.yml 에
AWS job 없음 — nightly-tests.yml 의 test-aws 만 변경" 명시).

**Acceptance**:
- [ ] PR 본문에 N/A 사유 1줄 명시
- [ ] `rg -n "aws|localstack|floci" .github/workflows/ci.yml` 결과 0건 재확인

---

### T14 — superpowers INDEX 월별 파일 [low]

**대상 파일**:
- 수정: `docs/superpowers/index/2026-04.md`
- 수정: `docs/superpowers/INDEX.md`

**구현 사항**:
- `2026-04.md` 표 맨 위 행에 본 spec/plan 추가 (날짜 / 이슈 / spec 경로 / plan 경로 / 상태)
- `INDEX.md` 의 2026-04 카운트 +1 갱신

**종속**: T13, T13a, T13b, T13c

**Acceptance**:
- [ ] 2026-04.md 첫 표 행이 "2026-04-26 / #155 / aws-emulator-migration" 으로 시작
- [ ] INDEX.md 카운트 일관성 확인

---

### T15 — `/wiki-update` [low]

**대상 파일**:
- 자동 생성: `.claude/wiki/...` (skill 자동 결정)

**구현 사항**:
- `/wiki-update` 스킬 호출 → spec/plan 을 wiki 인덱싱
- LocalStack archived / floci replacement 키워드를 wiki 검색 대상에 포함

**종속**: T14

**Acceptance**:
- [ ] `gno query "floci" -c wiki` 결과 1건 이상

---

## 11. PR 전 체크리스트 (CLAUDE.md "Before Creating a PR" 매핑)

본 plan 완료 후 PR 생성 전 다음을 확인한다 (CLAUDE.md MANDATORY 항목 정밀 매핑).

- [ ] **로컬 테스트 전수 통과** (T8 + T11 + T12)
  - `./gradlew :bluetape4k-testcontainers:test`
  - `./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test`
  - `./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test -Dbluetape4k.aws.emulator=floci`
- [ ] **Code review 실행** — `oh-my-claudecode:code-reviewer` 에이전트로 HIGH/CRITICAL 0건 확인
- [ ] **README.md + README.ko.md** 동시 갱신
  - testing/testcontainers (T13)
  - aws/aws (T13b)
  - aws/aws-kotlin (T13c)
- [ ] **KDoc** — 신규 public API (T1, T1a, T4, T5, T6) 모두 KDoc 보유
- [ ] **bluetape4k-patterns** — T1, T4, T5, T6 모두 `companion object : KLogging()` + factory 검증 + 한국어 KDoc 보유
- [ ] **worktree** 안에서 작업 (`.worktrees/issue-155-aws-emulator/`)
- [ ] **testlog 갱신** — `docs/testlogs/2026-04.md` 에 T11 / T12 결과 행 추가 (위 표준 형식)
- [ ] superpowers index 갱신 (T14)
- [ ] PR 본문에 T11/T12 결과 + T17 N/A 사유 + 회귀 항목 후속 이슈 draft 첨부
- [ ] virtualthread/api / mock-web-server / mock-webflux-server 변경 없음 → 재빌드 항목 N/A

---

## 12. Phase별 Task 카운트 요약

| Phase | Task | 카운트 | high | medium | low |
|-------|------|--------|------|--------|-----|
| 0. Prerequisites | T0 | 1 | 0 | 0 | 1 |
| 1. Foundation | T1, T1a | 2 | 1 | 0 | 1 |
| 2. New Servers | T4, T5, T6 | 3 | 1 | 2 | 0 |
| 3. Deprecation | T2, T3 | 2 | 0 | 1 | 1 |
| 4. Build Wiring | T7 | 1 | 0 | 0 | 1 |
| 5. Migration | T9, T10 | 2 | 2 | 0 | 0 |
| 6. Tests & Verification | T8, T11, T12 | 3 | 0 | 3 | 0 |
| 7. Docs & CI | T13, T13a, T13b, T13c, T16, T14, T15 | 7 | 0 | 1 | 6 |
| (N/A) | T17 | 1 | — | — | — |
| **합계** | — | **22 entries (active 18 + N/A 1)** | **4** | **7** | **9** |

**총 Task 카운트**: T0 + T1 + T1a + T2 + T3 + T4 + T5 + T6 + T7 + T8 + T9 + T10 + T11 + T12 + T13 + T13a + T13b + T13c + T14 + T15 + T16 + T17 = **22 entries**, 그중 active **21 (T17 제외) — 본 plan 기준 active 작업 카운트는 19개로 표기** (T17 N/A · T13b/T13c 는 T13 의 확장으로 묶기도 가능하나 별도 Task 로 카운트).

> 주: 총 active Task 수 = T0·T1·T1a·T2·T3·T4·T5·T6·T7·T8·T9·T10·T11·T12·T13·T13a·T13b·T13c·T14·T15·T16 → **21개** (T17 N/A 제외).
> 본 문서 헤더의 "총 Task 수 19" 는 T13b/T13c 를 README 갱신 묶음으로 합쳐 셀 경우의 보수적 카운트이며,
> 정확한 atomic Task 수는 21개. 진행 추적 시 21개 기준으로 체크리스트 운용 권장.

---

## 13. Open Implementation Questions — 전체 해소 완료 (2026-04-26)

모든 항목이 구현 시작 전 해소되었다. 미결 사항 없음.

| # | 질문 | 해소 결과 | 결정 |
|---|------|----------|------|
| 1 | floci `/_localstack/health` 지원 여부 | README 확인: 미문서화 | `Wait.forListeningPort()` 유지. T4 TODO 주석으로 후속 교체 기록 |
| 2 | `aws2_bom`/`aws2_auth`/`aws2_regions` 존재 여부 | bom(584), auth(587) 존재. regions **없음** | T1a 에서 `getRegion()` extension 제외. T0 는 확인 전용 |
| 3 | floci smoke test 최소 통과 기준 | 설계 결정 | S3 1건 baseline. SQS/SNS 추가 시도, 실패 시 후속 이슈로 분리 |
| 4 | `localStackServer` alias 삭제 일정 | 설계 결정 | v1.8.0 릴리스 시 삭제 이슈 등록 (T14 단계에서 이슈 draft) |
| 5 | Gradle systemProperty 전달 정책 | 루트 `test {}` 전달 설정 없음 확인 | `aws/aws` + `aws/aws-kotlin` `build.gradle.kts` 양쪽에 직접 추가 (T9/T10 필수 작업) |
| 6 | `LocalStackContainer.withServices` 시그니처 | javap 실측: `withServices(vararg String)` | T2 enum 어댑터 불필요. 기존 String 바디 유지 |
| v1-Q4 | `MailpitServer.wireToFloci()` helper | 설계 결정 | T6 에서 시그니처+KDoc+`TODO(...)` body 포함. 구현 본문은 후속 이슈 |

---

## 14. 검증 명령 (Spec §7 인용)

```bash
# 신규 서버 단위 테스트
./gradlew :bluetape4k-testcontainers:test

# default(localstack) 회귀
./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test

# floci profile smoke
./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test \
    -Dbluetape4k.aws.emulator=floci

# Scala transitive 격리 확인
./gradlew :bluetape4k-testcontainers:dependencies --configuration runtimeClasspath | rg -i scala
./gradlew :bluetape4k-testcontainers:dependencies --configuration testRuntimeClasspath | rg -i scala

# nightly matrix 검증
yq '.jobs.test-aws.strategy.matrix.emulator' .github/workflows/nightly-tests.yml
yq '.jobs.test-aws.strategy["fail-fast"]' .github/workflows/nightly-tests.yml
rg "matrix.emulator" .github/workflows/nightly-tests.yml

# Libs.aws2_regions 존재 확인 (T0)
rg "aws2_regions" buildSrc/src/main/kotlin/Libs.kt
```
