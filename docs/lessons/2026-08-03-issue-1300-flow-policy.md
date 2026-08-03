# 이슈 #1300 Flow 정책 후속 작업 교훈 (2026-08-03)

## 결정

실제 production caller가 없는 delay-error·explicit overflow 후보를 이번
milestone에 추가하지 않고, 현재 `concat`, `merge`,
`onBackpressureDrop`과 표준 `buffer`/`conflate`의 계약을 문서와 결정적
회귀 테스트로 고정했다. Reactive Streams의 demand·prefetch·overflow 오류
의미를 Kotlin Flow public API에 추정해서 복사하지 않는다.

## 재사용할 규칙

1. 새 Flow 연산자를 제안하기 전에 production caller와 downstream 요구를
   먼저 검색하고, 결과를 계약 매트릭스에 남긴다. caller가 없으면 API 추가가
   아니라 보류 조건과 재개 조건을 기록한다.
2. `buffer`의 `SUSPEND`·`DROP_OLDEST`·`DROP_LATEST`와 `conflate`의 최신값
   보존은 표준 Flow 계약으로 검증한다. 이것을 Reactive Streams의
   `request(n)` 또는 `MissingBackpressureException`과 동일하다고 표현하지
   않는다.
3. `merge` 회귀 테스트는 sibling cancellation과 예외의 class/message를
   검증한다. Throwable 객체의 `equals`에 의존해 특정 인스턴스 동일성을
   계약으로 만들지 않는다.
4. `catch`의 cancellation 제외를 검증할 때 upstream에서
   `CancellationException`을 직접 던지는 대신 collecting `Job`을 실제로
   취소한다. 두 경로는 Flow에서 같은 테스트 의미를 갖지 않는다.
5. delay-error를 다시 열 때는 오류 집계 순서, `CancellationException` 제외,
   동시성·queue 메모리 상한, downstream 취소 정리 시점을 별도 설계와
   deterministic test로 먼저 승인한다.

## 검증 증거

- `FlowPolicyContractTest` 6개 PASS
- 기존 `ConcatTest`, `MergeFlowsTest`, `OnBackpressureDropTest`와 합친 20개
  표적 회귀 PASS
- 새 production Kotlin API·dependency·module 변경 없음
- 계약 근거와 caller evidence: [`docs/flow-operator-policy-matrix.md`](../flow-operator-policy-matrix.md)

## 후속 리스크

독립 `gpt-5.6-luna max` 연구 lane은 현재 런타임에서 모델을 사용할 수 없어
차단되었다. 공식 Kotlin Flow·RxJava·Reactor 문서와 저장소 증거로 현재 범위를
검증했지만, 해당 독립 검토와 PR/CI/merge 검증은 별도 게이트로 남긴다.
