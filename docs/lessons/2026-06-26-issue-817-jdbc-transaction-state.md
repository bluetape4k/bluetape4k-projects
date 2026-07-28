# 교훈: JDBC transaction 상태 (2026-06-26)

**이슈**: #817
**모듈**: `:bluetape4k-jdbc`

## L1: transaction helper는 복구 실패를 숨기면 안 된다

### 문제

`withTransaction`은 상태 복구를 하나의 logged-and-swallowed block에서 처리했고
`Exception`만 잡았다. 그래서 `Exception`이 아닌 실패에서는 rollback이 빠질 수
있었고, restore 실패가 발생해도 성공처럼 보고될 수 있었다.
`withReadOnlyTransaction`도 호출자의 기존 상태를 복구하지 않고 read-only mode를
항상 `false`로 되돌렸다.

### 교훈

Transaction helper는 rollback과 상태 복구를 공개 계약의 일부로 다뤄야 한다.
호출자 소유 connection flag를 모두 캡처하고, transaction을 중단시키는 모든
`Throwable` 경로에서 rollback하며, 각 상태 필드를 독립적으로 복구하고 보조 실패는
suppressed exception으로 붙인다.

### 향후 방지책

JDBC transaction helper를 변경할 때는 `autoCommit=false`, 기존 read-only 상태,
`Exception`이 아닌 rollback 경로, primary failure suppression, success path의
restore failure를 검증하는 deterministic connection-proxy test를 포함한다.
