# 이슈 1356: Jackson legacy permissive typing 차단

## 배경

이슈 [#1356](https://github.com/bluetape4k/bluetape4k-projects/issues/1356)은
allowlist 기반 `Jackson.createTypedJsonMapper(...)`를 도입한 뒤에도 남아 있던
legacy polymorphic API를 다룬다. `typedJsonMapper`, `prettyTypedJsonWriter`,
`createDefaultJsonMapper(needTypeInfo = true)`가 외부에서 계속 호출될 수 있었고,
`needTypeInfo` 경로는 `allowIfBaseType(Any::class.java)`로 모든 base type을 허용했다.
deprecated 주석과 문서 경고만으로는 이 보안 경계를 강제할 수 없었다.

## 원인

legacy property 두 개가 `createDefaultJsonMapper(needTypeInfo = true)`를 호출하고,
해당 옵션이 permissive `BasicPolymorphicTypeValidator`를 설치했다. 따라서 호출자가
deprecated 경고를 무시하면 allowlist 없이 default typing을 활성화할 수 있었다.

## 결정

- `createTypedJsonMapper(...)`만 명시적 trusted subtype package allowlist를 받는
  polymorphic mapper 생성 경로로 유지한다.
- `typedJsonMapper`, `prettyTypedJsonWriter`,
  `createDefaultJsonMapper(needTypeInfo = true)`의 source-compatible public shape은
  당장 유지하되 접근 시 `UnsupportedOperationException`을 발생시킨다.
- `allowIfBaseType(Any::class.java)` production 경로를 제거한다. 허용 목록은
  `allowIfSubType(...)`와 배열 subtype 규칙으로만 구성한다.
- `1.13.0`에서는 legacy symbol을 migration signal로 남기고 runtime에서 즉시 차단한다.
  다음 major release에서 public symbol 제거를 재검토한다.

## 안전한 사용 경계

`createTypedJsonMapper("com.example.model.")`는 애플리케이션이 관리하는 trusted
payload에만 사용한다. untrusted 또는 외부 JSON은 구체적인 DTO나 별도 검증된 형태로
역직렬화하며 Jackson default typing을 활성화하지 않는다. 새 polymorphic API를 추가할
때도 permissive base validator를 도입하지 않고, 악성·비허용 type id 회귀 테스트를 함께
작성한다.

## 검증

- RED: legacy 세 진입점이 예외를 던져야 한다는 신규 테스트가 기존 구현에서 3건 실패하고,
  기존 테스트 10건은 통과했다.
- GREEN: 세 migration-failure 테스트와 기존 allowlist root/nested 거부 및 허용 round-trip을
  포함한 `JacksonTest` 전체가 통과했다.
- `./gradlew :bluetape4k-jackson2:test --no-daemon --no-configuration-cache`:
  473 tests passed.
- `./gradlew :bluetape4k-jackson2:compileTestKotlin --warning-mode all --rerun-tasks
  --no-daemon --no-configuration-cache`: `BUILD SUCCESSFUL`.
- `./gradlew :bluetape4k-jackson2:detekt --no-daemon --no-configuration-cache`:
  `BUILD SUCCESSFUL`.
- EN/KO README의 동일한 safe factory, legacy 세 진입점, trusted/untrusted 경계,
  `1.13.0` runtime 차단 및 다음 major 제거 검토 내용을 대조했고 `git diff --check`도
  통과했다.

## 향후 지침

`BasicPolymorphicTypeValidator`를 만들 때 `Any` base type을 허용하지 않는다. trusted
package prefix를 명시하고 root·nested payload 양쪽에서 비허용 type id가 실패하는지
검증한다. legacy API를 다시 살리는 대신 migration failure 메시지의
`createTypedJsonMapper(...)` 안내를 유지한다.
