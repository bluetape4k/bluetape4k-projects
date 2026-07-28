# 이슈 790: Jackson2 typed mapper allowlist 보존

## 배경

`Jackson.createTypedJsonMapper(...)`는 `BasicPolymorphicTypeValidator`를 만든 뒤,
Jackson에 설정된 default typing resolver를 raw `StdTypeResolverBuilder`로
교체했다. 그 결과 실제 polymorphic deserialization 경로에서 validator가 제거되어
external `@class` id를 가진 root `Any` payload를 허용했다.

## 결정

Property 기반 `@class` type id에 설정된 validator가 계속 붙어 있도록
Jackson의 `activateDefaultTypingAsProperty(...)`를 사용한다. Safe factory에서는
`allowIfBaseType(...)` 대신 `allowIfSubType(...)`으로 package prefix를 검증한다.
Base type을 허용하면 nominal base type이 맞는 순간 모든 합법 subtype을 승인할 수
있기 때문이다.

## 결과

- 허용되지 않은 root `Any` polymorphic payload는 이제 `InvalidTypeIdException`으로 실패한다.
- 허용되지 않은 nested payload는 계속 실패하고, 허용된 package payload는 `@class` type id와 함께 round-trip된다.
- `typedJsonMapper`는 deprecated legacy compatibility mapper로만 남으며, untrusted JSON 예시에서 더 이상 권장되지 않는다.

## 검증

- 구현 전 red test: `com.example.disallowed.DisallowedTypedPayload`를 담은 root `Any` payload가 예외를 던지지 않았다.
- `./gradlew :bluetape4k-jackson2:test --tests "io.bluetape4k.jackson.JacksonTest.createTypedJsonMapper*" --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-jackson2:test --tests "io.bluetape4k.jackson.JacksonTest" --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-jackson2:test --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-jackson2:compileTestKotlin --warning-mode all --rerun-tasks --no-daemon --no-configuration-cache`
- `git diff --check`

## 향후 지침

`PolymorphicTypeValidator`를 설치한 뒤 raw `StdTypeResolverBuilder`로
`setDefaultTyping(...)`을 호출하지 않는다. Validator를 조용히 우회할 수 있다.
Package allowlist에는 envelope나 nested-property test뿐 아니라 root `Any` payload
회귀 테스트도 포함한다.

## 동시성 helper gate

`MultithreadingTester`, `SuspendedJobTester`, `StructuredTaskScopeTester`는 여기서
적용 대상이 아니었다. 이 수정은 동시성, coroutine, virtual-thread,
structured-task 동작을 추가하지 않고 synchronous Jackson default typing validation만
좁힌다.
