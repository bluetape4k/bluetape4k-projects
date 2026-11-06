# TimeoutSupport Duration 경계와 executor 수명 교훈

## 배경

`TimeoutSupport`의 `Duration` overload는 millisecond overload로 변환된 뒤 최소 10ms 정책을 적용한다. 그러나 zero·음수·sub-millisecond·무한값의 변환 정책과 executor 제출 거부·취소 시 수명이 테스트로 고정되어 있지 않았다.

## 결정

- `Duration.inWholeMilliseconds`의 truncation·포화 변환 결과를 문서화하고 10ms 미만은 `MIN_TIMEOUT_MILLIS`로 보정한다.
- 함수가 소유한 executor는 성공·timeout·취소 완료·동기 제출 거부 모두에서 `shutdown()`을 요청한다.
- 완료 콜백을 연결한 dependent future를 반환하지 않고 원본 future를 반환해, 호출자가 반환 future를 취소할 때도 cleanup 콜백이 실행되게 한다.
- 이미 제출한 작업의 강제 중단은 보장하지 않는다. timeout·취소 뒤에도 작업이 실행될 수 있으므로 작업 자체의 협조와 소유자의 추가 정리가 필요하다.

## 결과

동기 `RejectedExecutionException` 경로에서 executor가 종료되지 않던 누수를 수정했다. Duration 경계값과 성공·timeout·취소·제출 거부 cleanup 회귀 테스트를 추가해 public overload와 lifecycle 계약을 고정했다. 공개 함수 서명과 Java compatibility bridge는 유지했다.

## 검증

- pre-fix RED: 제출 거부 cleanup 테스트가 `Expected <false> to be <true>`로 실패했다.
- pre-fix RED: 반환 future 취소 뒤 executor cleanup 테스트가 같은 assertion으로 실패했다.
- post-fix GREEN: `TimeoutSupportTest` 전체 통과.
- `Duration.INFINITE`, zero·음수·sub-millisecond·최대 유한 Duration 변환을 단위 테스트로 확인.

## 남은 경계

`CompletableFuture.cancel`은 이미 실행 중인 supplier를 반드시 interrupt하지 않는다. 이 helper는 executor 종료 요청까지만 책임지며, 작업 중단·외부 자원 해제는 호출자 작업과 executor 소유자가 협조적으로 처리해야 한다.

## 후속 방지책

새 timeout overload를 추가할 때는 Duration 변환, 결과 future의 cancellation 경로, 동기 제출 실패를 한 묶음으로 테스트한다. callback을 반환용 dependent stage에만 연결하지 말고, 호출자가 취소할 수 있는 소유 future에 lifecycle callback을 연결한다.
