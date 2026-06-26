# Lessons Learned - Hibernate Cache Key Encoding (2026-06-26)

Related issue: #788
Affected module: `:bluetape4k-hibernate-cache-lettuce`

## L1: Cache key collision claims need public-path collision tests

### Problem

`LettuceNearCacheStorageAccess` normalized Hibernate keys with delimiter joins and `toString()`.
That erased natural-id arity, array/scalar boundaries, and custom identifier state when `toString()` values matched.
The README claimed Redis key collision prevention, but tests only checked that readable key fragments existed.

### Lesson

When a cache bridge claims key collision resistance, cover the bridge's public storage path with adversarial keys:

- a single delimiter-containing natural-id value versus a composite natural-id array,
- scalar identifier text versus object-array identifier text,
- custom identifiers with identical `toString()` output but different serialized state.

### Future guard

Do not use readable delimiter strings as distributed cache keys unless every component is length-prefixed or otherwise
typed. Prefer a versioned canonical digest key when the raw key can contain user values or Hibernate-disassembled
objects.
