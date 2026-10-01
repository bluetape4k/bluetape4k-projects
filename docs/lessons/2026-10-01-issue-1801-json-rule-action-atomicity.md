# 규칙 action 파싱은 부분 목록을 만들지 않는다

## 맥락

[이슈 #1801](https://github.com/bluetape4k/bluetape4k-projects/issues/1801)의 입력은 `actions: ["valid action", {}]`처럼 유효한 값과 잘못된 값이 섞여도 규칙을 등록할 수 있었다. `JsonRuleReader.asMap()`이 `runCatching`과 `mapNotNull`로 변환에 실패한 action만 제거했고, `RuleReader.createRuleDefinition()`은 남은 action이 비어 있지 않으면 정상 정의로 받아들였다.

`readAll()`에서 `asMap()` 변환은 `tryGetRuleDefinition()` 호출 전에 평가된다. action 검증만 엄격하게 바꾸면 잘못된 규칙 하나가 전체 시퀀스 읽기를 중단할 수 있으므로, 각 규칙을 변환하는 경계에서 실패를 처리해야 한다.

## 결정

- action 목록의 각 항목을 문자열인지 확인한다. `read()`에서는 잘못된 항목이 `IllegalArgumentException`으로 규칙 전체를 거부한다.
- `readAll()`은 변환에 실패한 규칙의 인덱스와 필드 수를 경고하고 해당 규칙 전체를 제외한다. 나머지 유효한 규칙은 계속 반환한다.
- 규칙 의미를 구성하는 하위 항목에는 `mapNotNull`로 손실성 복구를 적용하지 않는다. 복구가 필요하면 소유 객체 전체를 제외하고 진단을 남긴다.

## 검증

- 수정 전 회귀 테스트는 `read()`가 예외를 던지지 않고, `readAll()`이 잘못된 규칙까지 포함해 각각 의도한 이유로 실패했다.
- `JsonRuleReaderTest`: 7개 통과. `readAll()` 테스트는 경고 메시지의 규칙 인덱스와 필드 수도 확인한다.
- `:bluetape4k-rule-engine:test`: 361개 통과, Kotlin Script 테스트 5개 pending. 해당 클래스는 `@Disabled("Kotlin Script 테스트는 CI 환경에서 불안정할 수 있으므로 로컬에서만 실행합니다")`로 비활성화되어 있다.
- `:bluetape4k-rule-engine:detekt`: `BUILD SUCCESSFUL`; 기존 진단은 변경 파일 밖에 있었고 변경 파일 진단은 없었다.
- 실행 명령과 결과는 `docs/testlogs/2026-10.md`에 기록했다.
- public JVM signature는 바뀌지 않았다. 이 변경에서는 ABI 검사를 실행하지 않았다.

## 재발 방지

실행 가능한 설정을 파싱할 때는 목록의 각 원소가 계약의 일부인지 먼저 확인한다. 잘못된 원소를 조용히 제거하면 사용자가 작성한 규칙과 실제 실행 규칙이 달라질 수 있다. `read`와 `readAll` 각각에 유효·잘못된 원소를 섞은 회귀 테스트를 두고, 전체 객체 거부 또는 진단을 동반한 전체 객체 제외를 검증한다.

`readAll`의 회귀 fixture에서는 잘못된 항목 앞과 뒤에 유효한 항목을 모두 둔다. 반환 결과의 순서와 양쪽 유효 항목의 보존을 확인해야 다음 항목까지 계속 처리하는 동작을 검증할 수 있다.
