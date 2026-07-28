# Lettuce typed cache lookup 교훈 (2026-06-26)

관련 이슈: #787
영향 module: `:bluetape4k-cache-lettuce`

## L1: typed cache lookup은 JCache boundary에서 실패해야 한다

### 문제

`LettuceCacheManager.getCache(cacheName, keyType, valueType)`는 요청된 key/value class를
무시하고 cached instance를 unchecked cast로 반환했다. caller는 configured type이 typed
lookup request와 맞지 않는 cache를 받을 수 있었다.

### 교훈

JCache typed lookup은 Kotlin generic convenience가 아니라 boundary check다. 먼저 named
cache를 resolve한 뒤, cache configuration의 `keyType`과 `valueType`을 요청 class와 비교하고
나서 cache를 반환한다. type mismatch는 즉시 `ClassCastException`으로 실패해야 한다.

### 향후 가드

cache manager 변경에는 exact type match, key mismatch, value mismatch, null type argument,
closed-manager behavior에 대한 regression coverage를 유지한다.
