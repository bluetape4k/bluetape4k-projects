# Issue #1051: Context propagation conformance

## 문제

coroutine, Reactor, task executor, Spring Observation, Ktor request는 context를 전달하는 방식과 종료 신호가
서로 다릅니다. 각 adapter가 자체 assertion을 가지면 parent visibility, cleanup, isolation의 의미가 쉽게
달라지고 cancellation을 일반 실패나 deadline으로 잘못 분류할 수 있습니다.

이번 작업은 framework별 구현을 공통 구현으로 합치지 않았습니다. 대신 `:bluetape4k-junit5`에
provider-neutral snapshot과 assertion 경계를 두고, 각 소비 module의 테스트가 실제 framework bridge를
실행한 결과를 그 snapshot으로 변환하도록 했습니다.

## 선택한 공통 경계

공통 계약은 다음 증거만 다룹니다.

- boundary 진입, suspension 이후, terminal 직전의 synthetic parent marker
- success, failure, cancellation, deadline의 실제 terminal 분류
- caller, worker, request 위치의 cleanup probe
- 고정 A/B parent가 겹쳐 실행됐다는 event-ledger partial order
- 후속 unwrapped 또는 unparented probe가 A/B 값을 재사용하지 않았다는 isolation 증거

fixture는 framework type, tracer provider, registry, exporter의 lifecycle을 소유하지 않습니다. adapter
테스트가 실제 framework context를 읽고 immutable snapshot으로 변환하며, fixture는 값 비교와 안전한
redacted 진단만 수행합니다.

## Cancellation과 deadline

`CancellationException`과 `TimeoutCancellationException`은 같은 cleanup 경로를 통과해도 의미가 다릅니다.
따라서 cancellation 시나리오는 실제 child/request cancellation을 발생시키고, deadline 시나리오는
250ms `withTimeout`을 실행해 각각 `CANCELLATION`과 `DEADLINE_EXCEEDED`로 검증했습니다. 모든 semantic
deadline은 5초 hang guard보다 짧습니다. `NonCancellable` 영역의 release, cancel, join 순서는 coroutine,
Spring, Ktor처럼 child/request job을 소유하는 경계에 적용합니다.
Reactor는 subscription dispose와 scheduler shutdown을 모두 시도한 뒤 interrupt를 복원하고, task executor는
future cancel과 executor shutdown/termination을 별도로 보장합니다. 서로 다른 lifecycle primitive를
`NonCancellable` cleanup 하나로 일반화하지 않습니다.

## 결정적인 isolation barrier

확률적 반복이나 ordering용 sleep/delay를 사용하지 않았습니다. A/B participant가 모두 `READY`를 기록한
뒤에만 `RELEASED`로 진행하며, parent가 양쪽 terminal과 `finally` 완료를 관측한 다음 cleanup probe를
실행합니다. participant 또는 client가 ready 이전이나 release 이후에 실패해도 첫 실패를 보존하고 peer
gate를 해제하며, 양쪽 terminal을 모두 기록한 뒤 원래 실패를 다시 던집니다.

## Production 변경 없음

변경은 `:bluetape4k-junit5`의 shared test fixture public surface, module test source, bilingual README,
이 lesson에만 한정했습니다. 실제 context를 전달하는 consumer runtime adapter, dependency, build script,
module registration, global OpenTelemetry/Reactor hook은 변경하지 않았습니다. Ktor 검증도 local
`OpenTelemetrySdk`와 in-process `testApplication`만 사용하며 외부 collector나 server를 요구하지 않습니다.

## 검증 증거

전체 module suite는 순차 실행했고 모두 통과했습니다.

- `:bluetape4k-junit5:test`: 338 tests, `real 16.01s`
- `:bluetape4k-opentelemetry:test`: 110 tests, `real 86.74s`
- `:bluetape4k-spring-boot-core:test`: 249 tests, `real 25.29s`
- `:bluetape4k-ktor-observability:test`: 20 tests, `real 6.68s`
- root `detekt`: `BUILD SUCCESSFUL`, `NO-SOURCE`, `real 3.04s`
- `:bluetape4k-junit5:dokkaGenerate`: `BUILD SUCCESSFUL`; 기존 README link warning 3건만 유지

대상 module은 개별 `detekt` task를 노출하지 않으므로 root 성공을 Kotlin 정적 분석 통과로 간주하지
않았습니다. 대신 각 module test의 Kotlin compile, 금지 API/output scan, executable documentation gate,
`git diff --check`, 독립 7-Tier review를 최종 품질 근거로 사용합니다.

추가 executable gate는 fresh JUnit XML의 conformance case 수와 단일 case 최대 7.5초 미만, README
English/Korean symbol 및 핵심 문구 parity, global telemetry/context mutation과 raw output API 부재,
`git diff --check`를 확인합니다.
