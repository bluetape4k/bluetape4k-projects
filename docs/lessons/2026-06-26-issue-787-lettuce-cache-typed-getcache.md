# Lessons Learned - Lettuce Typed Cache Lookup (2026-06-26)

Related issue: #787
Affected module: `:bluetape4k-cache-lettuce`

## L1: Typed cache lookup must fail at the JCache boundary

### Problem

`LettuceCacheManager.getCache(cacheName, keyType, valueType)` ignored the requested key and value classes and
returned the cached instance with an unchecked cast. Callers could therefore receive a cache whose configured types
did not match the typed lookup request.

### Lesson

JCache typed lookup is a boundary check, not only a Kotlin generic convenience. Resolve the named cache first, then
compare the cache configuration's `keyType` and `valueType` with the requested classes before returning the cache.
Type mismatches should fail immediately with `ClassCastException`.

### Future guard

Cache manager changes should keep regression coverage for exact type matches, key mismatches, value mismatches,
null type arguments, and closed-manager behavior.
