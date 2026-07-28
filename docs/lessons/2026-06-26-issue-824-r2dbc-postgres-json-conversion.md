# 교훈: R2DBC PostgreSQL JSON conversion (2026-06-26)

**이슈**: #824
**모듈**: `:bluetape4k-r2dbc`

## L1: converter fallback은 조용한 데이터 손실이 될 수 있다

### 문제

PostgreSQL JSON converter는 Jackson 실패를 log로 남기고 유효한 empty value를
반환했다. Malformed database value는 empty map이 되었고, 직렬화할 수 없는
application value는 `{}`가 되었다.

### 교훈

Persistence boundary에 놓인 converter는 API가 fallback semantics를 명시적으로
모델링하지 않는 한 원래 cause를 포함해 실패해야 한다. 유효한 empty value는 잘못된
저장 데이터나 직렬화 실패를 대신할 안전한 값이 아니다.

### 향후 방지책

R2DBC converter 변경에는 success path와 failure path 회귀 테스트를 모두 추가한다.
Failure-path test는 logging 여부만이 아니라 exception type과 cause를 검증해야 한다.
