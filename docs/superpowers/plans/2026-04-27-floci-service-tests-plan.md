# FlociServer AWS 서비스 통합 테스트 추가 — Implementation Plan

**Date**: 2026-04-27  
**Spec**: docs/superpowers/specs/2026-04-27-floci-service-tests-design.md  
**Issue**: #202

---

## Task List

### T1 — FlociS3Test 작성 (complexity: medium)
- 파일: `src/test/kotlin/io/bluetape4k/testcontainers/aws/floci/services/FlociS3Test.kt`
- S3Test.kt 패턴 그대로, LocalStackServer → FlociServer 교체
- `server.endpoint` → `floci.awsEndpoint`, `server.region` → `floci.regionName`
- 테스트: 서버 시작 확인, 버킷 생성, 오브젝트 put/get

### T2 — FlociSQSTest 작성 (complexity: medium)
- 파일: `src/test/kotlin/io/bluetape4k/testcontainers/aws/floci/services/FlociSQSTest.kt`
- SQSTest.kt 패턴: 큐 생성/조회, 메시지 send/receive/delete, batch

### T3 — FlociSNSTest 작성 (complexity: medium)
- 파일: `src/test/kotlin/io/bluetape4k/testcontainers/aws/floci/services/FlociSNSTest.kt`
- SNSTest.kt 패턴: 토픽 생성/조회/삭제, 발행, 구독

### T4 — FlociDynamoDBTest 작성 (complexity: medium)
- 파일: `src/test/kotlin/io/bluetape4k/testcontainers/aws/floci/services/FlociDynamoDBTest.kt`
- DynamoDBTest.kt 패턴: 테이블 생성, CRUD
- GSI 관련 테스트는 `@Disabled("Floci #587: GSI Query pagination loops forever")` 처리

### T5 — FlociKinesisTest 작성 (complexity: medium)
- 파일: `src/test/kotlin/io/bluetape4k/testcontainers/aws/floci/services/FlociKinesisTest.kt`
- KinesisTest.kt 패턴: 스트림 생성, put/get records

### T6 — FlociKMSTest 작성 (complexity: medium)
- 파일: `src/test/kotlin/io/bluetape4k/testcontainers/aws/floci/services/FlociKMSTest.kt`
- KMSTest.kt 패턴: symmetric key 생성, encrypt/decrypt, alias
- asymmetric key `GetKeyRotationStatus` 관련 테스트는 `@Disabled("Floci #586: GetKeyRotationStatus is not working with asymmetric key")` 처리

### T7 — FlociSTSTest 작성 (complexity: medium)
- 파일: `src/test/kotlin/io/bluetape4k/testcontainers/aws/floci/services/FlociSTSTest.kt`
- STSTest.kt 패턴: getCallerIdentity, assumeRole, getSessionToken

### T8 — FlociCloudWatchTest 작성 (complexity: medium)
- 파일: `src/test/kotlin/io/bluetape4k/testcontainers/aws/floci/services/FlociCloudWatchTest.kt`
- CloudWatchTest.kt 패턴: metrics, logs

### T9 — 테스트 실행 및 검증 (complexity: low)
- `./gradlew :bluetape4k-testcontainers:test --tests "io.bluetape4k.testcontainers.aws.floci.*"`
- 실패 시 원인 파악 및 수정 또는 `@Disabled` 추가

### T10 — Commit + Push (complexity: low)
- 커밋 메시지: `feat(testcontainers): FlociServer 기반 AWS 서비스 통합 테스트 추가 (#202)`
- push → PR 생성

---

## 의존 관계

T1-T8은 독립적 → 병렬 실행 가능  
T9는 T1-T8 완료 후  
T10은 T9 통과 후
