# Lessons Learned - currentVertx Lifecycle (2026-07-09)

**Related issue**: #796
**Affected modules**: `bluetape4k-vertx`, `bluetape4k-http`

## L1: Context fallback helpers must define ownership

### Problem

`currentVertx()` created a new `Vertx.vertx()` instance whenever no Vert.x context existed. Helpers such as
`vertxHttpClientOf()` inherited that hidden ownership and could leave event-loop resources unmanaged.

### Lesson

Fallback resource creation must either require an explicit owner or use a managed singleton with a documented close path.
For Vert.x helpers, prefer explicit `Vertx` parameters in lifecycle-sensitive APIs and keep default fallbacks reusable and
closable.

### Verification

- RED: lifecycle tests failed before `closeDefaultVertx`, explicit `Vertx` overload, and default client close API existed.
- GREEN: `:bluetape4k-vertx:test`
- GREEN: `:bluetape4k-http:test`
