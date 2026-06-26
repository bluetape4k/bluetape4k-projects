# Issue 809 - Cassandra Session Cache Identity

## Context

`CqlSessionProvider` cached sessions by keyspace name only. A process using the same keyspace name across tenants, endpoints, credentials, or client settings could silently reuse the first session.

## Decision

Use `CqlSessionIdentity` as the cache key. The compatibility overload derives a conservative per-call identity from the builder supplier and builder lambda so different builder blocks no longer collide by keyspace alone. For stable same-context reuse across call sites, callers should use the explicit `CqlSessionIdentity` overload.

## Outcome

Regression tests now prove that same identity reuses a session while the same keyspace with different connection context does not reuse the earlier session. README examples document when to use explicit identity.

## Future Guidance

Do not cache infrastructure clients by a logical namespace alone when connection, credential, or tenant context can differ. Add an explicit identity object before relying on implicit builder state.
