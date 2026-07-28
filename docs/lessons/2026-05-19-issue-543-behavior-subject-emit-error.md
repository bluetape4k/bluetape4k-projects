# 이슈 543 BehaviorSubject Terminal Cancellation

## 배경

`BehaviorSubject.emitError()`는 collector에게 알리는 동안 발생한 모든 `CancellationException`을
rethrow했다. 그 결과 collector-local cancellation이 남은 collector들에 대한 terminal error
notification을 중단할 수 있었다. Claude review는 인접한 `complete()` 경로도 parent cancellation을
보존한 뒤 collector-local cancellation을 error로 log한다는 점을 발견했다.

## 결정

`emitError()`와 `complete()`에 동일한 parent-cancellation guard shape을 사용한다.
`CancellationException`을 catch하고 `currentCoroutineContext().ensureActive()`를 호출한 뒤,
emitter coroutine이 여전히 active이면 계속 진행한다. 새 coroutine test에서는 더 좁은 exception이
예상될 때 broad `Throwable` catch를 피한다.

## 결과

`BehaviorSubject` terminal path는 이제 parent cancellation을 보존하면서도 cancelled collector 이후
notification을 계속 진행한다. Test는 collector cancellation 이후 `emitError()`와 `complete()`가
계속 진행되는지 모두 cover한다.

## 검증

`./gradlew :bluetape4k-coroutines:test --tests "io.bluetape4k.coroutines.flow.extensions.subject.SubjectCancellationTest"`가 12 tests로 통과.

Claude Code Opus rereview는 `complete()`를 정렬하고 symmetric test를 추가한 뒤 남은 P0/P1/P2 finding이
없다고 보고했다. 이후 `bluetape4k-patterns` pass에서 새 test catch block을 `Throwable`에서
`IllegalStateException`으로 좁혔다.

## 향후 가드

Subject terminal path는 `CancellationException`을 rethrow하기 전에 parent coroutine cancellation과
collector-local cancellation을 구분해야 한다. Coroutine test를 추가할 때는 broad collector failure
handling을 명시적으로 다루는 test가 아니라면 예상 exception만 catch한다.
