# Lessons Learned - Lettuce Near Cache Clear Failure (2026-06-26)

Related issue: #786
Affected module: `:bluetape4k-cache-lettuce`

## L1: Do not hide backend clear failures behind best-effort cleanup

### Problem

`LettuceNearCache.clearAll()` cleared the local Caffeine cache and then wrapped Redis key cleanup in `runCatching`.
When Redis `SCAN` or `UNLINK` failed, callers saw success even though backend entries could remain and later repopulate
the local cache.

### Lesson

For cache APIs whose contract says "local + backend", backend deletion failure must be visible to the caller unless the
API explicitly models partial success. Blocking and suspend near-cache implementations should expose equivalent failure
semantics.

### Future guard

Failure-path tests should make backend cleanup fail before deletion and assert both that the caller receives an exception
and that the backend key remains. Avoid `runCatching` around required backend operations when the result is ignored.
