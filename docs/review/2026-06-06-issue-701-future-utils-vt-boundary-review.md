# Issue #701 FutureUtils virtual-thread boundary review

## Scope

- `FutureUtils`의 stale virtual-thread 이동 TODO를 공개 API 경계 설명으로 대체했다.
- `virtualFutureOf` / `virtualFutureOfNullable` KDoc을 English public API documentation으로 정리했다.
- virtual-thread `CompletableFuture` factory가 실제 virtual thread에서 block을 실행하는 테스트를 추가했다.

## 발견 사항

- P0=0
- P1=0
- P2=0

## 증거

- `./gradlew :bluetape4k-core:test --tests "io.bluetape4k.concurrent.FutureUtilsTest" --tests "io.bluetape4k.concurrent.virtualthread.CompletableFutureSupportTest"`: PASS, 24 passing.
- `./gradlew :bluetape4k-core:test`: first run failed once in existing `FutureSupportTest.cancel propagates to wrapped Future and cancels wrapper`; isolated rerun passed.
- `./gradlew :bluetape4k-core:test --tests "io.bluetape4k.concurrent.FutureSupportTest.cancel propagates to wrapped Future and cancels wrapper"`: PASS, 1 passing.
- `./gradlew :bluetape4k-core:test`: retry PASS, 1598 passing.
- `git diff --check`: PASS.
- CodeGraph `detect_changes_tool`: risk score 0.00, affected flows 0, test gaps 0.
- CodeGraph `get_impact_radius_tool`: low risk, additional affected files 0.
- `rg "TODO:.*VirtualThreadUtils|VirtualThreadUtils"` over changed files: no matches.

## Residual Risk

- IntelliJ diagnostics were not available in this session.
- The first full core test run exposed a timing-sensitive existing `FutureSupportTest` cancellation assertion, but it passed when isolated and in the full retry.
