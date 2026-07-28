# 이슈 558 BehaviorSubject Log English Translation

## 배경

Issue #558은 `BehaviorSubject.emitError`에 남아 있던 한국어 runtime log message 하나를 추적한다.
Project policy는 의미 있게 수정한 log message를 영어로 유지한다.

## 결정

이 issue의 scope가 log message이므로 runtime log text만 번역하고 Korean KDoc/comment는 그대로 둔다.

## 결과

`BehaviorSubject.emitError`는 이제 인접한 `complete()` error log style과 맞는 영어 notification failure
message를 남긴다.

## 검증

- `./gradlew :bluetape4k-coroutines:compileKotlin :bluetape4k-coroutines:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-coroutines:test --tests io.bluetape4k.coroutines.flow.extensions.subject.BehaviorSubjectTest --tests io.bluetape4k.coroutines.flow.extensions.subject.SubjectCancellationTest --no-configuration-cache`: 21 passing.
- `git diff --check`
- `rg -n "BehaviorSubject.emitError 알림 실패|log\\.(trace|debug|info|warn|error)\\([^\\n]*\\) \\{ \\\"[^\\\"]*[가-힣]" bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/BehaviorSubject.kt`

## 향후 가이드

좁은 translation chore에서는 issue가 명시적으로 요구하지 않는 한 broad KDoc/comment rewrite를 피한다.
Touched code의 runtime log string은 영어로 유지한다.
