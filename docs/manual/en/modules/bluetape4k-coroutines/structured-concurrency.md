---
title: Structured concurrency policies
description: Select fail-fast, first-success, or supervised partial-result policies.
manualId: bluetape4k-coroutines
chapterId: structured-concurrency
---

# Structured concurrency policies

## Problem to solve

A scope policy must define what happens to siblings and the final result when one child fails.

## Mental model

Fail-fast invalidates the whole result, first-success accepts the earliest successful value, and supervision isolates independent failures.

## Smallest API surface

Choose `taskScope`, `firstSuccessTaskScope`, or `supervisedTaskScope` from the meaning of the result.

## Complete example

Use fail-fast when both providers are required, first-success when one replica is enough, and supervised partial results for independent dashboard widgets.

## Selection guide

Decide whether the business result is atomic, whether partial data remains valid, or whether only one successful value is required.

## Failure, cancellation, and lifecycle contract

No child should remain after the scope ends. Align loser cancellation and failure ordering with the exception contract visible to the caller.

## Operations and diagnosis

Separate success, failure, cancellation, and total latency by policy. Supervision must not hide failures that require action.

## Source and representative tests

[`StructuredConcurrency.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/StructuredConcurrency.kt) and its policy tests define the contract.

## Next chapter and runnable workshop

See [Deferred coordination](./deferred.md) for races and [Operations](./operations.md) for observability.
