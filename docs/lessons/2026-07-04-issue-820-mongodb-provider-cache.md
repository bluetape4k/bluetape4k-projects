# Issue #820 MongoDB Provider Cache Semantics

## Context

`MongoClientProvider.getOrCreate(connectionString, builder)` cached by URL only.
The first caller for a URL decided the effective settings for all later callers
with the same URL, even when those later callers supplied different builder
settings.

## Decision

Cache provider-managed clients by the final immutable `MongoClientSettings`
instead of the raw connection string. Keep the existing overloads source
compatible, but route every overload through the settings cache.

## Outcome

- Same URL plus equal settings returns the same shared client.
- Same URL plus different settings returns different shared clients.
- Provider-owned shared clients now have explicit `close(...)` and `closeAll()`
  lifecycle APIs, and README/KDoc warns callers not to directly close returned
  shared instances.

## Future Rule

When a provider overload accepts a builder or custom options, the cache key must
represent the final effective configuration. Do not cache by only the "base"
identifier when caller-provided settings can change runtime behavior.
