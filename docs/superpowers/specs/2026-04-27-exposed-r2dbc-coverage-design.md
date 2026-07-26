# exposed-r2dbc 테스트 커버리지 향상 Spec

## 이슈

Closes #176 — `data/exposed-r2dbc` 테스트 커버리지 47.60% → 70% 향상

## 현황 분석

### 소스 파일별 현황

| 파일                                | 라인 수 | 미커버 함수                                                                                                                                                                                                                  |
|-------------------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ReadableExtensions.kt`             | 346     | 타입별 inline 확장함수 (getString, getByte, getShort, getInt, getLong, getFloat, getDouble, getBigDecimal, getByteArray, getDate, getTimestamp, getInstant, getLocalDate, getLocalTime, getLocalDateTime, getOffsetDateTime) |
| `VirtualThreadTransaction.kt`       | 122     | `withVirtualThreadTransaction`                                                                                                                                                                                               |
| `QueryExtensions.kt`                | 93      | `forEach`, `forEachIndexed` (단위 테스트 없음)                                                                                                                                                                               |
| `BatchInsertOnConflictDoNothing.kt` | ~70     | MySQL(INSERT IGNORE) SQL 생성 경로 미테스트                                                                                                                                                                                  |

### 커버리지 갭 요약

-

**ReadableExtensions.kt**: 346 라인 중 `getAs`, `getAsOrNull`, `getExposedBlob`, `getExposedBlobOrNull`, `getUuid` 만 커버됨. 나머지 17개 타입 그룹 (각 4개 오버로드 = ~68개 함수) 미커버
- **VirtualThreadTransaction.kt**: `withVirtualThreadTransaction` (R2dbcTransaction 확장 함수) 완전 미테스트
- **QueryExtensions.kt**: `forEach`, `forEachIndexed` 단위 테스트 없음 (통합 테스트에 간접 포함 가능성 있으나 직접 검증 없음)
- **BatchInsertOnConflictDoNothing.kt**: MySQL dialect 분기 (`INSERT IGNORE`) SQL 경로 미테스트

## 설계 결정

### 단위 테스트 전략 (ReadableExtensions)

기존 `FakeReadable` 패턴을 재사용하여 DB 없이 단위 테스트 작성.

- 각 타입의 index/name 오버로드를 각각 검증
- null 케이스: `OrNull` 변형은 null 반환 확인, non-null 변형은 예외 발생 확인은 기존 테스트에서 충분히 검증됨 → 추가 케이스만 작성

### 통합 테스트 전략 (VirtualThreadTransaction, QueryExtensions)

기존 `AbstractExposedR2dbcTest` + `withTables` 패턴으로 통합 테스트 추가.

### BatchInsertOnConflictDoNothing SQL 단위 테스트

MySQL dialect 경로는 MockK로 dialect를 mocking하여 `prepareSQL` 반환값 검증.

## DoD

- [ ] `ReadableExtensions`: 모든 타입 그룹의 index/name 오버로드 테스트
- [ ] `VirtualThreadTransaction`: `withVirtualThreadTransaction` 테스트
- [ ] `QueryExtensions`: `forEach`, `forEachIndexed` 테스트
- [ ] `BatchInsertOnConflictDoNothing`: MySQL SQL 경로 단위 테스트
- [ ] `./gradlew :bluetape4k-exposed-r2dbc:test` 통과
- [ ] 커버리지 70% 이상 확인
