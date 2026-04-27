# FlociServer AWS 서비스 통합 테스트 추가 — Design Spec

**Date**: 2026-04-27  
**Issue**: #202  
**Branch**: feat/floci-service-tests

---

## 1. 배경 및 목표

`LocalStack Community Edition`이 2026-03-23 아카이브 이후 [Floci](https://github.com/floci-io/floci)가 무료 오픈소스 AWS 에뮬레이터 대안으로 부상하고 있다.

현재 `io.bluetape4k.testcontainers.aws.services` 패키지에 LocalStack 기반 서비스 통합 테스트 8개가 존재하지만, Floci 기반 대응 테스트는 없다. 이를 추가하여:
1. Floci의 실제 서비스 지원 범위를 검증한다
2. LocalStack 대체 가능성을 테스트 수준에서 확인한다
3. Floci 알려진 버그/미지원 항목을 문서화한다

---

## 2. 설계 결정

### 2-A. 패키지 분리

LocalStack 테스트와 별도 패키지로 분리한다:
- **신규**: `io.bluetape4k.testcontainers.aws.floci.services`
- **기존**: `io.bluetape4k.testcontainers.aws.services` (변경 없음)

**이유**: 두 에뮬레이터를 동시에 사용하는 경우 클래스명 충돌 방지. 향후 Floci 전용 헬퍼/확장이 추가될 수 있어 네임스페이스 분리가 유리.

### 2-B. FlociServer 참조 방식

```kotlin
// LocalStack 방식 (서비스 선택 가능)
private val server = LocalStackServer.Launcher.localStack.withServices("s3")

// Floci 방식 (모든 서비스 항상 활성화)
private val floci = FlociServer.Launcher.floci
```

`FlociServer.Launcher.floci`는 싱글턴으로 이미 `start()` 호출 포함. `@BeforeAll`에서 참조만 하면 lazy 초기화 트리거됨.

### 2-C. API 매핑

| LocalStack | Floci |
|-----------|-------|
| `server.endpoint` (URI) | `floci.awsEndpoint` (URI) |
| `server.region` (String) | `floci.regionName` (String) |
| `server.getCredentialProvider()` | `floci.getCredentialProvider()` (동일 확장함수) |
| `withServices("s3")` | no-op (전체 활성) |

### 2-D. @Disabled 처리 정책

Floci 알려진 버그/제한이 있는 테스트는 `@Disabled` + 이유 주석으로 표시:

| 서비스 | 알려진 이슈 | 처리 |
|-------|-----------|------|
| KMS | `GetKeyRotationStatus` asymmetric key 버그 (#586) | 해당 테스트 케이스만 `@Disabled` |
| DynamoDB | GSI pagination 무한루프 (#587) | GSI 관련 테스트만 `@Disabled`. 기본 CRUD는 정상 동작 |

> Lambda credentials 버그 (#611)는 본 테스트 범위 외 (Lambda 테스트 없음).

---

## 3. 테스트 파일 목록

| 파일명 | 대응 LocalStack 테스트 | 비고 |
|--------|----------------------|------|
| `FlociS3Test.kt` | `S3Test.kt` | 정상 |
| `FlociSQSTest.kt` | `SQSTest.kt` | 정상 |
| `FlociSNSTest.kt` | `SNSTest.kt` | 정상 |
| `FlociDynamoDBTest.kt` | `DynamoDBTest.kt` | GSI 테스트 제외 |
| `FlociKinesisTest.kt` | `KinesisTest.kt` | 정상 |
| `FlociKMSTest.kt` | `KMSTest.kt` | asymmetric key 테스트 제외 |
| `FlociSTSTest.kt` | `STSTest.kt` | 정상 |
| `FlociCloudWatchTest.kt` | `CloudWatchTest.kt` | 정상 |

---

## 4. 코드 패턴 (공통)

```kotlin
@Suppress("DEPRECATION")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FlociXxxTest : AbstractContainerTest() {

    companion object : KLogging()

    private val floci: FlociServer
        get() = FlociServer.Launcher.floci

    private val xxxClient: XxxClient by lazy {
        XxxClient.builder()
            .endpointOverride(floci.awsEndpoint)
            .region(Region.of(floci.regionName))
            .credentialsProvider(floci.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    @BeforeAll
    fun setup() {
        // Lazy 초기화 트리거 (내부에서 start() 호출됨)
        floci.isRunning.shouldBeTrue()
    }
    
    // ...
}
```

---

## 5. DoD (Definition of Done)

- [ ] 8개 테스트 파일 신규 생성 (`floci.services` 패키지)
- [ ] 모든 테스트 클래스에 `@Suppress("DEPRECATION")` 적용
- [ ] 모든 테스트 클래스에 `companion object : KLogging()` 적용
- [ ] 모든 public 클래스에 한국어 KDoc 작성
- [ ] Floci 알려진 버그 항목은 `@Disabled` + 이유 주석 처리
- [ ] `FlociServer.Launcher.floci` 싱글턴으로 컨테이너 재사용
- [ ] Kluent assertion 사용 (shouldBe*, shouldNotBe*, shouldContain 등)
- [ ] 로컬 테스트 통과 (`./gradlew :bluetape4k-testcontainers:test`)
