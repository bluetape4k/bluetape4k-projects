# Kover Coverage Policy

## Current Status

`bluetape4k-projects` generates broad Kover XML artifacts in Nightly and
aggregates coverage summaries across a large matrix. It does not enforce a
broad repository-wide `koverVerify` gate.

## Policy

Status: report-only transition.

This monorepo contains core libraries, infrastructure clients, Spring Boot
modules, virtual-thread modules, and Testcontainers support. Thresholds must be
introduced module-by-module rather than as one aggregate gate.

## Threshold Plan

- Core and utility modules should target 80% after current baselines are
  recorded.
- Integration-heavy infrastructure modules should start at 60-70% with
  documented rationale.
- `testing/testcontainers` requires artifact production checks before threshold
  changes because matrix partitioning can make coverage appear artificially low.
- Benchmark/generated/test fixture code should remain explicitly excluded.

## CI/Nightly Contract

Nightly aggregates Kover XML artifacts. Add `koverVerify` only for modules with
validated thresholds, and keep those tasks in the relevant matrix group.
