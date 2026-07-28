# 이슈 644 Ktor Resilience4j 통합

## 배경

issue #644는 Ktor module family epic의 후속으로, 첫 server-side Ktor extension point가
검증된 뒤 optional Resilience4j integration을 요청했다.

## 결정

`bluetape4k-ktor-resilience4j`를 얇은 route-scoped module로 추가한다. 기존
`bluetape4k-resilience4j` coroutine facade를 재사용하고 policy object는 caller-owned로
유지한다.

## 결과

module은 이제 다음을 제공한다.

- `KtorResiliencePolicies`
- `withKtorResilience`
- `resilientRoute`, `resilientGet`, `resilientPost`
- `bluetape4kResilienceErrors`

status mapping은 generic하게 둔다. open circuit은 503, rate limited는 429, timeout은
504다. cancellation은 rethrow하며 circuit breaker failure로 집계하지 않는다.

## 검증

- `./gradlew :bluetape4k-ktor-resilience4j:test --no-configuration-cache`

## 향후 가드

이 module은 route/block scope로 유지한다. 여기에 global Ktor plugin, registry creation,
auth, tracing, OpenAPI behavior를 추가하지 않는다.
