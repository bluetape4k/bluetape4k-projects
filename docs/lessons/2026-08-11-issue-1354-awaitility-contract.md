# Awaitility suspend adapter의 시간 계약 보존

## 배경

`testing/junit5`의 `untilSuspending`은 `ConditionFactory`를 받아 suspend 조건을 반복 평가하지만, 기존 구현은 timeout·poll·exception 설정 일부만 private reflection으로 읽고 `atLeast`, `during`, `failFast`를 버리고 있었습니다. 그 결과 조건이 최초로 `true`가 된 순간 성공할 수 있어 안정 조건 검증이 거짓 양성이 될 수 있었습니다.

## 원인

- `ConditionFactory`의 private 설정을 읽지 못하면 임의 기본값으로 계속 진행했습니다.
- 초기 poll delay 이후에 timeout 측정을 시작해 `atMost` deadline이 실제보다 늘어났습니다.
- 생성된 전체 poll timeout과 조건 본문에서 발생한 `ConditionTimeoutException`을 같은 예외 흐름으로 처리했습니다.

## 결정

- Awaitility 설정을 immutable snapshot으로 읽는 fail-closed adapter를 둡니다.
- `atLeast`, `during`, `failFast`, poll delay/interval, ignored exception을 coroutine polling 루프에 명시적으로 반영합니다.
- 초기 poll delay를 전체 `atMost` deadline에 포함합니다.
- 내부 poll timeout marker로 생성 timeout과 조건 본문 예외를 구분해 ignore 정책을 보존합니다.
- 지원하지 않는 Awaitility private field 구조에서는 기본값으로 묵살하지 않고 명시적으로 실패합니다.

## 결과

`untilSuspending`이 Awaitility의 시간·예외·조기 실패 계약을 보존하면서도 suspend 조건을 non-blocking 방식으로 평가하도록 수정했습니다. 설정 구조가 바뀌면 조용한 오동작 대신 즉시 호환성 오류를 확인할 수 있습니다.

## 검증

- RED: 계약 테스트 6건 중 `atLeast`, `during`, `failFast` 3건 실패를 재현했습니다.
- GREEN: `AwaitilityCoroutinesContractTest` 9/9, Awaitility package 18/18, `junit5` module 347/347 통과.
- `detekt` 성공 및 변경 파일 findings 0, `git diff --check` 통과.
- exact-model 독립 리뷰 APPROVE, P0–P3 findings 0건.

## 향후 지침

Awaitility 의존성 버전이나 `ConditionFactory`의 private field 구조를 변경할 때는 adapter의 fail-closed 계약과 시간 경계 회귀 테스트를 함께 갱신합니다. 기본값 fallback이나 stdout 경고로 설정 손실을 숨기지 않습니다.
