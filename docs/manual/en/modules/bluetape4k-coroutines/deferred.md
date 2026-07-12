---
title: Deferred coordination
description: Distinguish first completion, first success, and loser cancellation policies.
manualId: bluetape4k-coroutines
chapterId: deferred
---

# Deferred coordination

## Problem to solve

When several asynchronous tasks compete, define what to wait for and what happens to the remaining tasks.

## Mental model

First completion includes failure and cancellation, while first success skips failures until a value succeeds. Winner selection and loser cleanup are separate policies.

## Smallest API surface

Use `DeferredValue.await()` for one owned value, `awaitAny` for first completion, `awaitAnyAndCancelOthers` when losers must stop, and `firstSuccessTaskScope` for first success.

## Complete example

A replica race starts every `Deferred` in the caller scope, invokes the selected coordination function, and either retains or cancels losers according to the chosen policy.

## Selection guide

Choose first completion for the fastest response and first success when a failed replica should be skipped. Do not auto-cancel losers when their results are still needed for warming or comparison.

## Failure, cancellation, and lifecycle contract

`DeferredValue` starts eagerly in an owned scope and therefore must be closed. Prefer suspending `await()` over the deprecated blocking `value` access.

## Operations and diagnosis

Measure loser execution and cancellation completion as well as winner latency. Repeated uncancelled races can exhaust connections and threads.

## Source and representative tests

The evidence is [`DeferredValue.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/DeferredValue.kt), [`DeferredSupport.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/DeferredSupport.kt), and their representative tests.

## Next chapter and runnable workshop

Continue with [Ordered and parallel Flow](./flow.md) for stream concurrency and [Structured concurrency](./structured-concurrency.md) for policy scopes.
