# JDBC 상태 복구 실패는 primary failure를 대체하면 안 된다

## 배경

이슈 #946은 restore도 실패했을 때 primary block failure를 보존하지 않은 채
`finally`에서 connection state를 복구하는 JDBC helper function을 발견했다.

## 결정

Caller block 또는 state-change path의 primary failure를 기록한 뒤 restore failure를
suppressed exception으로 추가한다. Caller block은 성공하고 restore만 실패했다면
restore failure를 노출한다.

## 검증

- `./gradlew :bluetape4k-jdbc:test --tests 'io.bluetape4k.jdbc.sql.TransactionExtensionsTest'`
- `git diff --check`

## 향후 지침

JDBC connection state를 임시로 변경하는 lifecycle helper는 transaction
rollback/restore logic과 같은 primary-failure preservation pattern을 사용해야 한다.
