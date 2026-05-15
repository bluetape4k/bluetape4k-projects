# Lessons Learned — GrafanaServer testcontainers (2026-05-15)

**관련 PR**: #455
**영향 모듈**: `:bluetape4k-testcontainers`

## L1: Apache HttpComponents 5 fluent API — `basicAuth`는 존재하지 않음

### 문제
`Request.basicAuth(user, password)` 호출 시 컴파일 오류 (`Unresolved reference`). HC5 fluent `Request`에는 `basicAuth` 메서드가 없다.

### 교훈
HC5 fluent API로 Basic Auth를 설정할 때는 `addHeader("Authorization", "Basic <base64>")` 형식으로 직접 추가해야 한다.

---

## L2: Apache HttpComponents 5 timeout 타입은 `java.time.Duration`이 아님

### 문제
`connectTimeout(Duration.ofSeconds(10))` 호출 시 타입 불일치 컴파일 오류. HC5의 `connectTimeout` / `responseTimeout`은 `org.apache.hc.core5.util.Timeout`을 요구한다.

### 교훈
HC5 fluent API 타임아웃은 `Timeout.ofSeconds(N)` 사용. `java.time.Duration`은 `HttpWaitStrategy`(Testcontainers) 에서만 사용.

---

## L3: `withXxx` 네이밍 — Testcontainers 관례와 충돌

### 문제
Testcontainers 전체 생태계에서 `withXxx()` 메서드는 컨테이너 시작 전 설정 용도. 그러나 `withPrometheusDataSource` / `withDashboard`는 시작 후 HTTP 호출이 필요한 메서드다. 코드 리뷰에서 두 리뷰어 모두 지적.

### 교훈
이슈에서 지정한 메서드명이라 유지했지만, 향후 유사 패턴 설계 시 `provision*` / `add*` 접두사를 사용하는 것이 의도를 더 명확히 전달한다.

---

## L4: JSON string interpolation — URL 이스케이프 필수

### 문제
`"url":"$prometheusUrl"` 형태의 raw string interpolation은 URL에 `"` 또는 `\`가 포함될 경우 malformed JSON을 생성.

### 교훈
JSON body에 외부 입력값을 인라인할 때는 최소한 `replace("\\", "\\\\").replace("\"", "\\\"")` 이스케이프를 적용. 이상적으로는 JSON 라이브러리를 사용.

---

## L5: 테스트 assertion은 동작 결과를 직접 검증해야 함

### 문제
초기 datasource 프로비저닝 테스트가 `server.isRunning.shouldBeTrue()`만 검사 — 프로비저닝이 실제로 성공했는지 무관한 assertion이었음 (코드 리뷰에서 "tautological" 지적).

### 교훈
provisioning/side-effect 테스트는 반드시 해당 동작의 결과를 API 호출로 검증: `GET /api/datasources` → response body에 `"Prometheus"` 포함 여부.
