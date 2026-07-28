# 교훈: 이슈 #821 MongoDB transaction abort

## 배경

`MongoClient.inTransaction`은 `CancellationException`을 다시 던져 올바르게 처리했지만,
이미 취소된 coroutine context에서 suspend `ClientSession.abortTransaction()`을
호출했다. 이 때문에 MongoDB가 transaction을 해제하기 전에 abort cleanup이 생략될 수
있었다.

## 교훈

Coroutine cancellation 이후 반드시 실행되어야 하는 suspend cleanup에는 명시적인
`withContext(NonCancellable)` 경계가 필요하다. 이 경계는 가능한 작게 유지한다.
Cleanup 호출만 non-cancellable로 실행하고 원래 cancellation은 계속 다시 던져야 한다.

## 결과

MongoDB transaction abort는 cancellation과 non-cancellation exception 경로 모두에서
`NonCancellable`로 실행된다. Abort failure는 owner throwable의 suppressed
exception으로 보존된다.

## 향후 방지책

Suspend cleanup 호출이 `catch (e: CancellationException)` 경로에서 실행된다면,
cleanup 전에 현재 coroutine context를 취소하고 cleanup 내부 suspension point가 여전히
완료됨을 증명하는 회귀 테스트를 추가한다.

## 검증

- `:bluetape4k-mongodb:compileKotlin` and
  `:bluetape4k-mongodb:compileTestKotlin` passed.
- `:bluetape4k-mongodb:test --tests "io.bluetape4k.mongodb.MongoClientSupportTest"`
  passed with 12 tests.
- `:bluetape4k-mongodb:test` passed with 50 tests.
- `:bluetape4k-mongodb:koverXmlReport` generated the XML coverage report.
- `git diff --check` passed.
