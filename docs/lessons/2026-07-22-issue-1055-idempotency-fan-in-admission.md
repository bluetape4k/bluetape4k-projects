# Idempotency Fan-In 테스트의 admission 순서

## Context

Issue #1055의 repeated fan-in scenario는 같은 key에 owner 1개, bounded waiter, overflow request를 동시에
보내고 각 결과 수가 정확한지 검증한다. 이 검증은 shared runner뿐 아니라 실제 Ktor와 Spring HTTP boundary에서도
같은 계약을 증명해야 했다.

## Unexpected Failure

초기 테스트는 coroutine을 생성한 순서가 waiter로 등록되는 순서라고 가정하고, 마지막에 생성한 request가
`idempotency_waiters_exceeded`를 받을 것으로 기대했다. 실제 HTTP engine과 dispatcher는 시작된 coroutine을
다른 순서로 실행할 수 있다. 따라서 waiter limit 자체는 정확해도 어느 attempt가 admission을 얻는지는
비결정적이었고, real HTTP proof에서 scheduler에 따라 테스트가 실패했다.

## Lesson

경쟁 테스트는 launch index나 completion index를 ownership/admission identity로 사용하면 안 된다. 시스템이
보장하는 것은 특정 request의 순서가 아니라 다음과 같은 outcome cardinality다.

- 같은 scope와 fingerprint에서 business owner는 정확히 1개다.
- admitted waiter 수는 `maxWaitersPerKey`를 넘지 않는다.
- 나머지 attempt는 즉시 overflow 결과를 받는다.
- terminal completion 뒤 owner와 admitted waiter만 terminal response를 받고 모든 resource가 quiescent해진다.

각 attempt identity와 실제 완료 결과를 함께 기록한 뒤 outcome별 개수를 검증하면 dispatcher가 실행 순서를
바꿔도 계약은 안정적으로 증명된다. 특정 순서를 검증해야 한다면 production code가 그 순서를 명시적으로
보장하고 관측 가능한 admission sequence를 제공해야 한다.

## Outcome

repeated fan-in proof는 launch 순서 대신 실제 attempt 결과를 분류한다. shared in-memory adapter, Ktor
`testApplication`, Spring MockMvc가 같은 cardinality와 cleanup 계약을 통과하며, framework scheduler 차이가
false failure를 만들지 않는다.

## Verification

- `bluetape4k-junit5`: 317 tests passed.
- `bluetape4k-ktor-testing`: 7 tests passed.
- `bluetape4k-spring-boot-core`: 243 tests passed.
- 세 affected module의 combined `build`가 통과했다.

## Future Guard

fan-in, bulkhead, waiter queue, single-flight, ownership election 테스트를 추가할 때 launch 순서 기반 assertion을
사용하지 않는다. exact identity 순서가 공개 계약이 아니라면 owner/admitted/rejected/terminal outcome의
cardinality와 최종 quiescence를 검증한다.
