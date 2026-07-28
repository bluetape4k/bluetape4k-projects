## Issue 436-438 States Runtime DSL 문서

배경: Issue #436, #437, #438은 #251의 StateMachine 비교 이후 `utils/states`를
확장했다.

결정: `bluetape4k-states`는 Kotlin/JVM 중심으로 유지한다. 별도 package에 optional
reactive runtime을 추가하고, 기존 flat DSL을 교체하지 않고 transition resolution을
확장해 nested state-family transition을 구현한다.

결과: Inherited transition resolution, `reactiveStateMachine`, one-time effect,
lifecycle side effect, keyed side-effect restart control, test,
English/Korean README positioning guidance를 추가했다. 이후 PR review는 reactive
lifecycle behavior를 강화했다. follow-up event는 비동기로 queue되고, transition
cancellation은 여전히 target side effect를 restart하며, `close()`는 active side
effect를 cancel하고 이후 send를 reject한다. side-effect registry update는 명시적인
lock으로 보호된다.

검증: 수정한 Kotlin file에서 IDE diagnostic을 실행했고 error는 0개였다.
`./gradlew :bluetape4k-states:test --no-configuration-cache`를 실행했으며 결과는
57 passing과 BUILD SUCCESSFUL이었다. Claude follow-up review는 lifecycle fix 이후
blocking, high, medium issue가 없다고 확인했다.

향후 에이전트: Inherited transition보다 exact transition precedence를 보존한다.
Reactive runtime은 optional로 유지하고, 별도 design issue 없이 UI/Compose 또는 KMP
concern을 이 module로 옮기지 않는다. Reactive side effect에서는 `close()`와
`restartSideEffects()` lifecycle update를 함께 조정한다. Thread-safe map만으로는
compound check/cancel/replace operation에 충분하지 않다.
