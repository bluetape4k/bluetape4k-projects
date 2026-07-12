---
title: Ordered and parallel Flow
description: Select Flow operators by ordering, parallelism, and downstream capacity.
manualId: bluetape4k-coroutines
chapterId: flow
---

# Ordered and parallel Flow

## Problem to solve

Increasing throughput requires deciding whether output preserves input order or follows completion order.

## Mental model

`flow.async` overlaps internal work while preserving emission order. `mapParallel` uses bounded parallelism and can emit completed results first.

## Smallest API surface

Use standard `map` for sequential transformation, `flow.async` for ordered concurrency, and `mapParallel` for throughput-first work.

## Complete example

For remote enrichment by ID, choose the ordered path when result order is an API contract and bounded `mapParallel` when independent writes can complete in any order.

## Selection guide

Consider ordering, latency variance, and downstream throughput together. `parallelism <= 1` uses the sequential path and avoids unnecessary workers.

## Failure, cancellation, and lifecycle contract

Upstream cancellation must reach active child tasks. Keep parallelism at or below the external connection pool and downstream capacity.

## Operations and diagnosis

Observe in-flight count, buffer size, and item latency distribution. An average alone hides head-of-line blocking.

## Source and representative tests

The contract comes from [`AsyncFlow.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/AsyncFlow.kt), [`mapParallel.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/mapParallel.kt), and their tests.

## Next chapter and runnable workshop

Run the `flow-extensions-parallel-enrichment`, `flow-extensions-race-fallback`, and `flow-extensions-search-pipeline` workshops.
