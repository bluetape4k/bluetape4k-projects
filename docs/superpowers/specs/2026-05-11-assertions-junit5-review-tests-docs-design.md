# assertions/junit5 리뷰·테스트·문서 보강 스펙

**날짜**: 2026-05-11 **브랜치**: `codex/assertions-junit5-review-tests-docs`
**Worktree**: `.worktrees/assertions-junit5-review-tests-docs/`
**대상 모듈**:

- `testing/assertions` -> `:bluetape4k-assertions`
- `testing/junit5` -> `:bluetape4k-junit5`

## 1. 목표

두 테스트 기반 모듈의 public API 품질을 현재 bluetape4k 기준으로 재점검한다.

- 코드 리뷰로 P0/P1 결함을 식별하고 제거한다.
- 누락된 테스트와 edge case를 추가한다.
- public API KDoc을 한국어로 보강하고 실제 사용 가능한 예제를 유지한다.
- `README.md`와 `README.ko.md`의 예제가 현재 API와 컴파일 맥락에 맞도록 갱신한다.
- `bluetape4k-design` 6-Tier review gate를 적용해 P0/P1이 0임을 확인한다.

## 2. 리뷰 범위

### assertions

- `Numerical.kt`: 공개 tolerance 상수, range/near assertion edge case.
- `Exceptions.kt`: `CancellationException` 전파, DSL 예제와 실제 API 일치성.
- `coroutines/FlowAssertions.kt`: cancellation 보존, 순서/중복 값 edge case.
- `Softly.kt`: JUnit 5 `assertAll` 위임과 virtual thread 안전성 유지.
- README: coroutine/Flow 예제가 suspend 컨텍스트에서 실행되도록 수정.

### junit5

- `tempfolder`: 임시 루트 밖 파일 생성 방지, symlink 부모 edge case, close idempotency.
- coroutine/awaitility helpers: cancellation 전파와 README 예제 일치성.
- extension/fake/random/outputcapture public API: KDoc과 README 예제의 현재성 확인.

## 3. 비목표

- 새로운 assertion API 대량 추가.
- 외부 dependency 추가.
- 기존 테스트 프레임워크나 Gradle 구조 재편.
- 기존 public API의 호환성 없는 시그니처 변경.

## 4. 성공 기준

- `:bluetape4k-assertions:test` 통과.
- `:bluetape4k-junit5:test` 통과.
- 필요 시 모듈 `build`까지 통과 또는 실패 사유 명시.
- 6-Tier review gate 결과 P0/P1 0.
- 변경은 Lore commit protocol로 커밋되고 원격 브랜치에 push된다.
- Draft PR이 생성된다.

## 5. 6-Tier Gate 기준

1. Correctness: assertion 결과, temp path confinement, cancellation 전파.
2. Edge tests: boundary/tolerance, duplicated Flow items, symlink/path escape.
3. API/compatibility: public signature 호환 유지.
4. Concurrency/coroutines: structured cancellation, virtual thread safety.
5. Documentation: KDoc/README 예제가 실제 API와 일치.
6. Maintainability: 기존 패턴 준수, 불필요한 abstraction/dependency 없음.

P0/P1 발견 시 수정 후 동일 gate를 재실행한다.
