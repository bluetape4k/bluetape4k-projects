# HC5 Production HTTP Client Tuning Defaults

**날짜**: 2026-05-24
**이슈**: #582
**브랜치**: feat/http-client-tuning-defaults-20260524

## 문제

`bluetape4k-http`는 Apache HttpComponents 5(HC5)에 대한 low-level DSL builder만 expose했다.
Caller는 connection pool sizing, eviction, keep-alive fallback, retry, request timeout을 직접 연결해야
했고, 하나만 잘못 설정해도 production에서 connection leak 또는 silent hang이 발생할 수 있었다.

## 해결

모든 필수 tuning을 한 번에 적용하면서도 각 parameter를 개별 override할 수 있는 named-parameter factory
function을 추가했다:

| 함수 | Layer |
|----------|-------|
| `productionRequestConfigOf()` | `hc5.http` — timeout: 5 s pool-wait / 10 s connect / 30 s response |
| `defaultKeepAliveStrategy()` | `hc5.http` — server가 `Keep-Alive` header를 생략할 때 60 s fallback |
| `defaultRetryStrategy()` | `hc5.http` — retry 3회, interval 1 s |
| `productionHttpClientOf()` | `hc5.classic` — eviction + keep-alive + retry + timeout |
| `productionVirtualThreadHttpClientOf()` | `hc5.classic` — `productionHttpClientOf`에 위임 |
| `productionHttpAsyncClientOf()` | `hc5.async` — async equivalent |

## HC5 API 확인 사항(javap로 검증)

- `evictExpiredConnections()`와 `evictIdleConnections(TimeValue)`는
  **`HttpClientBuilder`**와 **`HttpAsyncClientBuilder`**에 있으며
  `PoolingHttpClientConnectionManagerBuilder`에는 없다.
- `DefaultConnectionKeepAliveStrategy.getKeepAliveDuration()`은 server가 `Keep-Alive` header를 생략하면
  **negative** `TimeValue`를 반환한다. Fallback에는 `duration.duration < 0`을 확인한다.
- HC5 5.6.1의 `PoolingHttpClientConnectionManagerBuilder`에는 **`setThreadFactory` API가 없다**.
  `productionVirtualThreadHttpClientOf`의 "VirtualThread"는 calling context만 의미한다.

## Code review 지적(Step 6-R)

| 심각도 | 지적 | 해결 |
|----------|---------|------------|
| CRITICAL | `productionHttpAsyncClientOf` test가 0개 | `ProductionHttpAsyncClientTest` 추가(4 tests, all pass) |
| HIGH | `productionVirtualThreadHttpClientOf`가 내부에서 VT를 wiring하지 않음 | HC5 5.x limitation을 KDoc에 문서화 |

## 테스트 결과

| Test class | Tests | Pass |
|-----------|-------|------|
| `ProductionRequestConfigTest` | 6 | 6 ✅ |
| `ProductionHttpClientTest` | 7 | 7 ✅ |
| `ProductionHttpAsyncClientTest` | 4 | 4 ✅ |
| **Total** | **17** | **17** |

## 교훈

1. **HC5 API 위치는 구현 전에 항상 javap로 확인한다.** Eviction은 connection manager builder가 아니라
   client builder에 있다. 기존 code grep만으로는 잡지 못한다.
2. **새 public function은 test가 필요하다.** Reviewer가 missing async test를 잡았다. Review 전에 test를 추가한다.
3. **"VirtualThread" naming은 신중하게 문서화한다.** HC5는 thread factory API를 expose하지 않으므로 caller를
   오해시키지 않게 이름과 KDoc을 맞춘다.
