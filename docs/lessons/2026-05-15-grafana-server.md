# 배운 점 — GrafanaServer testcontainers (2026-05-15)

**관련 PR**: #455
**영향 모듈**: `:bluetape4k-testcontainers`

## L1: Apache HttpComponents 5 fluent API — `basicAuth`는 존재하지 않음

### 문제
`Request.basicAuth(user, password)` 호출 시 compile error(`Unresolved reference`)가 발생했다. HC5 fluent `Request`에는 `basicAuth` 메서드가 없다.

### 교훈
HC5 fluent API로 Basic Auth를 설정할 때는 `addHeader("Authorization", "Basic <base64>")` 형식으로 직접 추가한다.

---

## L2: Apache HttpComponents 5 timeout 타입은 `java.time.Duration`이 아님

### 문제
`connectTimeout(Duration.ofSeconds(10))` 호출 시 type mismatch compile error가 발생했다. HC5의 `connectTimeout` / `responseTimeout`은 `org.apache.hc.core5.util.Timeout`을 요구한다.

### 교훈
HC5 fluent API timeout은 `Timeout.ofSeconds(N)`를 사용한다. `java.time.Duration`은 `HttpWaitStrategy`(Testcontainers)에서만 사용한다.

---

## L3: `withXxx` 네이밍 — Testcontainers 관례와 충돌

### 문제
Testcontainers 전체 ecosystem에서 `withXxx()` 메서드는 container 시작 전 설정 용도다. 그러나 `withPrometheusDataSource` / `withDashboard`는 시작 후 HTTP 호출이 필요한 메서드다. 코드 리뷰에서 두 리뷰어 모두 지적했다.

### 교훈
Issue에서 지정한 메서드명이라 유지했지만, 향후 유사 pattern 설계 시 `provision*` / `add*` 접두사를 사용하는 편이 의도를 더 명확히 전달한다.

---

## L4: JSON string interpolation — URL 이스케이프 필수

### 문제
`"url":"$prometheusUrl"` 형태의 raw string interpolation은 URL에 `"` 또는 `\`가 포함될 경우 malformed JSON을 만든다.

### 교훈
JSON body에 외부 입력값을 inline할 때는 최소한 `replace("\\", "\\\\").replace("\"", "\\\"")` escaping을 적용한다. 이상적으로는 JSON library를 사용한다.

---

## L5: 테스트 assertion은 동작 결과를 직접 검증해야 함

### 문제
초기 datasource provisioning test는 `server.isRunning.shouldBeTrue()`만 검사했다. Provisioning이 실제로 성공했는지와 무관한 assertion이었다. 코드 리뷰에서 "tautological" 지적을 받았다.

### 교훈
Provisioning/side-effect test는 반드시 해당 동작의 결과를 API 호출로 검증한다. 예: `GET /api/datasources` response body에 `"Prometheus"`가 포함되는지 확인한다.
