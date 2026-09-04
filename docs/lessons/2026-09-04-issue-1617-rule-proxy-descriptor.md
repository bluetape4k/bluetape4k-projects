# Issue #1617: reflection으로 찾은 Method descriptor를 이름으로 축약하지 않는다

## 맥락

`RuleDefinitionValidator`와 `RuleProxy`는 `@Condition`, `@Action`, `@Priority`가 붙은
정확한 `Method`를 찾고도 실행 직전에 메서드 이름으로 다시 조회했다. 같은 이름의
overload가 먼저 반환되면 검증한 parameter/return descriptor와 실제 invoke 대상이 달라져
`RuleException`, `argument type mismatch`, `wrong number of arguments`가 발생했다.

## 결정

- annotation discovery에서 얻은 `Method`를 condition parameter 주입과 invoke에 함께 쓴다.
- action 정렬 객체가 보관한 `Method`를 그대로 invoke한다.
- priority annotation을 가진 `Method`를 이름 재조회 없이 invoke한다.
- 이름이 같은 overload가 annotation method보다 먼저 노출되는 fixture로 세 경로를 각각
  검증한다.
- generic interface 구현에서 compiler가 생성한 bridge method가 함께 노출되는 경로도
  별도 회귀 테스트로 고정한다.

## 결과

reflection 순서와 무관하게 annotation이 붙은 descriptor가 condition, action, priority의
실제 호출 대상이 된다. public API와 JVM descriptor는 변경하지 않았고 private name-based
lookup만 제거했다.

## 검증

- 수정 전 `RuleProxyTest`: 12 tests 중 새 회귀 4건 실패.
- 수정 후 `RuleProxyTest`: 12 passing.
- `:bluetape4k-rule-engine:test`: 356 passing, 5 pending.
- compile/detekt task: `BUILD SUCCESSFUL`; 새 fixture의 Detekt finding 0.
- `git diff --check`: 통과.

## 향후 지침

reflection 기반 validator, dispatcher, serializer가 annotation이나 signature로 멤버를
선택하면 선택 결과의 `Method`, `Constructor`, `Field` identity를 실행 단계까지 보존한다.
이름은 로그와 표시 용도로만 사용하고 overload, bridge, 상속 멤버를 다시 고르는 key로
사용하지 않는다. 회귀 테스트는 같은 이름이면서 parameter descriptor가 다른 멤버를 먼저
노출해 name-only lookup이 반드시 실패하도록 구성한다.
