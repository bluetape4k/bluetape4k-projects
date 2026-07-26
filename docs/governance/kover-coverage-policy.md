# Kover Coverage Policy

## Current Status

`bluetape4k-projects` generates broad Kover XML artifacts in Nightly and aggregates coverage summaries across a large matrix. It does not enforce a broad repository-wide coverage gate.

## Policy

Status: report-only transition.

This monorepo contains core libraries, infrastructure clients, Spring Boot modules, virtual-thread modules, and Testcontainers support. Thresholds must be introduced module-by-module rather than as one aggregate gate.

## Threshold Plan

- Treat Kover as a trend signal, not a build gate.
- Use Nightly XML reports and existing coverage artifact uploads to identify coverage regressions.
- Open a focused issue when a module needs coverage repair; do not introduce a failing threshold as the default enforcement mechanism.
- `testing/testcontainers` requires artifact production checks before threshold changes because matrix partitioning can make coverage appear artificially low.
- Benchmark/generated/test fixture code should remain explicitly excluded.

## CI/Nightly Contract

Nightly aggregates Kover XML artifacts and keeps trend visibility. CI and Nightly must not fail solely because a module is below a fixed coverage percentage unless a future issue explicitly reintroduces that gate.
