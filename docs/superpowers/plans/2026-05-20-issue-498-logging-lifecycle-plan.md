# Issue #498 KLoggingChannel lifecycle 계획

## 작업

1. `KLoggingChannel` lifecycle API를 갱신한다.
    - `AutoCloseable`을 구현한다.
    - shutdown hook 하나를 사용하는 shared runtime scope를 추가한다.
    - `isClosed`와 `closeAndJoin()`을 추가한다.
    - close 이후 event를 버린다.

2. test를 강화한다.
    - in-memory appender로 Logback event를 캡처한다.
    - 발생한 level/message를 검증한다.
    - `closeAndJoin()` 이후 collector가 비활성 상태인지 검증한다.
    - `close()`가 idempotent하고 close 이후 event가 버려지는지 검증한다.

3. public 문서를 갱신한다.
    - 변경된 public API에는 English KDoc을 작성한다.
    - `README.md`와 `README.ko.md`에 lifecycle 및 사용 지침을 갱신한다.

4. 검증한다.
    - 가능한 경우 IDE diagnostics/import cleanup을 실행한다.
    - `:bluetape4k-logging:compileKotlin`.
    - 대상 `KLoggingChannelTest`.
    - 전체 `:bluetape4k-logging:test`.

5. 전달한다.
    - 간결한 lesson을 추가한다.
    - Lore trailer를 포함해 commit한다.
    - branch를 push하고 `debop`에게 할당한 PR을 연다.
    - 현재 session Codex Review comment와 formal review를 게시한다.
    - PR CI check를 기다리되 merge하지 않는다.
