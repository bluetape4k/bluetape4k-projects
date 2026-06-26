# Lessons Learned — R2DBC Map Typed Null Binding (2026-06-26)

**Issue**: #823
**Module**: `:bluetape4k-r2dbc`

## L1: Map-based null binding needs an explicit type

### Problem

Map helpers accepted raw null values and invented a default R2DBC type at the
binding boundary. Named and indexed map helpers used `String`, while
`Update.set(parameters)` used `Any` through `setNullable<Any>`.

### Lesson

A raw map null is not equivalent to a typed NULL parameter. Preserve explicit
`Parameter` values and reject raw null entries so callers must provide the
database type at the API boundary.

### Future Guard

When changing parameter-map helpers, test all public surfaces that flow into
`bindMap`: direct named binding, direct indexed binding, update-map setters,
and insert/update DSL paths that store map entries before execution.
