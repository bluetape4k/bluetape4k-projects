# 배운 점 — Prometheus + Grafana Integration Test (2026-05-15)

**관련 PR**: #462
**영향 모듈**: `bluetape4k-testcontainers` (`testing/testcontainers`)

## L1: Testcontainers reuse=true 환경에서 datasource 체크는 URL까지 검증해야 한다

### 문제
`datasourceExists()`가 Grafana API `/api/datasources/name/Prometheus`의 HTTP 200만 확인했다.
`testcontainers.reuse.enable=true`가 활성화된 환경에서는 이전 실행의 Prometheus 컨테이너 URL이
남아있는 datasource에 cache되어, URL이 달라진 새 container에서 setup이 skip되고 stale 상태로 test가 진행됐다.

### 교훈
datasource idempotency check는 **name 존재** + **URL 일치** 두 조건을 모두 만족해야 한다.
응답 body에 현재 `prometheus.url`이 포함되는지 확인하는 `datasourceUpToDate()` pattern을 사용한다.

```kotlin
private fun datasourceUpToDate(): Boolean {
    val body = runCatching {
        Request.get("${grafana.url}/api/datasources/name/Prometheus")
            .addHeader("Authorization", basicAuth())
            .execute().returnContent().asString()
    }.getOrNull() ?: return false
    return body.contains(prometheus.url)
}
```

---

## L2: Grafana dashboard overwrite:true는 UID 기반으로 동작한다

### 문제
`withDashboard()`가 내부적으로 `"overwrite":true`를 전송하지만 Grafana는 UID로 기존 dashboard를 식별한다.
UID가 없으면 같은 title의 dashboard가 reuse container에 중복 생성될 수 있다.
또한 `/api/search?type=dash-db`로 검색하면 title 중복 시 여러 결과가 반환되어 assertion이 불안정해진다.

### 교훈
- Dashboard JSON에는 **stable `uid`** 를 항상 포함한다.
- 검증은 `/api/search` 대신 `/api/dashboards/uid/<uid>`로 직접 조회한다(결정론적).

```kotlin
private const val DASHBOARD_UID = "bluetape4k-prometheus-integration"
private val DASHBOARD_JSON =
    """{"uid":"$DASHBOARD_UID","title":"Prometheus Integration","panels":[],"schemaVersion":36}"""

// 검증
Request.get("${grafana.url}/api/dashboards/uid/$DASHBOARD_UID")
```

---

## L3: Launcher 싱글턴 패턴과 Docker 네트워크 제약

### 문제
`Launcher.prometheus` / `Launcher.grafana` singleton은 container 생성 시점에 network를 설정해야 한다.
생성 후 `withNetwork()`를 호출할 수 없으므로, 두 container 간 실제 Docker 내부 통신
(`grafana → prometheus`)은 singleton pattern으로는 지원하지 않는다.

결과적으로 `withPrometheusDataSource(prometheus.url)`에서 전달되는 URL은
test JVM에서 접근 가능한 host-mapped URL이고, Grafana container 내부에서는 `localhost`를
자신의 컨테이너로 해석해 실제 연결이 되지 않는다. 기존 `GrafanaServerTest`도 동일 패턴.

### 교훈
- Launcher singleton pattern은 **단일 container 검증**에 적합하다.
- 두 container 간 실제 network 통신이 필요한 test는 test-scoped container + 공유 `Network` 객체를 사용한다.
- 이 제약은 acceptance criteria에 명시되지 않았으므로 known limitation으로 수용했다.
