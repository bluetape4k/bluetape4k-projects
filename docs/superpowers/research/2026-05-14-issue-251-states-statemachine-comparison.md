## Issue #251 — states vs joost-klitsie/StateMachine comparison

날짜: 2026-05-14

## 목적

`joost-klitsie/StateMachine`을 분석해 `utils/states`와 비교하고,
`bluetape4k-states`에 반영할 개선 이슈를 도출한다.

## 외부 라이브러리 요약

- 저장소: `joost-klitsie/StateMachine`
- 최신 릴리스: `release/0.3.0` (2026-01-29)
- GitHub 메타데이터: 약 45 stars, 4 forks, open issue 1개
- 라이선스: GitHub metadata 기준 감지되지 않음
- 성격: Kotlin Multiplatform, presentation-layer 중심
- 핵심 API: `stateMachine<State, Effect, Event>(scope, initialState)`,
  `send(event)`, `StateFlow` 구현, `consumeEffects`, `sideEffect`,
  `nestedState`, `state`, `onEvent`, `trigger(effect)`

## `utils/states` 현재 위치

- Kotlin/JVM backend-friendly FSM 모듈
- Sync `StateMachine<S, E>`와 suspend `SuspendStateMachineInterface<S, E>` 제공
- `TransitionResult<S, E>`를 명시적으로 반환
- guard, final states, `canTransition`, `allowedEvents`, `isInFinalState` 제공
- suspend 구현은 `stateFlow`를 노출
- 의존성은 `bluetape4k-core`, `bluetape4k-coroutines`, Kotlin coroutines 중심

## 비교 결론

외부 라이브러리를 직접 의존성으로 들이지 않는다.

이유:

- bluetape4k는 JVM backend library scope가 중심이다.
- 외부 라이브러리는 UI/ViewModel/Compose 친화적인 이벤트-효과 런타임이다.
- 라이선스가 GitHub metadata에서 확인되지 않아 dependency risk가 있다.
- 작은 프로젝트 표면과 초기 릴리스 단계라 장기 안정성 근거가 약하다.

하지만 다음 아이디어는 가치가 있다.

- event queue 기반 `send(event)` API
- one-time effect flow
- 상태 진입/이탈에 묶인 lifecycle side effect
- key 기반 side effect restart 제어
- nested state DSL과 parent-level shared transition

## 등록한 후속 이슈

- #436 — Reactive event/effect runtime
- #437 — Nested state DSL
- #438 — README comparison and positioning guidance

## 검증

- GitHub issue #251 본문 확인
- `utils/states` 파일/README/API 표면 확인
- 외부 저장소 README, Gradle, source/test tree, release/open issue metadata 확인
