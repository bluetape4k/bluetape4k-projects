# MiniStackServer 설계 스펙

**날짜**: 2026-04-27 **브랜치**: `feat/ministack-server`
**범위**: `testing/testcontainers` 모듈

---

## 1. 배경 및 목적

### 1.1 배경

- **LocalStack Community Edition** — 2026-03-23 아카이브됨. 기존 `LocalStackServer`는 `@Deprecated(WARNING)` 상태.
-

**Floci** — GraalVM Native Image 기반 경량 에뮬레이터 (v1.5.8). KMS DisableKey/EnableKey/Grant, DynamoDB GSI pagination (#587) 등 미지원 기능 다수. `@Deprecated(WARNING)` 상태.

- **MiniStack** — 새 AWS 에뮬레이터. v1.3.14 (2026-04-24), MIT License, 31+ 서비스, ~270MB 이미지, ~30MB RAM, ~2s 기동.
    - Maven: `org.ministack:testcontainers-ministack:0.1.4` (공식 Testcontainers 모듈)
    - Docker: `ministackorg/ministack:1.3.14`
    - 헬스 엔드포인트: `/_ministack/health`
    - Floci 미지원 기능 — KMS 전 기능, DynamoDB GSI 지원 확인됨

### 1.2 목적

1. `MiniStackServer.kt` 구현 — `AwsEmulatorServer` 인터페이스 구현, `MiniStackContainer` 래핑
2. `MiniStackServerTest.kt` — 서버 시작/정상 동작 기본 검증
3. AWS 서비스별 통합 테스트 8종 — FlociServer 서비스 테스트 대응
4. `Libs.kt`, `build.gradle.kts` 의존성 추가
5. `AwsEmulatorServer` KDoc에 MiniStack 언급 추가

---

## 2. 범위

### In-scope

- `MiniStackServer.kt` 메인 소스
- `MiniStackServerTest.kt` 기본 서버 테스트
- AWS 서비스 테스트 8종: CloudWatch, DynamoDB, KMS, Kinesis, S3, SNS, SQS, STS
- `Libs.kt` — MiniStack 의존성 상수 추가
- `testing/testcontainers/build.gradle.kts` — `compileOnly` 의존성 추가
- `AwsEmulatorServer.kt` KDoc 업데이트
- `testing/testcontainers/README.md` + `README.ko.md` 업데이트

### Out-of-scope

- Spring Boot Auto-configuration (별도 PR)
- `@Deprecated` 제거 (LocalStack/Floci/MiniStack 중 안정적인 것이 확인된 후)
- DynamoDB Enhanced Client 심화 테스트

---

## 3. 기술 설계

### 3.1 브레인스토밍 — 접근 방식 비교

#### 접근 방식 A: `MiniStackContainer` 직접 래핑 + `AwsEmulatorServer` 구현 [검토]

```kotlin
class MiniStackServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): MiniStackContainer(imageName), GenericServer, PropertyExportingServer, AwsEmulatorServer {
    ...
}
```

**장점**:

- MiniStack 공식 Testcontainers 모듈 (`testcontainers-ministack:0.1.4`) 사용 → 헬스체크, 초기화 로직 자동 처리
- Floci처럼 `GenericContainer` 직접 래핑하지 않아도 됨
- `MiniStackContainer.getEndpoint()` 활용 가능

**단점**:

- `MiniStackContainer`의 `getEndpoint()` → `awsEndpoint` 이름 충돌 위험 (JVM getter 충돌 패턴)
- 외부 라이브러리 변경 시 영향 받음

#### 접근 방식 B: `GenericContainer<MiniStackServer>` 직접 래핑 (Floci 패턴 동일) [채택]

```kotlin
class MiniStackServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<MiniStackServer>(imageName), GenericServer, PropertyExportingServer, AwsEmulatorServer {
    ...
}
```

**장점**:

- Floci 패턴과 완전히 동일 → 코드베이스 일관성 극대화
- 외부 라이브러리 의존성 없음 (공식 testcontainers-ministack 모듈 불필요)

**단점**:

- 헬스체크, 환경변수 등 MiniStack 특유 설정을 직접 구현해야 함
- 공식 모듈이 업데이트될 때 수동 동기화 필요

#### 접근 방식 C: `MiniStackContainer` 위임 (delegation) 패턴 [기각]

**장점**: 래핑 없이 인터페이스만 구현
**단점**: Kotlin delegation은 GenericContainer 상속 구조와 충돌, Testcontainers가 lifecycle 관리를 상속 기반으로 처리하므로 불가

**채택 결정**: **접근 방식 B** (GenericContainer 직접 래핑)

- Floci 패턴과 동일하여 유지보수 일관성 확보
- `MiniStackContainer`의 내부 구현에 의존하지 않아 안정적
- 헬스 엔드포인트 (`/_ministack/health`)를 직접 Wait 전략으로 설정
- `compileOnly` 의존성으로 선택적 사용 가능

### 3.2 위험 요인 및 대응

| 위험                | 설명                                                          | 대응                                                                                                                                                                                                      |
|---------------------|---------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **포트 충돌**       | MiniStack 기본 포트 4566 — LocalStack/Floci와 동일            | `useDefaultPort=false` 기본값 (동적 포트 할당). `GATEWAY_PORT` 환경변수는 GenericContainer 직접 래핑에서 불필요 — Testcontainers가 랜덤 외부 포트로 매핑                                                  |
| **S3 path-style**   | Floci와 동일하게 virtual-hosted URL 미지원 가능               | 서비스 테스트에 `pathStyleAccessEnabled(true)` 적용                                                                                                                                                       |
| **헬스체크 타이밍** | 2초 기동이지만 CI 환경에서 더 느릴 수 있음 (이미지 pull 포함) | `Wait.forHttp("/_ministack/health").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(60))`. 구현 시 health endpoint 응답 body에 서비스별 준비 상태 필드가 있는지 확인 후 body predicate 추가 검토 |
| **미지원 서비스**   | 아직 31+ 지원이지만 엣지 케이스 존재 가능                     | 실패 테스트에 `@Disabled` + KDoc 알려진 제한사항 문서화                                                                                                                                                   |
| **안정성 불확실**   | v1.3.14이지만 107 릴리스로 활발히 개발 중                     | `@Deprecated(WARNING)` 적용하지 않음 (LocalStack 아카이브 이후 주력 에뮬레이터로 포지셔닝)                                                                                                                |

### 3.3 `MiniStackServer` 클래스 설계

```kotlin
@Suppress("DEPRECATION")
class MiniStackServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<MiniStackServer>(imageName), GenericServer, PropertyExportingServer, AwsEmulatorServer {

    companion object: KLogging() {
        const val IMAGE = "ministackorg/ministack"
        const val TAG = "1.3.14"
        const val NAME = "ministack"
        const val PORT = 4566
        const val DEFAULT_ACCESS_KEY = "test"
        const val DEFAULT_SECRET_KEY = "test"
        const val DEFAULT_REGION = "us-east-1"

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): MiniStackServer { ... }

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): MiniStackServer { ... }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"
    override val awsEndpoint: URI get() = URI.create("http://$host:$port")
    override val awsAccessKey: String = DEFAULT_ACCESS_KEY
    override val awsSecretKey: String = DEFAULT_SECRET_KEY
    override val regionName: String = DEFAULT_REGION
    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "aws-endpoint", "aws-access-key", "aws-secret-key", "region")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "aws-endpoint" to awsEndpoint.toString(),
        "aws-access-key" to awsAccessKey,
        "aws-secret-key" to awsSecretKey,
        "region" to regionName,
    )

    init {
        addExposedPorts(PORT)
        withReuse(reuse)
        // MiniStack 공식 헬스 엔드포인트 사용
        setWaitStrategy(
            Wait.forHttp("/_ministack/health")
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofSeconds(60))
        )
        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun withServices(vararg services: String): MiniStackServer {
        log.debug { "MiniStack enables all services by default. withServices(${services.toList()}) is a no-op." }
        return this
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    object Launcher {
        val miniStack: MiniStackServer by lazy {
            MiniStackServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
```

### 3.4 서비스 테스트 구조

서비스 테스트는 `floci/services/` 패턴을 그대로 따름:

- 경로: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/ministack/services/`
- 테스트 클래스: `FlociXxxTest` → `MiniStackXxxTest` (1:1 대응)
- `floci: FlociServer` → `miniStack: MiniStackServer` 교체
- KMS: `@Disabled` 테스트 제거 (MiniStack은 KMS 전 기능 지원)
- S3: `pathStyleAccessEnabled(true)` 유지 (안전망)
- DynamoDB: GSI 테스트 추가 가능 (Floci #587 제한 없음)

### 3.5 기본 서버 테스트

`MiniStackServerTest.kt` — `FlociServerTest.kt` 패턴 동일:

- 서버 시작 확인
- `awsEndpoint` URI 형식 검증
- `awsAccessKey`, `awsSecretKey`, `regionName` 기본값 확인
- S3 client 연결 테스트 (path-style)

---

## 4. 의존성

### 4.1 Libs.kt 추가

```kotlin
// MiniStack — free MIT-licensed AWS emulator (ministack.org)
const val testcontainers_ministack =
    "org.ministack:testcontainers-ministack:0.1.4"  // https://mvnrepository.com/artifact/org.ministack/testcontainers-ministack
```

> 참고: MiniStack 공식 testcontainers 모듈은 `org.ministack:testcontainers-ministack` 좌표를 사용한다.
> 일반 testcontainersModule () 헬퍼 (org.testcontainers 그룹)와 다르므로 `const` 리터럴로 선언.

### 4.2 build.gradle.kts 추가

```kotlin
// MiniStack for AWS emulation
compileOnly(Libs.testcontainers_ministack)
```

`LocalStack` 의존성 바로 아래 (line ~136)에 추가.

---

## 5. 파일 목록

| 파일                                                                                                                    | 작업                                              |
|-------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| `buildSrc/src/main/kotlin/Libs.kt`                                                                                      | MiniStack 의존성 상수 추가                        |
| `testing/testcontainers/build.gradle.kts`                                                                               | `compileOnly(Libs.testcontainers_ministack)` 추가 |
| `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/MiniStackServer.kt`                            | 신규 생성                                         |
| `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/AwsEmulatorServer.kt`                          | KDoc에 MiniStack 언급 추가                        |
| `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/MiniStackServerTest.kt`                        | 신규 생성                                         |
| `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/ministack/services/MiniStackCloudWatchTest.kt` | 신규 생성                                         |
| `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/ministack/services/MiniStackDynamoDBTest.kt`   | 신규 생성                                         |
| `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/ministack/services/MiniStackKMSTest.kt`        | 신규 생성                                         |
| `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/ministack/services/MiniStackKinesisTest.kt`    | 신규 생성                                         |
| `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/ministack/services/MiniStackS3Test.kt`         | 신규 생성                                         |
| `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/ministack/services/MiniStackSNSTest.kt`        | 신규 생성                                         |
| `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/ministack/services/MiniStackSQSTest.kt`        | 신규 생성                                         |
| `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/ministack/services/MiniStackSTSTest.kt`        | 신규 생성                                         |
| `testing/testcontainers/README.md`                                                                                      | MiniStack 섹션 추가                               |
| `testing/testcontainers/README.ko.md`                                                                                   | MiniStack 섹션 추가                               |

---

## 6. DoD (Definition of Done)

- [ ] `MiniStackServer.kt` 구현 완료 (`AwsEmulatorServer` 인터페이스 전부 구현)
- [ ] `MiniStackServer.Launcher.miniStack` 싱글턴 동작 확인
- [ ] `MiniStackServerTest.kt` 통과
- [ ] 8종 서비스 통합 테스트 모두 통과 (실패 시 `@Disabled` + KDoc 제한사항 문서화)
- [ ] KMS 테스트: `@Disabled` 없이 전체 통과 (MiniStack KMS 전 기능 지원)
- [ ] `Libs.kt` + `build.gradle.kts` 의존성 추가 완료
- [ ] Korean KDoc — 모든 public API
- [ ] README.md + README.ko.md 업데이트
- [ ] Step 6-R 코드 리뷰 (CRITICAL/HIGH 0건)
