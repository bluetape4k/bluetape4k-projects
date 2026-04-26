# AWS 에뮬레이터 전환 설계 (LocalStack → floci + ElasticMQ + Mailpit)

- Issue: #155
- Worktree: `.worktrees/issue-155-aws-emulator`
- Branch: `feat/issue-155-aws-emulator`
- 작성일: 2026-04-26
- 단계: Step 1-D (설계 스펙)

---

## 1. 배경 및 목적

### 1.1 배경

`bluetape4k` 프로젝트는 AWS 통합 테스트(`aws/`, `aws-kotlin/`, `spring-boot3/aws-*`, `spring-boot4/aws-*`)에서 LocalStack(`localstack/localstack:4`)을 단일 에뮬레이터로 사용해 왔다. 그러나 다음과 같은 환경 변화가 발생했다.

| 항목 | 상태 | 일자 |
|------|------|------|
| LocalStack | GitHub repo `localstack/localstack` archived (read-only) | 2026-03-23 |
| MinIO | 핵심 서버 archived, AIStor로 rebrand | 2026-04-25 |
| floci | LocalStack 대체 OSS 에뮬레이터 (latest `1.5.7`, Docker Hub `floci/floci`) | 활발히 개발 중 |
| ElasticMQ | SQS 호환 in-process 서버 (`1.6.12`) | 활발히 유지 보수 |
| Mailpit | SMTP 캡처 + Web UI 메일 테스트 도구 (`1.29`) | 활발히 유지 보수 |

LocalStack/MinIO를 그대로 유지하면 다음 리스크가 누적된다.
- 보안 취약점 패치 정지
- AWS API 신규 동작 미반영 (S3 conditional write, SES v2 등)
- Apple Silicon/ARM64 이미지 업데이트 정지
- bluetape4k 전체 테스트 인프라가 archived 의존성에 묶임

### 1.2 목적

1. **신규 에뮬레이터 도입**: `FlociServer`, `ElasticMqServer`, `MailpitServer`를 `bluetape4k-testcontainers`에 추가한다.
2. **기존 자산 보호**: `LocalStackServer`, `MinIOServer`는 즉시 삭제하지 않고 `@Deprecated(...)`로 표시한다 (한 릴리스 사이클 호환).
3. **마이그레이션 인터페이스 제공**: AWS 테스트 코드가 LocalStack/floci 양쪽을 무리 없이 선택하도록 추상화 레이어를 제공한다.
4. **본 이슈의 범위는 testcontainers + AbstractAwsTest 전환만**: 개별 테스트 파일 마이그레이션은 후속 이슈로 분리한다.

### 1.3 비-목표

- AWS 실서비스 대상 통합 테스트 추가 (현 LocalStack 기반 테스트만 등가 변환)
- floci에서 미지원하는 stateful 서비스(Lambda/RDS/ElastiCache) 도입
- AWS Kotlin SDK / AWS Java SDK 자체 버전 업그레이드
- `aws-mock-bedrock` 등 외부 모킹 도구 추가

---

## 2. 설계 리스크 (Failure Modes)

연구 결과(Step 1-R)로부터 식별된 리스크와 대응 방향이다.

### Risk R1 — floci/Mailpit는 공식 testcontainers 모듈이 없다

**증상**: `LocalStackContainer`처럼 직접 `extends` 가능한 testcontainers 클래스가 없으므로 `GenericContainer<T>` 래핑 패턴(`KeycloakServer`와 동일)을 사용해야 한다.

**리스크**:
- `LocalStackContainer.withServices(...)`처럼 강타입 enum이 없어 사용자가 잘못된 서비스 이름을 넣어도 컴파일 단에서 잡지 못한다.
- `LocalStackContainer.endpoint`처럼 SDK가 신뢰하는 endpoint provider 호환 메서드가 없으면 기존 호출 코드가 깨진다.

**대응**:
- `FlociServer`에 `withServices(vararg services: String)` + `validateServices()` 가드를 둔다.
- `endpoint`/`accessKey`/`secretKey`/`region` API 시그니처를 LocalStackContainer와 호환되도록 노출 (extension function으로 보강).

### Risk R2 — Wire-protocol/credentials 호환성이 보장되지 않는다

**증상**: floci는 LocalStack과 동일한 wire protocol(port 4566)을 표방하지만, 모든 SDK 동작이 일치한다는 보장은 없다. 특히 SES SMTP relay, KMS 키 형식, S3 presigned URL의 host header 처리 등에서 미세한 차이가 있을 수 있다.

**리스크**:
- 하드 스왑(Option A) 시 한 모듈이 깨지면 전체 통합 테스트가 막힌다.
- 마이그레이션 전후 동작 차이를 검증할 회귀 채널이 없다.

**대응**:
- AbstractAwsTest 추상화 레이어(Option B 채택)로 LocalStack/floci를 동시 빌드/실행 가능하게 한다.
- floci 이전에 LocalStackServer 호환 테스트를 한 번 더 그린으로 만든 뒤, FlociServer로 같은 테스트를 다시 돌려 회귀를 비교한다 (후속 이슈에서 진행).

### Risk R3 — Stateful 서비스(Lambda 등) 사용 가능성 차단

**증상**: floci는 일부 stateful 서비스 실행 시 `/var/run/docker.sock` bind-mount가 필요하다. 현재 사용 중인 10개 서비스는 모두 stateless이지만, 향후 Lambda/RDS 도입 시 docker socket 노출이 보안 정책상 문제될 수 있다.

**대응**:
- `FlociServer`에 `withDockerSocket()` 옵션 메서드를 두되 기본값은 비활성.
- KDoc 및 README에 "stateful 서비스 사용 시에만 docker socket 마운트" 정책 명시.

### Risk R4 — ElasticMQ Scala 의존성 폭발

**증상**: ElasticMQ 임베드(`elasticmq-rest-sqs_2.13:1.6.12`) 도입 시 Scala 표준 라이브러리(`scala-library`, `scala-reflect`) 약 6MB가 추가된다.

**리스크**:
- bluetape4k는 현재 Scala 의존성이 0이다 → 첫 도입의 영향 범위가 크다.
- `bluetape4k-testcontainers`가 Scala를 transitive로 끌고 가면 모든 모듈에 영향.

**대응**:
- ElasticMQ 의존성은 `compileOnly` + `testRuntimeOnly`로 묶어 production transitive에서 제외.
- `ElasticMqServer`는 별도 패키지 `io.bluetape4k.testcontainers.aws.embedded`로 격리하여 사용자가 명시적으로 import할 때만 활성화되도록 한다.

### Risk R5 — Mailpit는 SES API를 구현하지 않는다

**증상**: Mailpit은 SMTP 트래픽 캡처 도구이지 AWS SES API 서버가 아니다. 따라서 `SesClient.sendEmail()`을 직접 받지는 못한다.

**리스크**:
- 사용자가 "Mailpit 띄우면 SES 테스트가 된다"고 오해할 수 있다.
- Floci SES + Mailpit SMTP relay 조합이 필수인데 이 결합 방식을 오해하면 메일 캡처가 작동하지 않는다.

**대응**:
- KDoc에 "Mailpit은 SMTP 캡처용이며, SES API는 floci가 담당하고 floci → Mailpit으로 SMTP relay 설정해야 한다"고 명시.
- `MailpitServer.Launcher`에 `wireToFloci(floci: FlociServer)` helper를 제공하여 페어링 패턴을 표준화.

### Risk R6 — Singleton Launcher와 두 에뮬레이터의 동시성

**증상**: 기존 `LocalStackServer.Launcher.localStack`은 `lazy { ... }` JVM 싱글턴이다. AbstractAwsTest를 추상화하면서 두 에뮬레이터를 모두 띄우면 testcontainers reuse 캐시가 충돌하거나 포트가 중복될 수 있다.

**대응**:
- 빌드 단위에서는 둘 중 하나만 활성화 (시스템 프로퍼티 또는 Gradle property로 선택).
- 기본은 LocalStack(현 동작) 유지, CI 매트릭스에서 floci profile을 별도로 추가한다 (T16에서 도입).
- AbstractAwsTest의 `awsEmulator` lazy는 기존 `LocalStackServer.Launcher.localStack` 싱글턴에 위임하여 이중 초기화를 방지한다.

### Risk R7 — `AwsEmulatorServer` 인터페이스의 Java getter 충돌

**증상**: `LocalStackServer`는 `LocalStackContainer`를 상속하며, parent class는 Java로 작성된 `getEndpoint(): URI`, `getAccessKey(): String`, `getSecretKey(): String`, `getRegion(): String` 메서드를 노출한다. Kotlin에서 인터페이스에 `val endpoint: URI`, `val accessKey: String` 등을 선언하면 컴파일러는 자동으로 `getEndpoint()` getter를 기대하지만, Java 부모 클래스의 동명 메서드와 시그니처 충돌이 발생한다.

**대응**:
- 인터페이스 구현체는 명시적 override로 Java getter에 위임한다 (§4.6 참조).
- `AwsEmulatorServer` 인터페이스 KDoc에 "Java testcontainers 클래스를 상속하는 구현체는 충돌 회피를 위해 모든 추상 프로퍼티를 명시적으로 override해야 한다"고 명시.

### Risk R8 — AWS SDK 의존성의 leaky abstraction

**증상**: `bluetape4k-testcontainers` 모듈은 현재 AWS SDK 의존성이 0이다. 인터페이스에 `credentialsProvider(): AwsCredentialsProvider`를 선언하면 모든 testcontainers 소비자(비-AWS 모듈 포함)가 AWS SDK를 classpath에 두어야 한다.

**대응**:
- 인터페이스에서 `credentialsProvider()` 메서드를 제거하고, 대신 별도 extension 파일 `AwsEmulatorServerExtensions.kt`에서 `compileOnly(Libs.aws2_auth)`로 격리.
- 각 구현체(LocalStackServer, FlociServer)는 자체적으로 `getCredentialProvider()` 메서드를 가지되 공통 인터페이스 의무는 아니다.

---

## 3. 접근법 비교

핵심 결정 지점: **AbstractAwsTest가 LocalStack/floci를 어떻게 선택할 것인가?**

### Option A — Hard Replace (LocalStackServer를 FlociServer로 그냥 교체)

**개요**: `AbstractAwsTest`의 `localStackServer` 필드를 `flociServer: FlociServer`로 교체하고, 기존 `LocalStackServer`는 즉시 삭제 또는 `@Deprecated`만 표시한다.

| 장점 | 단점 |
|------|------|
| 변경 범위 최소, 코드가 단순 | 회귀 발생 시 즉시 롤백 불가 |
| 추상화 오버헤드 없음 | 호환성 사이클이 없어 외부 사용자(라이브러리 소비자)에게 즉시 영향 |
| | wire-protocol 차이로 발생하는 미묘한 회귀를 LocalStack과 비교 검증 불가능 |

**평가**: bluetape4k는 외부에 publish되는 라이브러리이며, archived dependency 마이그레이션은 신중한 전환이 표준이다. 한 릴리스 사이클의 `@Deprecated` 호환을 두는 관행과 충돌. **채택 부적합**.

### Option B — `AwsEmulatorServer` 인터페이스 (LocalStackServer/FlociServer 모두 구현)

**개요**: 두 서버가 공통으로 노출해야 하는 contract를 인터페이스로 추출하고, `AbstractAwsTest`는 인터페이스를 통해 동작한다. Launcher 측에서 어떤 구현을 띄울지 결정.

```kotlin
interface AwsEmulatorServer : GenericServer, PropertyExportingServer {
    val endpoint: java.net.URI
    val accessKey: String
    val secretKey: String
    val regionName: String
    fun withServices(vararg services: String): AwsEmulatorServer
}
```

> AWS SDK 의존성은 인터페이스에 포함하지 않는다 (Risk R8 참조). `AwsCredentialsProvider` 변환은 `AwsEmulatorServerExtensions.kt`의 extension function으로 별도 제공.

| 장점 | 단점 |
|------|------|
| LocalStack/floci를 동시 지원 → 회귀 비교 가능 | 인터페이스 진화 시 두 구현 모두 손봐야 함 |
| 외부 사용자도 한 인터페이스만 의존 → SDK 전환 비용 낮음 | 인터페이스 단계에서 LocalStack 고유 API(`withNetworkAliases` 등)가 가려짐 → escape hatch 필요 |
| Deprecation 사이클 자연스럽게 표현 가능 | 단순 swap보다 작업량 많음 |

**평가**: bluetape4k 컨벤션(`GenericServer`, `PropertyExportingServer`)과 동질적. 회귀 검증 채널을 제공한다. **채택**.

### Option C — System Property 플래그(`bluetape4k.aws.emulator=floci|localstack`)만 도입

**개요**: 인터페이스 없이 `AbstractAwsTest.companion`에서 `System.getProperty("bluetape4k.aws.emulator", "localstack")` 분기로 두 에뮬레이터를 선택. 결과 객체는 `Any` 또는 sealed class.

| 장점 | 단점 |
|------|------|
| 런타임 플래그로 가장 빠르게 toggle 가능 | 분기 로직이 사용 측마다 흩어짐 → DRY 위반 |
| 인터페이스 추상화 없음 → 빠른 도입 | 컴파일 타임 안전성 부족, sealed class로 보강 시 결국 인터페이스 패턴과 유사 |
| | 사용자 코드에서도 분기를 노출해야 함 |

**평가**: 플래그 자체는 Option B와 결합해도 되지만, 단독으로 쓰면 코드가 분산된다. **Option B에 흡수**(시스템 프로퍼티는 Launcher 내부에서 활용).

### 채택 — Option B + Option C(보조)

- 공통 contract: `AwsEmulatorServer` 인터페이스 (AWS SDK 비-의존)
- `LocalStackServer`/`FlociServer` 모두 인터페이스 구현
- `AbstractAwsTest.companion`에 `awsEmulator: AwsEmulatorServer` 단일 필드 도입
- 기본값 = LocalStackServer (호환)
- `-Dbluetape4k.aws.emulator=floci` 시 FlociServer 사용 (CI 매트릭스용)
- 한 릴리스 사이클 후 default를 floci로 swap → LocalStack 삭제 (별도 이슈)

### 보조 컴포넌트: ElasticMqServer는 `AwsEmulatorServer`를 구현하지 않는다

`ElasticMqServer`는 **`AwsEmulatorServer`를 구현하지 않는다**. Docker 기반이 아닌 JVM 내장 서버이며 SQS 전용 API만 제공하기 때문이다. 따라서 `AbstractAwsTest`의 교체 대상이 아니라, **SQS 전용 단위 테스트에서 Docker 없이 사용하는 보조 유틸리티**로 위치한다. 사용자는 명시적으로 `ElasticMqServer().use { ... }` 형태로 호출하며, AWS SDK의 `SqsClient`를 위한 endpoint URI/credentials만 노출한다.

---

## 4. 상세 설계

### 4.1 패키지 구조

```
testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/
├── aws/
│   ├── AwsEmulatorServer.kt              (NEW) 공통 인터페이스 (AWS SDK 비-의존)
│   ├── AwsEmulatorServerExtensions.kt    (NEW) AWS SDK v2 변환 extension (compileOnly)
│   ├── LocalStackServer.kt               (변경) AwsEmulatorServer 구현 + @Deprecated
│   ├── FlociServer.kt                    (NEW) GenericContainer 래퍼
│   └── embedded/
│       └── ElasticMqServer.kt            (NEW) 임베드 SQS 서버 (AwsEmulatorServer 비-구현)
├── mail/                                 (NEW)
│   └── MailpitServer.kt                  (NEW) SMTP 캡처 GenericContainer
└── storage/
    └── MinIOServer.kt                    (변경) @Deprecated 마킹만
```

### 4.2 `AwsEmulatorServer` 인터페이스

```kotlin
package io.bluetape4k.testcontainers.aws

import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import java.net.URI

/**
 * AWS 호환 에뮬레이터(`LocalStack`, `floci` 등)의 공통 계약.
 *
 * 모든 구현체는 다음을 제공해야 한다:
 *  - AWS SDK가 endpoint override로 사용할 수 있는 [endpoint] URI
 *  - 정적 자격 증명 ([accessKey], [secretKey])
 *  - AWS region 문자열 ([regionName])
 *  - [io.bluetape4k.testcontainers.GenericServer] / [PropertyExportingServer] 계약
 *
 * ### 구현 시 주의 사항
 *
 * 1. **Java getter 충돌**: 본 인터페이스를 구현하는 클래스가 Java로 작성된 testcontainers
 *    부모 클래스(`LocalStackContainer` 등)를 상속하는 경우, Kotlin 컴파일러가 자동 생성하는
 *    `getEndpoint()` getter가 Java 부모의 동명 메서드와 충돌한다. 모든 추상 프로퍼티는
 *    명시적 `override get() = this.getXxx()` 위임으로 충돌을 해소해야 한다.
 *    예: `override val endpoint: URI get() = this.getEndpoint()`
 *
 * 2. **AWS SDK 의존성 격리**: 본 인터페이스는 의도적으로 AWS SDK 타입을 노출하지 않는다.
 *    `AwsCredentialsProvider` 변환이 필요하면 [AwsEmulatorServerExtensions]의 extension
 *    function을 사용하라 (이는 `compileOnly(Libs.aws2_auth)` 의존성을 요구한다).
 *
 * 구현체는 service 활성화 방식이 서로 다르므로 [withServices]는 호환을 위한
 * 권고 시그니처로만 둔다.
 */
interface AwsEmulatorServer : GenericServer, PropertyExportingServer {

    /** AWS SDK가 endpoint override로 사용할 URI (예: `http://localhost:32001`) */
    val endpoint: URI

    /** 자격 증명 access key (보통 `test`) */
    val accessKey: String

    /** 자격 증명 secret key (보통 `test`) */
    val secretKey: String

    /** AWS region 문자열 (예: `us-east-1`) */
    val regionName: String

    /**
     * 활성화할 AWS 서비스 이름 목록 (예: `s3`, `sqs`, `sns`).
     * 구현체에 따라 동작 시점(컨테이너 시작 전 vs 런타임)이 다를 수 있다.
     */
    fun withServices(vararg services: String): AwsEmulatorServer
}
```

### 4.2.1 `AwsEmulatorServerExtensions` (분리 파일, AWS SDK 의존)

```kotlin
package io.bluetape4k.testcontainers.aws

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region

/**
 * [AwsEmulatorServer]를 AWS SDK v2 타입으로 변환하는 extension.
 *
 * 본 파일은 `compileOnly(Libs.aws2_auth)` + `compileOnly(Libs.aws2_regions)`를
 * 요구한다. AWS SDK가 classpath에 없는 모듈에서는 extension 자체를 호출하지 않으면
 * NoClassDefFoundError가 발생하지 않는다.
 */
fun AwsEmulatorServer.getCredentialProvider(): StaticCredentialsProvider =
    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

fun AwsEmulatorServer.getRegion(): Region = Region.of(regionName)
```

### 4.3 `FlociServer`

```kotlin
package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import java.net.URI

/**
 * floci AWS 에뮬레이터 ([floci.io](https://github.com/floci-io/floci))
 *
 * - LocalStack과 wire-protocol 호환 (port 4566)
 * - Docker Hub 이미지: `floci/floci:1.5.7`
 * - 기본 stateless 서비스만 지원: stateful 서비스(Lambda 등)는 [withDockerSocket]을 명시 활성화
 *
 * ```kotlin
 * val server = FlociServer().apply {
 *     withServices("s3", "sqs", "sns")
 *     start()
 * }
 * ```
 */
class FlociServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
) : GenericContainer<FlociServer>(imageName), AwsEmulatorServer {

    companion object : KLogging() {
        const val IMAGE = "floci/floci"
        const val NAME = "floci"
        const val TAG = "1.5.7"
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
        ): FlociServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            return FlociServer(DockerImageName.parse(image).withTag(tag), useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): FlociServer = FlociServer(imageName, useDefaultPort, reuse)
    }

    private val activeServices = mutableSetOf<String>()
    private var dockerSocketEnabled = false

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"
    override val endpoint: URI get() = URI.create(url)
    override val accessKey: String = DEFAULT_ACCESS_KEY
    override val secretKey: String = DEFAULT_SECRET_KEY
    override val regionName: String = DEFAULT_REGION
    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "endpoint", "access-key", "secret-key", "region", "services")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "endpoint" to endpoint.toString(),
        "access-key" to accessKey,
        "secret-key" to secretKey,
        "region" to regionName,
        "services" to activeServices.joinToString(","),
    )

    /**
     * AWS SDK v2 자격 증명 제공자 — concrete 메서드(인터페이스 의무 아님).
     * AWS SDK 비-의존 모듈은 본 메서드를 호출하지 않으면 안전.
     */
    fun getCredentialProvider(): StaticCredentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

    init {
        addExposedPorts(PORT)
        withReuse(reuse)
        // floci health check endpoint가 공식 문서에 확정되지 않아 안전 fallback으로 listening port 사용.
        // TODO: floci가 /_localstack/health 를 지원하면 Wait.forHttp("/_localstack/health") 로 교체.
        setWaitStrategy(Wait.forListeningPort())
        if (useDefaultPort) exposeCustomPorts(PORT)
    }

    /**
     * floci는 모든 지원 서비스가 항상 활성화되므로 SERVICES env var이 존재하지 않는다.
     * 이 메서드는 AwsEmulatorServer 인터페이스 호환을 위해 존재하며,
     * 내부적으로는 서비스 이름을 로깅 목적으로만 저장한다 (실제 컨테이너 동작에 영향 없음).
     */
    override fun withServices(vararg services: String): FlociServer = apply {
        services.map { it.lowercase().trim() }
            .onEach { require(it.isNotBlank()) { "service name must not be blank" } }
            .forEach { activeServices += it }
        // floci는 서비스 선택 env var 없음 — 모든 서비스 항상 활성
    }

    /** stateful 서비스(Lambda/RDS 등) 사용 시에만 docker socket을 마운트한다. */
    fun withDockerSocket(): FlociServer = apply {
        dockerSocketEnabled = true
        withFileSystemBind("/var/run/docker.sock", "/var/run/docker.sock")
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    object Launcher {
        val services = listOf(
            "cloudwatch", "logs", "dynamodb", "kinesis", "kms",
            "s3", "ses", "sns", "sqs", "sts",
        )

        val floci: FlociServer by lazy { getFloci(*services.toTypedArray()) }

        fun getFloci(vararg services: String): FlociServer =
            FlociServer().apply {
                withServices(*services)
                start()
                ShutdownQueue.register(this)
            }
    }
}
```

### 4.4 `ElasticMqServer` (임베드 SQS — `AwsEmulatorServer` 비-구현)

> **중요**: `ElasticMqServer`는 **`AwsEmulatorServer` 인터페이스를 구현하지 않는다**.
> Docker 기반이 아닌 JVM 내장 서버이며 SQS 전용이다. AbstractAwsTest의 교체 대상이
> 아니라, SQS 전용 단위 테스트에서 Docker 없이 사용하는 보조 유틸리티이다.

```kotlin
package io.bluetape4k.testcontainers.aws.embedded

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.utils.ShutdownQueue
import org.elasticmq.rest.sqs.SQSRestServer
import org.elasticmq.rest.sqs.SQSRestServerBuilder
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import java.net.URI
import java.net.ServerSocket

/**
 * 임베드(in-process) SQS 호환 서버. Docker 미사용.
 *
 * - Lightweight 단위 테스트 또는 Docker 의존을 줄이고 싶은 환경에 적합
 * - SQS 호환 API (queue 생성, send, receive, delete, attribute)
 * - `region`/`accessKey`는 형식적이며 임의 값 사용 가능
 * - `AwsEmulatorServer` 인터페이스는 구현하지 않는다 (SQS 전용)
 *
 * ```kotlin
 * ElasticMqServer().use { mq ->
 *     mq.start()
 *     val sqs = SqsClient.builder().endpointOverride(mq.endpoint).build()
 * }
 * ```
 */
class ElasticMqServer private constructor(
    private val requestedPort: Int,
) : AutoCloseable, PropertyExportingServer {

    companion object : KLogging() {
        const val NAME = "elasticmq"
        const val DEFAULT_HOST = "localhost"
        const val DEFAULT_REGION = "elasticmq"
        const val DEFAULT_ACCESS_KEY = "x"
        const val DEFAULT_SECRET_KEY = "x"

        /** OS가 빈 포트를 할당하도록 한다 (테스트 격리). */
        private fun pickFreePort(): Int = ServerSocket(0).use { it.localPort }

        @JvmStatic
        operator fun invoke(port: Int = pickFreePort()): ElasticMqServer = ElasticMqServer(port)
    }

    private var server: SQSRestServer? = null
    val host: String = DEFAULT_HOST
    val port: Int get() = requestedPort
    val url: String get() = "http://$host:$port"
    val endpoint: URI get() = URI.create(url)

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "endpoint", "access-key", "secret-key", "region")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "endpoint" to endpoint.toString(),
        "access-key" to DEFAULT_ACCESS_KEY,
        "secret-key" to DEFAULT_SECRET_KEY,
        "region" to DEFAULT_REGION,
    )

    fun getCredentialProvider(): StaticCredentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY))

    fun start(): ElasticMqServer = apply {
        check(server == null) { "ElasticMqServer already started" }
        server = SQSRestServerBuilder
            .withPort(requestedPort)
            .withInterface(DEFAULT_HOST)
            .start()
        writeToSystemProperties()
    }

    override fun close() {
        server?.stopAndWait()
        server = null
    }

    object Launcher {
        val elasticmq: ElasticMqServer by lazy {
            ElasticMqServer().apply {
                start()
                ShutdownQueue.register(AutoCloseable { close() })
            }
        }
    }
}
```

> ⚠ Scala 의존(`org.elasticmq:elasticmq-rest-sqs_2.13:1.6.12`)은 **`compileOnly` + `testRuntimeOnly`** 로만 노출하여 transitive에서 격리한다.

### 4.5 `MailpitServer` (SMTP 캡처)

```kotlin
package io.bluetape4k.testcontainers.mail

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * Mailpit ([axllent/mailpit](https://github.com/axllent/mailpit))
 *
 * SMTP 트래픽 캡처 + Web UI/HTTP API 제공. AWS SES API 자체는 구현하지 않는다.
 *
 * SES 통합 테스트 시에는 [io.bluetape4k.testcontainers.aws.FlociServer]가 SES API를
 * 처리하고, floci → Mailpit으로 SMTP relay하도록 결합한다.
 */
class MailpitServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
) : GenericContainer<MailpitServer>(imageName), GenericServer, PropertyExportingServer {

    companion object : KLogging() {
        const val IMAGE = "axllent/mailpit"
        const val TAG = "v1.29"
        const val NAME = "mailpit"
        const val SMTP_PORT = 1025
        const val UI_PORT = 8025

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): MailpitServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            return MailpitServer(DockerImageName.parse(image).withTag(tag), useDefaultPort, reuse)
        }
    }

    val smtpPort: Int get() = getMappedPort(SMTP_PORT)
    val uiPort: Int get() = getMappedPort(UI_PORT)
    override val port: Int get() = smtpPort
    override val url: String get() = "smtp://$host:$smtpPort"

    val webUiUrl: String get() = "http://$host:$uiPort"
    val apiUrl: String get() = "$webUiUrl/api/v1"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "smtp-port", "ui-port", "smtp-url", "web-url", "api-url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "smtp-port" to smtpPort.toString(),
        "ui-port" to uiPort.toString(),
        "smtp-url" to url,
        "web-url" to webUiUrl,
        "api-url" to apiUrl,
    )

    init {
        addExposedPorts(SMTP_PORT, UI_PORT)
        withReuse(reuse)
        setWaitStrategy(Wait.forHttp("/").forPort(UI_PORT).forStatusCode(200))
        if (useDefaultPort) exposeCustomPorts(SMTP_PORT, UI_PORT)
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    object Launcher {
        val mailpit: MailpitServer by lazy {
            MailpitServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
```

### 4.6 `LocalStackServer` retrofit + `MinIOServer` Deprecation

`LocalStackServer`는 Java 부모 클래스 `LocalStackContainer`의 getter와 충돌하지 않도록 모든 인터페이스 프로퍼티를 명시적으로 override해야 한다.

```kotlin
@Deprecated(
    message = "LocalStack 프로젝트가 archived(2026-03-23). 신규 코드는 FlociServer 사용 권장. " +
              "한 릴리스 사이클(다음 minor) 후 삭제 예정.",
    replaceWith = ReplaceWith(
        "FlociServer",
        "io.bluetape4k.testcontainers.aws.FlociServer"
    ),
    level = DeprecationLevel.WARNING,
)
class LocalStackServer ... : LocalStackContainer(...), AwsEmulatorServer {

    // ─── Java getter 충돌 회피: 명시적 override 위임 ─────────────────────
    // LocalStackContainer (Java) 부모 클래스의 getEndpoint(): URI / getAccessKey(): String /
    // getSecretKey(): String / getRegion(): String 메서드와의 시그니처 충돌을 명시 override로 해소.
    override val endpoint: URI get() = this.getEndpoint()
    override val accessKey: String get() = this.getAccessKey()
    override val secretKey: String get() = this.getSecretKey()
    override val regionName: String get() = this.getRegion()
    // ─────────────────────────────────────────────────────────────────

    override fun withServices(vararg services: String): LocalStackServer = apply {
        // LocalStackContainer.withServices는 enum-typed라 String 호환 변환 어댑터 필요
        services.forEach { name ->
            val svc = LocalStackContainer.Service.valueOf(name.uppercase().replace('-', '_'))
            super.withServices(svc)
        }
    }

    // 기존 getCredentialProvider() 시그니처 유지 (외부 호환)
    fun getCredentialProvider(): StaticCredentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
}
```

`MinIOServer`:
```kotlin
@Deprecated(
    message = "MinIO core가 archived(2026-04-25), AIStor로 rebrand. S3 통합 테스트는 FlociServer 사용 권장.",
    replaceWith = ReplaceWith(
        "FlociServer",
        "io.bluetape4k.testcontainers.aws.FlociServer"
    ),
    level = DeprecationLevel.WARNING,
)
class MinIOServer ... { ... }
```

### 4.7 `AbstractAwsTest` 마이그레이션

`aws/aws/src/test/kotlin/io/bluetape4k/aws/AbstractAwsTest.kt`:

```kotlin
abstract class AbstractAwsTest {
    companion object : KLogging() {
        val services = listOf(
            "cloudwatch", "logs", "dynamodb", "kinesis", "kms",
            "s3", "ses", "sns", "sqs", "sts",
        )

        private val emulatorKind: String =
            System.getProperty("bluetape4k.aws.emulator", "localstack").lowercase()

        /**
         * 활성 AWS 에뮬레이터. JVM 내 단일 인스턴스를 보장하기 위해 기존 Launcher 싱글턴에 위임.
         * 이중 초기화 방지: getXxx() 호출이 아니라 .Launcher.xxx 객체에 직접 위임.
         */
        @JvmStatic
        val awsEmulator: AwsEmulatorServer by lazy {
            when (emulatorKind) {
                "floci" -> FlociServer.Launcher.floci          // 기존 lazy singleton 위임
                else    -> LocalStackServer.Launcher.localStack // 기존 lazy singleton 위임
            }
        }

        // 기존 호환 alias (기존 테스트 코드 변경 최소화)
        @JvmStatic
        @Deprecated(
            "awsEmulator 사용 권장",
            replaceWith = ReplaceWith("awsEmulator"),
        )
        val localStackServer: LocalStackServer
            get() = awsEmulator as? LocalStackServer
                ?: error("emulator is not LocalStack: $emulatorKind")

        // SDK helper 확장 (AWS SDK v2)
        fun AwsEmulatorServer.region(): Region = Region.of(this.regionName)

        val AwsEmulatorServer.credentialsProvider
            get() = staticCredentialsProviderOf(this.accessKey, this.secretKey)

        @JvmStatic protected val faker = Fakers.faker
        @JvmStatic protected fun randomString(): String = Fakers.randomString(256, 2048)
    }
}
```

#### aws-kotlin 측 마이그레이션 (T10 범위)

`aws/aws-kotlin/AbstractAwsTest.kt`는 현재 extension function들을 `LocalStackContainer` receiver에 정의한다 (예: `LocalStackContainer.endpointUrl`). `awsEmulator: AwsEmulatorServer` 타입으로 전환하면 호출이 컴파일 깨진다.

**해결 방안**:
1. 새 extension function은 `AwsEmulatorServer` receiver로 재정의:
   ```kotlin
   val AwsEmulatorServer.endpointUrl: Url
       get() = Url.parse(this.endpoint.toString())

   val AwsEmulatorServer.kotlinCredentialsProvider: AwsCredentialsProvider
       get() = StaticCredentialsProvider {
           accessKeyId = this@kotlinCredentialsProvider.accessKey
           secretAccessKey = this@kotlinCredentialsProvider.secretKey
       }

   val AwsEmulatorServer.kotlinRegion: String
       get() = this.regionName
   ```
2. 기존 `LocalStackContainer.endpointUrl` extension은 같은 PR에서 `@Deprecated`로 표시 (한 릴리스 사이클 호환).
3. AbstractAwsTest의 `awsEmulator` 필드는 Java 측과 동일하게 Launcher 싱글턴에 위임.

### 4.8 의존성 변경 (`buildSrc/Libs.kt`)

```kotlin
// floci는 image-only — 별도 Maven artifact 없음. 주석으로 명시.
// const val floci_image = "floci/floci:1.5.7"

// ElasticMQ embedded SQS
const val elasticmq = "org.elasticmq:elasticmq-rest-sqs_2.13:1.6.12"

// Mailpit은 image-only — 별도 artifact 없음.
// const val mailpit_image = "axllent/mailpit:v1.29"
```

`testing/testcontainers/build.gradle.kts`:

```kotlin
// 기존 LocalStack/MinIO는 호환 유지
compileOnly(Libs.testcontainers_localstack)
compileOnly(Libs.testcontainers_minio)
compileOnly(Libs.minio)

// AWS SDK v2 — AwsEmulatorServerExtensions.kt 전용 (인터페이스 자체는 비-의존)
// 본 의존성은 extension 파일에서만 사용되며, 인터페이스/구현체는 AWS SDK 없이도 컴파일 가능.
compileOnly(platform(Libs.aws2_bom))
compileOnly(Libs.aws2_auth)
compileOnly(Libs.aws2_regions)

// ElasticMQ (Scala transitive 격리)
compileOnly(Libs.elasticmq)
testRuntimeOnly(Libs.elasticmq)
```

### 4.9 시스템 프로퍼티 export 키 명세

| Server | namespace | keys |
|--------|-----------|------|
| FlociServer | `floci` | `host, port, url, endpoint, access-key, secret-key, region, services` |
| ElasticMqServer | `elasticmq` | `host, port, url, endpoint, access-key, secret-key, region` |
| MailpitServer | `mailpit` | `host, smtp-port, ui-port, smtp-url, web-url, api-url` |

모두 `testcontainers.<namespace>.<key>` 형식 (`PropertyExportingServer` 컨벤션 준수).

---

## 5. 태스크 초안

| # | Task | 산출물 | 종속성 |
|---|------|--------|--------|
| T1 | `AwsEmulatorServer` 인터페이스 작성 + KDoc (Java getter 충돌 / SDK 의존성 격리 가이드 포함) | `testcontainers/aws/AwsEmulatorServer.kt` | — |
| T1a | `AwsEmulatorServerExtensions.kt` 작성 (AWS SDK v2 변환 extension, compileOnly) | `testcontainers/aws/AwsEmulatorServerExtensions.kt` | T1 |
| T2 | `LocalStackServer`에 `AwsEmulatorServer` 구현 + 명시 override 위임 + `@Deprecated` | LocalStackServer.kt 수정 | T1 |
| T3 | `MinIOServer`에 `@Deprecated` 추가 | MinIOServer.kt 수정 | — |
| T4 | `FlociServer` 신규 작성 (Wait.forListeningPort fallback) | `testcontainers/aws/FlociServer.kt` | T1 |
| T5 | `ElasticMqServer` 신규 작성 + Libs.kt에 `elasticmq` 등록 (`AwsEmulatorServer` 비-구현 명시) | `testcontainers/aws/embedded/ElasticMqServer.kt`, Libs.kt | — |
| T6 | `MailpitServer` 신규 작성 (`mail/` 패키지 신설) | `testcontainers/mail/MailpitServer.kt` | — |
| T7 | testcontainers `build.gradle.kts`에 의존성 추가 (AWS SDK v2 compileOnly, elasticmq compileOnly) | `testing/testcontainers/build.gradle.kts` | T4, T5 |
| T8 | testcontainers 단위 테스트 작성: FlociServer/ElasticMqServer/MailpitServer 각각 start/stop + propertyKeys/properties 검증 | `testing/testcontainers/src/test/kotlin/...` | T4, T5, T6 |
| T9 | `aws/aws/AbstractAwsTest`에 `awsEmulator` 도입, 기존 Launcher 싱글턴에 위임 (이중 초기화 방지), `localStackServer` alias 호환 유지 | `aws/aws/src/test/kotlin/io/bluetape4k/aws/AbstractAwsTest.kt` | T1, T2, T4 |
| T10 | `aws/aws-kotlin/AbstractAwsTest` 동일 패턴 적용 + `AwsEmulatorServer.endpointUrl`/`kotlinCredentialsProvider` extension 신규 정의 + 기존 `LocalStackContainer.endpointUrl` 등 extension `@Deprecated` 처리 | `aws/aws-kotlin/src/test/kotlin/io/bluetape4k/aws/kotlin/AbstractAwsTest.kt` 외 extension 파일 | T1, T2, T4 |
| T11 | 회귀 검증: 기본(localstack) profile로 `./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test` 통과 확인 | 테스트 결과 로그 | T9, T10 |
| T12 | 회귀 검증: floci profile (`-Dbluetape4k.aws.emulator=floci`) 로 동일 모듈 smoke test 통과 확인 (실패 항목은 후속 이슈로 분리) | 테스트 결과 로그 | T9, T10 |
| T13 | README.md / README.ko.md 업데이트: `bluetape4k-testcontainers` 모듈에 floci/ElasticMQ/Mailpit 섹션 추가, LocalStack/MinIO Deprecated 표기 | README 2개 | T4~T6 |
| T13a | 루트 `CLAUDE.md` 업데이트 — Key Design Patterns 섹션에 `AwsEmulatorServer` 인터페이스와 LocalStack→floci 마이그레이션 정책 문서화, `testing/` 모듈 그룹 항목 갱신 | `CLAUDE.md` | T1, T4 |
| T14 | `docs/superpowers/index/2026-04.md` 항목 추가 + INDEX 카운트 갱신 | 인덱스 파일 2개 | 전 작업 |
| T15 | `/wiki-update`로 본 spec을 wiki 인덱싱 | wiki 페이지 | 본 spec 작성 후 |
| T16 | `.github/workflows/nightly-tests.yml` 수정 — `test-aws` job에 floci profile 매트릭스 추가 (`-Dbluetape4k.aws.emulator=floci`), `continue-on-error: true`로 스모크 검증 | `.github/workflows/nightly-tests.yml` | T9, T10 |
| T17 | ~~`ci.yml` AWS job 동기화 검토~~ **N/A** — `ci.yml`에 AWS 전용 job 없음. nightly-tests.yml의 `test-aws` job만 존재하므로 T16에서 완결. | N/A | T16 |

> Plan 단계(Step 2)에서 위 태스크를 phase로 묶고 acceptance criteria/예상 작업 시간을 보강한다.
> **총 18개 태스크** (T1, T1a, T2~T13, T13a, T14, T15, T16, T17). T17은 N/A.

---

## 6. 범위 외 (Out of Scope)

다음 항목은 본 이슈에서 다루지 않으며, 별도 이슈로 분리한다.

| 항목 | 사유 |
|------|------|
| 개별 AWS 모듈 테스트 파일(`io/bluetape4k/aws/s3/...` 등)의 floci 전환 | 모듈 단위 회귀 검증이 필요하여 분리 |
| `spring-boot3/aws-*`, `spring-boot4/aws-*` 의 floci 전환 | Spring auto-configuration 영향 큰 별도 작업 |
| Lambda/RDS/ElastiCache 등 stateful 서비스 도입 | docker socket 노출, security review 필요 |
| LocalStack/MinIO 코드 완전 삭제 | 한 릴리스 사이클 호환 후 진행 (`v1.8.0` 후보) |
| AWS SDK v2 / AWS Kotlin SDK 자체 버전 업그레이드 | 의존 변경이 대규모 — 별도 이슈 |
| `aws-mock-bedrock` 등 LLM 관련 모킹 도구 도입 | bluetape4k-llm 모듈에서 별도 검토 |

---

## 7. 검증 계획 (Step 3 plan에서 정밀화)

### Acceptance Criteria

- [ ] `./gradlew :bluetape4k-testcontainers:test`가 신규 서버 단위 테스트 포함 전수 통과
- [ ] `./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test` 가 default profile(`localstack`)로 회귀 없이 통과
- [ ] floci profile smoke test에서 SQS/SNS/S3 기본 시나리오 1건 이상 통과 (회귀 항목은 후속 이슈에 등록)
- [ ] LocalStackServer/MinIOServer 호출부에서 `@Deprecated` 경고 정상 노출 (컴파일 에러 없음)
- [ ] README/README.ko.md에 신규 서버 섹션과 deprecation 안내 반영
- [ ] 루트 `CLAUDE.md`에 `AwsEmulatorServer` 인터페이스 및 마이그레이션 정책 반영
- [ ] `nightly-tests.yml`에 floci profile 매트릭스 추가 + `ci.yml` 동기화 확인

### 검증 명령

```bash
./gradlew :bluetape4k-testcontainers:test
./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test
./gradlew :bluetape4k-aws:test :bluetape4k-aws-kotlin:test \
    -Dbluetape4k.aws.emulator=floci   # smoke test
```

---

## 8. 결정 로그

| 결정 | 채택안 | 이유 |
|------|--------|------|
| AbstractAwsTest 추상화 방식 | Option B (인터페이스) | 회귀 검증 채널 + bluetape4k 컨벤션 일치 |
| LocalStack/MinIO 즉시 삭제 여부 | 한 릴리스 호환 후 삭제 | 외부 라이브러리 소비자 보호 |
| ElasticMQ Docker화 vs 임베드 | 임베드(in-process) | docker 미사용 단위 테스트 활용 가능, Scala 의존은 격리 |
| ElasticMQ가 `AwsEmulatorServer` 구현 여부 | 비-구현 (보조 유틸리티) | SQS 전용, Docker 비-사용, AbstractAwsTest 교체 대상 아님 |
| Mailpit 단독 vs floci 페어링 | 페어링 (SES API는 floci, 캡처는 Mailpit) | Mailpit이 SES API 비구현이므로 단독으로는 불완전 |
| floci docker socket | 기본 비활성, 명시 메서드(`withDockerSocket`)로만 활성화 | 보안 표면 최소화 |
| floci profile 기본값 | LocalStack 유지, floci는 `-D` 플래그 | 한 릴리스 호환 사이클 정책 |
| floci health check 전략 | `Wait.forListeningPort()` (안전 fallback) | floci `/_localstack/health` 지원 미확정. TODO 코멘트 + 추후 교체 |
| `AwsEmulatorServer`가 AWS SDK 노출 여부 | 비-노출 (extension 파일로 분리) | testcontainers 모듈의 AWS SDK leaky abstraction 방지 |
| AbstractAwsTest의 awsEmulator 초기화 | 기존 Launcher 싱글턴에 위임 | 이중 초기화 / testcontainers reuse 캐시 충돌 방지 |
| floci Docker 이미지 경로 | `floci/floci:1.5.7` (Docker Hub) | 공식 배포 채널 확인. GitHub Container Registry 경로(`ghcr.io/floci-io/floci`)는 미사용 |

---

## 9. Step 1-R 근거 요약

본 spec의 모든 주요 설계 주장은 다음 Step 1-R 발견에 근거한다.

- floci 1.5.7 / Docker Hub `floci/floci` / port 4566 / 공식 TC 모듈 없음 / docker socket은 stateful 서비스에서만 필요 / MIT
- ElasticMQ `org.elasticmq:elasticmq-rest-sqs_2.13:1.6.12` / `SQSRestServerBuilder` 임베드 / Scala transitive ~6MB
- Mailpit `axllent/mailpit:v1.29` / SMTP 1025 + UI 8025 / SES API 비구현 → floci 페어링 필요
- 기존 `LocalStackServer`는 `LocalStackContainer`를 상속하며 `Launcher.localStack` lazy singleton 패턴
- `KeycloakServer`가 `GenericContainer` 래핑 reference 패턴 (testcontainers 모듈 부재 시)
- AbstractAwsTest는 `aws/aws` 와 `aws/aws-kotlin` 두 곳에 존재하며 동일한 10개 서비스 리스트를 하드코드
- `aws-kotlin/AbstractAwsTest.kt`의 extension function들은 현재 `LocalStackContainer` receiver에 정의됨 → 인터페이스 전환 시 receiver 타입 갱신 필요
