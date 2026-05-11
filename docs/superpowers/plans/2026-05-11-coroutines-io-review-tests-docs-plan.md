# bluetape4k-coroutines / bluetape4k-io 작업 계획

## 순서

1. `bluetape4k-coroutines` 리뷰
   - cancellation 전파가 필요한 `runCatching` 경로를 정리한다.
   - `DeferredSupport`와 Subject 계열 edge test를 추가한다.
   - public API KDoc/README 예제를 보강한다.
   - targeted test와 `:bluetape4k-coroutines:test`를 실행한다.
   - 6-Tier gate를 수행해 P0/P1을 제거한다.

2. `bluetape4k-io` 리뷰
   - file/path/zip/compressor/serializer public contract를 점검한다.
   - nullable/blank/empty/invalid path, resource cleanup edge test를 추가한다.
   - public API KDoc/README 예제를 보강한다.
   - targeted test와 `:bluetape4k-io:test`를 실행한다.
   - 6-Tier gate를 수행해 P0/P1을 제거한다.

3. 통합 마무리
   - diff review, module verification evidence 정리.
   - Lore commit 작성.
   - branch push 및 draft PR 생성.

## Coroutines 현재 판단

- P1 후보: Subject 종료 신호의 `runCatching`이 caller coroutine cancellation을 삼킬 수 있는 경로.
- P1 후보: `awaitAnyAndCancelOthers` winner 결과가 `CancellationException`일 때 내부 async 실패가 scope cancellation을 유발하지 않는지 명시적 test 필요.
- 문서 보강: Deferred helper의 cancellation contract와 Subject termination contract를 README 양쪽에 추가.

## IO 현재 판단

- P1 후보는 coroutines 완료 후 별도 리뷰로 확정한다.
- `Result` API의 `runCatching`은 sync IO 실패 값화가 목적이면 유지하고, contract/test/KDoc로 명확히 한다.
