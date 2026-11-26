# concurrent 함수 변경 inline review

## 범위

- 기준: `develop` `21a7f39905cd5fff23ec90fd0f125812f8c76d3b`
- 대상: `AtomicIntRoundrobin.kt`, `VirtualFutureExtensions.kt`, `VirtualThreadDispatcher.kt`, `VirtualFutureTest.kt`
- 작업 유형: Type-B fast track, Kotlin concurrent API refactor

## 검토 결과

Inline verdict: PASS; P0=0, P1=0.

독립 code-reviewer lane은 `codex-code-mode-host` 실행 파일 누락으로 직접 diff·LSP 검사를 수행하지 못했습니다. 따라서 이 문서는 독립 reviewer의 승인 증거가 아니라, 실패한 lane을 복구한 뒤 기록하는 bounded inline review입니다.

- `virtualFutureAll`은 `ExecutorService.submit`이 반환한 원본 `Future`를 `asCompletableFuture`로 연결해 `cancel(true)`가 실행 중 작업까지 전달되도록 유지합니다.
- timed aggregate는 `sequence(executor)`와 `orTimeout(timeout.toNanos(), NANOSECONDS)`를 사용하고, 예외 완료 시 원본 futures를 취소합니다.
- `awaitAll(timeout)`은 caller-owned future를 취소하지 않고 대기만 제한한다는 의미를 KDoc에 명시했습니다.
- `AtomicIntRoundrobin`의 양수 검증과 dispatcher 변경은 기존 공개 JVM facade를 유지합니다.
- 초기 `futureOf` 전환은 interruption 회귀 테스트에서 실패했으며, 원본 `submit(...).asCompletableFuture()`로 복구한 뒤 회귀가 통과했습니다.

## 검증 증거

- 기준 worktree targeted baseline: 14 tests passed.
- 변경 worktree targeted: 14 tests passed, failures/errors/skipped 0.
- 변경 worktree full `:bluetape4k-core:test`: 1729 tests passed, failures/errors/skipped 0.
- `:bluetape4k-core:detekt`: 성공.
- `git diff --check`: 성공.
- `:bluetape4k-core:jar` 및 `javap -public`: 공개 메서드 facade 유지.

## 후속 관찰

제출 중 executor rejection 시 이미 제출된 작업을 rollback하는 개선은 기존 map-submit 경계이며 이번 변경의 P0/P1 회귀가 아니므로 별도 작업으로 분리합니다.

