# Vert.x Call은 listener dispatch 후 응답 완료를 공개한다

## 맥락

`VertxCallFactory`는 `promise.complete(response)`와 `callEnd` dispatch를 같은 `CompletableFuture`에 연결했다. `CompletableFuture.get()`의 대기 스레드는 원래 promise가 완료되는 순간 깨어날 수 있으므로, `callEnd`가 아직 실행 중이어도 `execute()`가 반환할 수 있었다. 이 경계가 CI 재시도에서만 `start -> end` 순서 검증 실패로 드러났다.

## 결정

Vert.x 요청을 취소·완료하는 내부 `promise`와 호출자에게 반환하는 `completion`을 분리한다. 내부 promise의 완료 callback에서 성공/실패 listener를 먼저 dispatch하고, `finally`에서 공개 completion을 성공 또는 실패로 완료한다. 따라서 `execute()`와 `enqueue()` callback은 listener dispatch 이후에만 관찰 가능한 완료를 받는다.

## 결과

성공 호출의 `callStart -> callEnd` happens-before 경계가 명시되었고, 실패·취소 경로도 기존 `toIOException()` 예외 계약을 유지한다. `cancel()`은 내부 promise를 취소하므로 기존 취소 전파와 callback 계약을 바꾸지 않는다. 공개 JVM API와 dependency는 변경하지 않았다.

## 검증

- RED: `callEnd`에서 release latch를 잡아 둔 동안 기존 `execute()`가 먼저 반환해 `responseFuture.isDone == true`가 되었다.
- GREEN: 동일 테스트가 listener release 전 반환하지 않고 성공 응답을 반환했다.
- 원래 `등록한 event listener는 성공 호출의 시작과 종료를 받는다()` 테스트 5회 반복 통과.
- `./gradlew :bluetape4k-retrofit2:test --no-daemon --no-configuration-cache --console=plain` — 311 tests 통과, 1 skipped.
- `:bluetape4k-retrofit2:detekt` — task 성공; 기존 `MaxLineLength`, `TooManyFunctions`, `TooGenericExceptionCaught` finding은 변경 범위 밖.

## 놓친 점과 다음 방지책

future가 완료되었다는 신호와 listener dispatch가 끝났다는 신호를 같은 상태로 취급했다. 비동기 adapter가 외부 completion과 lifecycle callback을 함께 제공할 때는 내부 작업 완료와 공개 완료를 분리하고, callback을 block한 테스트로 happens-before를 고정한다. 다른 adapter의 동일 패턴은 이 이슈 범위를 넘어 별도 conformance 검토 대상으로 남긴다.
