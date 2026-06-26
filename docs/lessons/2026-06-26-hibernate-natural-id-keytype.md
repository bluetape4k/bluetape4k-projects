# Lessons Learned - Hibernate Natural Id KeyType (2026-06-26)

Related issue: #908
Affected module: `:bluetape4k-hibernate`

## L1: Hibernate natural-id helpers must use loader APIs on Hibernate 7

### Problem

`Session.findBySimpleNaturalId()` and `Session.findByNaturalId()` used `Session.find(..., KeyType.NATURAL)`.
`org.hibernate.KeyType` is not available in the Hibernate 7.2 runtime used by the module tests, so the helper tests
failed with `NoClassDefFoundError`.

### Lesson

Use Hibernate's natural-id loader APIs for natural-id lookups:

- `Session.bySimpleNaturalId(entityClass).load(value)` for simple natural ids.
- `Session.byNaturalId(entityClass).using(values).load()` for composite natural ids.

### Future guard

When a helper wraps Hibernate-specific APIs, verify against the exact `testRuntimeClasspath` Hibernate jar before
using removed compatibility constants or overloads.
