# Kover Matrix Coverage Aggregation Needs Artifact Boundaries

## 배경

`bluetape4k-projects` Nightly workflow는 integration-heavy test group을 포함한
큰 multi-module build에서 Kover coverage를 집계한다.

## 결정 또는 발견

Coverage aggregation은 각 matrix group이 만든 명시적인 Kover XML artifact를
입력으로 삼아야 한다. artifact가 없으면 허용된 empty group이거나 workflow 실패다.
누락된 artifact가 aggregate coverage를 조용히 낮추면 안 된다.

## 결과

Nightly workflow는 aggregation 동작을 명시적으로 유지하면서 daily smoke coverage와
weekly full coverage를 분리할 수 있게 됐다.

## 검증

- `bluetape4k-projects` Nightly workflow는 smoke/full scope 처리를 분리한다.
- Aggregation script는 `.github/scripts/` 아래에서 추적된다.
- Org Nightly dispatcher dry-run은 `scope=smoke` 또는 `scope=full`을
  `bluetape4k-projects`로 명시적으로 전달한다.

## 향후 지침

- Kover input artifact는 test group 이름으로 구분한다.
- 의도적으로 제외한 integration group은 문서화한다.
- Coverage가 지나치게 낮아 보이면 threshold를 바꾸기 전에 artifact 생성 여부를 먼저 확인한다.
