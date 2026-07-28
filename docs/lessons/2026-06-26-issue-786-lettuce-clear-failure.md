# Lettuce near cache clear failure 교훈 (2026-06-26)

관련 이슈: #786
영향 module: `:bluetape4k-cache-lettuce`

## L1: backend clear failure를 best-effort cleanup 뒤에 숨기지 않는다

### 문제

`LettuceNearCache.clearAll()`은 local Caffeine cache를 지운 뒤 Redis key cleanup을
`runCatching`으로 감쌌다. Redis `SCAN` 또는 `UNLINK`가 실패해도 caller는 성공을 봤고,
backend entry가 남아 나중에 local cache를 다시 채울 수 있었다.

### 교훈

cache API contract가 "local + backend"라면 API가 partial success를 명시적으로 모델링하지
않는 한 backend deletion failure는 caller에게 보여야 한다. blocking/suspend near-cache
implementation은 동등한 failure semantic을 노출해야 한다.

### 향후 가드

failure-path test는 deletion 전에 backend cleanup을 실패시키고, caller가 exception을 받으며
backend key가 남아 있음을 함께 assert해야 한다. 결과를 무시하는 필수 backend operation
주변에서는 `runCatching`을 피한다.
