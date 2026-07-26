# assertions/junit5 리뷰·테스트·문서 보강 계획

**Spec**: `docs/superpowers/specs/2026-05-11-assertions-junit5-review-tests-docs-design.md`
**날짜**: 2026-05-11 **브랜치**: `codex/assertions-junit5-review-tests-docs`

## 순서

1. `bluetape4k-assertions` 리뷰
    - public API KDoc 공백과 README 예제 불일치를 먼저 수정한다.
    - exception/cancellation, Flow 중복 값, numerical tolerance edge test를 추가한다.
    - `:bluetape4k-assertions:test`로 검증한다.

2. assertions 6-Tier gate
    - P0/P1을 코드·테스트·문서 기준으로 분류한다.
    - P0/P1이 있으면 수정 후 gate를 반복한다.

3. `bluetape4k-junit5` 리뷰
    - `TempFolder` path confinement을 symlink 부모까지 검증한다.
    - tempfolder public API KDoc/README를 현재 계약으로 보강한다.
    - `:bluetape4k-junit5:test`로 검증한다.

4. junit5 6-Tier gate
    - P0/P1을 코드·테스트·문서 기준으로 분류한다.
    - P0/P1이 있으면 수정 후 gate를 반복한다.

5. 통합 마감
    - 필요한 추가 targeted test/build를 실행한다.
    - 전체 diff를 리뷰하고 Lore protocol로 commit한다.
    - branch push 후 draft PR을 생성한다.

## 현재 관찰

- `testing/assertions/README*.md`는 `BlueConf` import와 non-suspend Flow 예제가 현재 API와 맞지 않는다.
- `Numerical.kt`의 공개 epsilon 상수는 KDoc이 없다.
- `FlowAssertions.assertResultSet`은 set 비교라 중복 값이 다른 경우 size로만 일부 방어한다. 중복 개수 차이를 명확히 테스트한다.
- `TempFolder`는 기본 `..`/absolute path 방어가 있으나, 기존 디렉터리가 symlink일 때 root 밖으로 생성될 수 있는지 확인이 필요하다.

## Gate 결과

### assertions 6-Tier review

| Tier                   | 결과       | 근거                                                                                  |
|------------------------|------------|---------------------------------------------------------------------------------------|
| Correctness            | P0=0, P1=0 | `shouldBeNear` invalid tolerance 거부, Flow duplicate-aware 비교                      |
| Edge tests             | P0=0, P1=0 | negative/NaN tolerance, BigDecimal tolerance, Flow duplicate/cancellation 테스트 추가 |
| API/compatibility      | P0=0, P1=0 | public signature 유지, Turbine 불필요 cast 제거                                       |
| Concurrency/coroutines | P0=0, P1=0 | Flow `CancellationException` rethrow 테스트 추가                                      |
| Documentation          | P0=0, P1=0 | README/README.ko 예제를 현재 API와 suspend context에 맞게 수정                        |
| Maintainability        | P0=0, P1=0 | 새 dependency/large abstraction 없음                                                  |

### junit5 6-Tier review

| Tier                   | 결과       | 근거                                                                                              |
|------------------------|------------|---------------------------------------------------------------------------------------------------|
| Correctness            | P0=0, P1=0 | `TempFolder` relative-only path confinement + symlink parent real path 검사                       |
| Edge tests             | P0=0, P1=0 | absolute path, `..`, symlink escape 테스트 추가                                                   |
| API/compatibility      | P0=0, P1=0 | public method signatures 유지                                                                     |
| Concurrency/coroutines | P0=0, P1=0 | `MultithreadingTester`, `StructuredTaskScopeTester`, `SuspendedJobTester` 기반 안정성 테스트 추가 |
| Documentation          | P0=0, P1=0 | README/README.ko에 relative-only/symlink escape 계약 추가                                         |
| Maintainability        | P0=0, P1=0 | 기존 tempfolder 구조 안에서 helper만 추가                                                         |

## Verification

- `./gradlew :bluetape4k-assertions:test` -> 689 passing
- `./gradlew :bluetape4k-junit5:test` -> 258 passing
- `./gradlew :bluetape4k-assertions:build :bluetape4k-junit5:build` -> BUILD SUCCESSFUL
- `git diff --check` -> clean
