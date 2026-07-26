# Issue #498 KLoggingChannel Lifecycle Plan

## Tasks

1. Update `KLoggingChannel` lifecycle API.
    - Implement `AutoCloseable`.
    - Add shared runtime scope with one shutdown hook.
    - Add `isClosed` and `closeAndJoin()`.
    - Drop post-close events.

2. Strengthen tests.
    - Capture Logback events with an in-memory appender.
    - Assert emitted levels/messages.
    - Assert `closeAndJoin()` leaves the collector inactive.
    - Assert `close()` is idempotent and post-close events are dropped.

3. Update public docs.
    - English KDoc for changed public API.
    - `README.md` and `README.ko.md` lifecycle and usage guidance.

4. Verify.
    - IDE diagnostics/import cleanup when available.
    - `:bluetape4k-logging:compileKotlin`.
    - Targeted `KLoggingChannelTest`.
    - Full `:bluetape4k-logging:test`.

5. Delivery.
    - Add concise lesson.
    - Commit with Lore trailers.
    - Push branch and open PR assigned to `debop`.
    - Post current-session Codex Review comment and formal review.
    - Wait for PR CI checks; do not merge.
