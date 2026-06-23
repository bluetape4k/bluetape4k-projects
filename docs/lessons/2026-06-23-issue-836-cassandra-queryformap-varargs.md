# Lessons Learned - Cassandra queryForMap varargs (#836, 2026-06-23)

Related issue: #836
Affected module: `:bluetape4k-spring-boot-cassandra`

## L1: Vararg bug reports need real bind-marker evidence

The issue suspected that `queryForMap(cql, args)` passed a single array value
instead of expanding positional bind arguments. A new unit contract and a real
Cassandra integration test with two bind markers both passed before the
production change, which means the original runtime failure was not reproduced
locally.

The fix still made the public helper explicit by calling `queryForMap(cql, *args)`
and updated the KDoc to match that contract. Future changes to Kotlin wrappers
around Java varargs should include both MockK vararg verification and at least one
real bind-marker query when a database binding bug is suspected.

## L2: Do not let mocks encode the misleading shape

The old unit fixture used `any<Array<*>>()`, which reinforced the suspicious
array-shaped forwarding even though the wrapper behaved correctly in the
verified paths. Test fixtures for Java vararg APIs should use `*anyVararg()` or
concrete positional arguments so the test reads like the public contract.
