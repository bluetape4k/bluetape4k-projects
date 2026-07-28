# Vert.x transaction cancellation cleanup

## 배경

이슈 #940은 Vert.x SQL transaction helper가 rollback과 connection close를 caller
coroutine context에서 수행해서 caller cancellation이 transaction cleanup을 중단시킬
수 있음을 확인했다.

## 결정

Rollback과 close operation은 `NonCancellable` cleanup boundary 안에서 실행한다.
Primary cancellation 또는 failure를 보존하고 rollback/close failure는 suppressed
exception으로 붙인다.

## 결과

`withSuspendTransaction`과 `withSuspendRollback`은 action이 coroutine context를 취소해도
cleanup을 완료하며, 원래 `CancellationException`은 계속 다시 던진다.

## 검증

- `./gradlew :bluetape4k-vertx:test --tests 'io.bluetape4k.vertx.sqlclient.PoolSupportTest'`

## 향후 지침

Coroutine 기반 database lifecycle cleanup은 suspending rollback/close call 주변에
`runCatching`을 사용하지 않는다. `NonCancellable` boundary 안에서 명시적인
`try/catch`를 사용하고 secondary cleanup failure를 suppressed evidence로 보존한다.
