# 교훈: 빈 range overlap (2026-06-26)

관련 이슈: #783
대상 모듈: `:bluetape4k-core`

## L1: 빈 range는 overlap 검사에서 먼저 short-circuit해야 한다

### 문제

`Range.overlaps()`는 endpoint ordering과 boundary inclusiveness만 비교했다.
그래서 `(1, 1)`, `[1, 1)`, `(1, 1]` 같은 빈 range가 공통 원소가 없는데도
non-empty range와 overlap한다고 보고할 수 있었다.

### 교훈

Range operation의 계약이 element 기반이면 endpoint 비교를 적용하기 전에
`isEmpty()`를 확인한다. 빈 range도 non-empty interval 안에 놓일 수 있으므로
boundary 비교만으로는 충분하지 않다.

### 향후 방지책

Range helper 변경에는 receiver와 argument 양쪽 위치의 빈 operand 회귀 coverage를
포함하고, non-empty range에 대한 기존 boundary inclusiveness test도 함께 유지한다.
