# 교훈: `currentVertx` lifecycle (2026-07-09)

**관련 이슈**: #796
**대상 모듈**: `bluetape4k-vertx`, `bluetape4k-http`

## L1: context fallback helper는 ownership을 정의해야 한다

### 문제

`currentVertx()`는 Vert.x context가 없을 때마다 새 `Vertx.vertx()` instance를 만들었다.
`vertxHttpClientOf()` 같은 helper는 이 숨은 ownership을 상속했고 event-loop resource를
관리되지 않은 채 남길 수 있었다.

### 교훈

Fallback resource creation은 명시적인 owner를 요구하거나 문서화된 close path가 있는
managed singleton을 사용해야 한다. Vert.x helper에서는 lifecycle-sensitive API에
명시적인 `Vertx` parameter를 우선하고, default fallback은 재사용 가능하고 닫을 수
있게 유지한다.

### 검증

- RED: lifecycle tests failed before `closeDefaultVertx`, explicit `Vertx` overload, and default client close API existed.
- GREEN: `:bluetape4k-vertx:test`
- GREEN: `:bluetape4k-http:test`
