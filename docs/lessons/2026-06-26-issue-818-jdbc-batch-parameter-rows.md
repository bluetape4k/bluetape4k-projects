# 교훈: JDBC batch parameter row (2026-06-26)

**이슈**: #818
**모듈**: `:bluetape4k-jdbc`

## L1: batch row 형태는 binding 전에 검증해야 한다

### 문제

JDBC `PreparedStatement`는 batch addition 사이에서도 parameter 상태를 유지한다.
뒤쪽 row의 값 개수가 앞쪽 row보다 적으면, 짧은 row가 이전에 binding된 stale
value를 재사용하여 실패 대신 손상된 데이터를 insert할 수 있었다.

### 교훈

List 기반 batch helper는 statement를 준비하거나 실행하기 전에 parameter row의
형태를 검증하고, 각 row를 binding하기 전에 statement parameter를 비워야 한다.
이전 값이 이미 binding된 뒤 짧은 row를 driver가 잡아줄 것이라고 기대하지 않는다.

### 향후 방지책

JDBC batch helper를 변경할 때는 뒤쪽 row의 parameter 수가 더 적은 회귀 테스트를
추가하고, fail-fast 동작과 해당 batch 시도에서 persisted row가 0개임을 함께
검증한다.
