# #1425 SuspendNearJCache back-first mutation 계약

## 맥락

`SuspendNearJCache`의 일반 mutation이 front를 먼저 변경한 뒤 back cache를
호출하고 있어, back 작업이 실패해도 front에 미커밋 값이나 삭제 상태가
남을 수 있었다. 이는 back cache를 기준 데이터 원본으로 사용하는 near-cache
계약과 맞지 않았다.

## 원인

`put`, `putAll`, `putIfAbsent`, `remove`, `replace`가 compound API와 달리
front-first 순서를 사용했다. 특히 `replace`와 조건부 `remove`는 front 결과를
먼저 버린 뒤 back 상태를 별도로 조회하여 원자성도 보장하지 못했다.

## 결정

- 일반 mutation은 back-first로 수행하고 back 결과가 성공한 뒤 front를
  reconcile한다.
- back 실패 시 front를 호출하지 않는다.
- back commit 후 front reconcile이 실패하면 대상 front key를 invalidate하고
  원래 예외를 호출자에게 전달한다. invalidate 실패는 suppressed 예외로
  보존한다.
- `CancellationException`은 fallback이나 retry로 바꾸지 않고 재전파한다.

## 검증

- RED: 기존 구현에서 back 실패 시 front가 먼저 호출되어 9개 계약 테스트가
  실패함.
- GREEN: `SuspendNearJCacheBackFirstContractTest` 11개 PASS.
- 기존 `SuspendNearJCacheTest`와 `NearJCacheCompoundOperationContractTest`
  35개 PASS.
- `detekt` task는 BUILD SUCCESSFUL이며 cache-core 기존 findings가 남아 있다.

## 후속 지침

새로운 SuspendNearJCache mutation은 front-first write를 추가하지 말고,
back authoritative 결과와 front invalidate/reconcile 경계를 테스트로
고정한다. cancellation은 항상 별도 회귀 사례로 검증한다.
