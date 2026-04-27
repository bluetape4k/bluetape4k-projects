# MiniStackServer 구현 계획

**날짜**: 2026-04-28  
**브랜치**: `feat/ministack-server`  
**Spec**: `docs/superpowers/specs/2026-04-27-ministack-server-design.md`  
**범위**: `testing/testcontainers` 모듈

---

## Task 목록

### T1. 의존성 추가 (complexity: low)

**파일**: `buildSrc/src/main/kotlin/Libs.kt`

- `testcontainers_ministack` 상수를 LocalStack 상수 근처에 추가
  ```kotlin
  // MiniStack — free MIT-licensed AWS emulator (ministack.org)
  const val testcontainers_ministack =
      "org.ministack:testcontainers-ministack:0.1.4"
  ```

**파일**: `testing/testcontainers/build.gradle.kts`

- `compileOnly(Libs.testcontainers_localstack)` 아래에 추가
  ```kotlin
  compileOnly(Libs.testcontainers_ministack)
  ```

---

### T2. MiniStackServer.kt 구현 (complexity: medium)

**파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/MiniStackServer.kt`

FlociServer.kt 패턴 그대로 따르되 MiniStack 특화 부분 적용:

- `GenericContainer<MiniStackServer>` 상속 + `AwsEmulatorServer` 구현
- companion object: IMAGE/TAG/NAME/PORT/DEFAULT_ACCESS_KEY/DEFAULT_SECRET_KEY/DEFAULT_REGION 상수
- `invoke(image, tag, useDefaultPort, reuse)` + `invoke(imageName, useDefaultPort, reuse)` factory
- `init` 블록:
  - `addExposedPorts(PORT)`
  - `withReuse(reuse)`
  - `Wait.forHttp("/_ministack/health").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(60))`
  - `if (useDefaultPort) exposeCustomPorts(PORT)`
- `override val port`, `url`, `awsEndpoint`, `awsAccessKey`, `awsSecretKey`, `regionName`, `propertyNamespace`
- `propertyKeys()`, `properties()` 구현
- `withServices()`: no-op (FlociServer 패턴 동일)
- `start()`: `super.start()` + `writeToSystemProperties()`
- `Launcher.miniStack`: lazy singleton + `ShutdownQueue.register`
- 모든 public API에 Korean KDoc 필수

---

### T3. AwsEmulatorServer.kt KDoc 업데이트 (complexity: low)

**파일**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/AwsEmulatorServer.kt`

- class-level KDoc의 에뮬레이터 목록에 MiniStack 추가
- `withServices` KDoc에 MiniStack no-op 동작 추가

---

### T4. MiniStackServerTest.kt 구현 (complexity: medium)

**파일**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/MiniStackServerTest.kt`

FlociServerTest.kt 패턴 따르되 MiniStack 적용:

- blank image/tag 검증 (`assertFailsWith<IllegalArgumentException>`)
- 서버 시작 후 `isRunning.shouldBeTrue()`
- S3 client 연결 테스트 (path-style access enabled)
  - bucket 생성 → object put → object get 검증
- `withServices()` no-op 검증

---

### T5. MiniStack CloudWatch/DynamoDB/KMS 서비스 테스트 (complexity: medium)

**경로**: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/ministack/services/`

공통 패턴:
- `MiniStackServer.Launcher.miniStack` (no `.withServices()`)
- `server.awsEndpoint` (LocalStack의 `.endpoint`가 아닌 `AwsEmulatorServer` 인터페이스 속성)
- `Region.of(server.regionName)` (LocalStack의 `.region`이 아님)
- `server.getCredentialProvider()`

**MiniStackCloudWatchTest.kt**: CloudWatchTest.kt 1:1 대응
- `put metric data`, `list metrics`, `create/describe log groups/streams`, `put log events`

**MiniStackDynamoDBTest.kt**: DynamoDBTest.kt 1:1 대응  
- `insert/get/update/delete item`, `list tables`

**MiniStackKMSTest.kt**: KMSTest.kt 1:1 대응
- `create key`, `encrypt`, `decrypt`, `disable/enable key`, `create/list/revoke grant`
- `create/list/delete alias`, `list keys`, `put key policy`
- ⚠️ `@Disabled` 없이 전체 테스트 실행 (MiniStack KMS 전 기능 지원)

---

### T6. MiniStack Kinesis/S3/SNS 서비스 테스트 (complexity: medium)

**MiniStackKinesisTest.kt**: KinesisTest.kt 1:1 대응
- `create stream`, `put records`, `get records`

**MiniStackS3Test.kt**: S3Test.kt 1:1 대응
- S3Client에 `.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())` 추가
- `create bucket`, `put object`, `get object`

**MiniStackSNSTest.kt**: SNSTest.kt 1:1 대응
- `create topic`, `publish`, `subscribe`, `list topics/subscriptions`

---

### T7. MiniStack SQS/STS 서비스 테스트 (complexity: medium)

**MiniStackSQSTest.kt**: SQSTest.kt 1:1 대응
- `create queue`, `send message batch`, `receive message`, `delete message`

**MiniStackSTSTest.kt**: STSTest.kt 1:1 대응
- `get caller identity`, `get session token`

---

### T8. README 업데이트 (complexity: low)

**파일**: `testing/testcontainers/README.md`
- MiniStack 섹션 추가 (LocalStack/Floci 섹션 다음)
- Docker image, port, health endpoint, 의존성, 예시 코드 포함

**파일**: `testing/testcontainers/README.ko.md`
- 동일 내용 한국어로 작성

---

## 실행 순서

```
T1 (의존성) → T2 (MiniStackServer) → T3 (KDoc) → T4 (기본 테스트)
                                                 → T5 (CloudWatch/DynamoDB/KMS — 병렬)
                                                 → T6 (Kinesis/S3/SNS — 병렬)
                                                 → T7 (SQS/STS — 병렬)
                                     → T8 (README)
```

T5/T6/T7은 T2 완료 후 병렬 실행 가능.

---

## DoD 체크리스트 (Spec 기준)

- [ ] `MiniStackServer.kt` 구현 완료 (`AwsEmulatorServer` 인터페이스 전부 구현)
- [ ] `MiniStackServer.Launcher.miniStack` 싱글턴 동작 확인
- [ ] `MiniStackServerTest.kt` 통과
- [ ] 8종 서비스 통합 테스트 모두 통과 (실패 시 `@Disabled` + KDoc 제한사항 문서화)
- [ ] KMS 테스트: `@Disabled` 없이 전체 통과
- [ ] `Libs.kt` + `build.gradle.kts` 의존성 추가 완료
- [ ] Korean KDoc — 모든 public API
- [ ] README.md + README.ko.md 업데이트
- [ ] Step 6-R 코드 리뷰 (CRITICAL/HIGH 0건)
