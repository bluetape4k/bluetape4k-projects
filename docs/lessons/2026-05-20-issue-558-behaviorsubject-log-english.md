# Issue 558 BehaviorSubject Log English Translation

## Context

Issue #558 tracks one Korean runtime log message in `BehaviorSubject.emitError`.
Project policy keeps meaningfully edited log messages in English.

## Decision

Translate only the runtime log text and leave the Korean KDoc/comments untouched
because this issue is scoped to the log message.

## Outcome

`BehaviorSubject.emitError` now logs an English notification failure message,
matching the adjacent `complete()` error log style.

## Verification

- `./gradlew :bluetape4k-coroutines:compileKotlin :bluetape4k-coroutines:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-coroutines:test --tests io.bluetape4k.coroutines.flow.extensions.subject.BehaviorSubjectTest --tests io.bluetape4k.coroutines.flow.extensions.subject.SubjectCancellationTest --no-configuration-cache`: 21 passing.
- `git diff --check`
- `rg -n "BehaviorSubject.emitError 알림 실패|log\\.(trace|debug|info|warn|error)\\([^\\n]*\\) \\{ \\\"[^\\\"]*[가-힣]" bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/BehaviorSubject.kt`

## Future Guidance

For narrow translation chores, avoid broad KDoc/comment rewrites unless the issue
explicitly asks for them. Keep runtime log strings English in touched code.
