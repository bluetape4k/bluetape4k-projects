# Lessons Learned - Issue 1000 Kafka4 Catalog Regression (2026-07-09)

## Context

Nightly(full) run `28964461250` failed in `Test / Infra (kafka-resilience)` after the catalog set `kafka4` back to `4.3.1`.

## Decision

Materialize the source-of-truth `kafka4 = "4.2.1"` compatibility line in `bluetape4k-projects` until Spring Kafka 4.1 embedded-test support is compatible with Kafka 4.3.x.

## Outcome

The failing local equivalent passed after aligning Kafka runtime artifacts to `4.2.1`.

## Future Guard

When `kafka4` changes, check both dependency insight and `:bluetape4k-kafka4:test`. A green dependency sync alone is not sufficient because the failure is an embedded-test ABI mismatch.
