# exposed-r2dbc 커버리지 향상 Plan

## Task 목록

### T1: ReadableExtensions — 숫자 타입 단위 테스트 (complexity: medium)

- 대상: `getString/OrNull`, `getByte/OrNull`, `getShort/OrNull`, `getInt/OrNull`, `getLong/OrNull`, `getFloat/OrNull`, `getDouble/OrNull`, `getBigDecimal/OrNull`
- 각 타입별: index 기반 + name 기반 각 1개 케이스
- 파일: `ReadableExtensionsTest.kt` 내부에 추가 테스트 함수

### T2: ReadableExtensions — 날짜/시간 타입 단위 테스트 (complexity: medium)

- 대상: `getDate/OrNull`, `getTimestamp/OrNull`, `getInstant/OrNull`, `getLocalDate/OrNull`, `getLocalTime/OrNull`, `getLocalDateTime/OrNull`, `getOffsetDateTime/OrNull`
- 파일: `ReadableExtensionsTest.kt` 내부에 추가

### T3: ReadableExtensions — ByteArray 타입 단위 테스트 (complexity: low)

- 대상: `getByteArray/OrNull`
- 파일: `ReadableExtensionsTest.kt`

### T4: VirtualThreadTransaction — withVirtualThreadTransaction 통합 테스트 (complexity: medium)

- `suspendTransaction { withVirtualThreadTransaction { } }` 패턴 테스트
- 내부 트랜잭션에서 INSERT 후 외부에서 SELECT 확인
- 파일: `VirtualThreadTransactionTest.kt` 확장

### T5: QueryExtensions — forEach / forEachIndexed 단위 테스트 (complexity: low)

- `flowOf(...)` 기반 단위 테스트로 forEach, forEachIndexed 직접 검증
- Query.forEach/forEachIndexed는 통합 테스트 필요 → 단위는 Flow 확장으로 검증
- 파일: `QueryExtensionsTest.kt` 확장

### T6: BatchInsertOnConflictDoNothing — MySQL SQL 경로 단위 테스트 (complexity: medium)

- MockK로 `Transaction` + `MysqlDialect` mocking
- `prepareSQL(transaction, prepared=false)` 결과에 `INSERT IGNORE` 포함 확인
- 파일: `BatchInsertOnConflictDoNothingTest.kt` 또는 새 단위 테스트 파일

### T7: 테스트 실행 및 커버리지 확인 (complexity: low)

- `./gradlew :bluetape4k-exposed-r2dbc:test` 실행
- 빌드 성공 + 전체 테스트 통과 확인

## 파일 변경 목록

| 파일                                                                   | 변경 유형              |
|------------------------------------------------------------------------|------------------------|
| `src/test/kotlin/.../ReadableExtensionsTest.kt`                        | 수정 (T1, T2, T3)      |
| `src/test/kotlin/.../VirtualThreadTransactionTest.kt`                  | 수정 (T4)              |
| `src/test/kotlin/.../QueryExtensionsTest.kt`                           | 수정 (T5)              |
| `src/test/kotlin/.../statements/BatchInsertOnConflictDoNothingTest.kt` | 수정 또는 새 파일 (T6) |
