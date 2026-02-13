# AWS Kotlin Handover (2026-02-13)

## 범위

- 요청 범위: `aws-kotlin/*` 모듈 대상
    - code review
    - refactoring
    - 주석 보강/오류 수정
    - 미비 테스트 추가

## 완료 상태

- 완료 모듈
    - `aws-kotlin/core`
    - `aws-kotlin/ses`
    - `aws-kotlin/sesv2`
    - `aws-kotlin/sns`
    - `aws-kotlin/sqs`
    - `aws-kotlin/s3`
    - `aws-kotlin/dynamodb`

## 핵심 수정 요약

### 1) core

- `AuthSupport.kt`
    - 입력 검증 강화 (`accessKeyId`, `secretAccessKey` 공백 불가)
    - overload 정리
- 추가 테스트
    - `AuthSupportTest.kt`
    - `CrtHttpEngineSupportTest.kt`

### 2) ses / sesv2

- extension/model KDoc 오기 수정
- `getTemplateOrNull` 안정 동작 (`runCatching { ... }.getOrNull()`)
- 요청 빌더 입력 검증 강화 (빈 값/빈 바이트 배열 등)
- 오타 함수 alias는 `@Deprecated`로 유지
- 추가 테스트
    - `aws-kotlin/ses/.../SesModelSupportTest.kt`
    - `aws-kotlin/sesv2/.../SesV2ModelSupportTest.kt`

### 3) sns

- 오타 API 정리
    - `listSubscriptinosRequestOf` -> `listSubscriptionsRequestOf`
    - 기존 함수는 deprecated alias 유지
- `Publish.kt` KDoc `@parem` 오타 수정
- `MessageAttributeValue` 입력 검증 추가
- 추가 테스트
    - `SnsModelSupportTest.kt`

### 4) sqs

- `SqsClientExtensions.kt` 로그 오타 수정 (`SqlClient` -> `SqsClient`)
- KDoc 파라미터명 정리
- 모델 배치 빌더들 empty entries 검증 추가
- `receiveMessageRequestOf` 범위 검증 강화
    - `maxNumberOfMessages in 1..10`
    - `waitTimeSeconds in 0..20`
- 추가 테스트
    - `SqsModelSupportTest.kt`

### 5) s3

- `deleteObjectsRequestOf` builder 기본값 제공
- 모델 유틸 테스트 보강
    - `S3ModelSupportTest.kt`

### 6) dynamodb

- `DynamoDbClientExtensions.kt`
    - KDoc 오기 수정 (`S3 endpoint` -> `DynamoDB endpoint`)
    - 오탈자 수정 (`테이븖` -> `테이블`)
- `DynamoDbBatchExecutor.kt`
    - 미처리 항목 재시도 상한 추가 (`maxUnprocessedRetry`, 기본 10)
    - 무한 재귀 위험 완화
- 중요 버그 수정: Any-map 변환 패턴
    - 잘못된 `mapValues { it.toAttributeValue() }`를
    - 올바른 `mapValues { it.value.toAttributeValue() }`로 정정
    - 영향 파일: `PutItem/Query/Scan/Update/Get/Delete/Put/ConditionCheck/KeysAndAttributes`
- 추가 테스트
    - `DynamoDbClientSupportTest.kt`
    - `DynamoDbModelSupportTest.kt`

## 검증 명령

아래 통합 검증 통과함.

```bash
./gradlew \
  :bluetape4k-aws-kotlin-core:test \
  :bluetape4k-aws-kotlin-ses:test \
  :bluetape4k-aws-kotlin-sesv2:test \
  :bluetape4k-aws-kotlin-sns:test \
  :bluetape4k-aws-kotlin-sqs:test \
  :bluetape4k-aws-kotlin-s3:test \
  :bluetape4k-aws-kotlin-dynamodb:test
```

- 결과: `BUILD SUCCESSFUL`
- 참고: SNS confirm subscription 관련 2건은 기존 `@Disabled`로 pending

## 주의/맥락

- 워크트리에 `aws-kotlin` 외 기존 변경도 다수 존재함.
- 이번 작업은 `aws-kotlin/*` 중심으로 반영했고, 타 영역 변경은 되돌리지 않음.

## 맥 미니 재개 시 권장 순서

1. `git status --short`로 작업 트리 확인
2. 위 통합 검증 명령 1회 재실행
3. 필요 시 모듈별 추가 정리(문서화/커밋 분리)
