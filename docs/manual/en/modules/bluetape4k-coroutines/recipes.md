---
title: Recipes and workshops
description: Assemble request composition, replica races, Flow transforms, and callback bridges.
manualId: bluetape4k-coroutines
chapterId: recipes
---

# Recipes and workshops

## Problem to solve

Production examples must combine ownership, failure policy, ordering, and capacity rather than demonstrate one API in isolation.

## Mental model

Each recipe closes its input boundary, child policy, result ordering, and resource cleanup as one unit.

## Smallest API surface

Use only the required subset of `coroutineScope`, `async`, `awaitAny`, `firstSuccessTaskScope`, `mapParallel`, and Subjects.

## Complete example

The recipe set covers two suspend calls in one request, fastest and first-success replicas, ordered and throughput-first transforms, and callback-to-Subject adaptation.

## Selection guide

Record result completeness, ordering, acceptable parallelism, and cleanup on caller cancellation before choosing a recipe.

## Failure, cancellation, and lifecycle contract

Every recipe propagates parent cancellation and closes owned resources in `finally` or `close()`.

## Operations and diagnosis

Set a latency and in-flight bound for each recipe, then verify that timeout and retry layers do not multiply work unexpectedly.

## Source and representative tests

Each recipe is grounded in library tests and the matching module under `/Users/debop/work/bluetape4k/bluetape4k-workshop/kotlin`.

## Next chapter and runnable workshop

Run the Flow extensions, Ktor REST coroutines, Spring WebFlux coroutines, and observability workshops according to the target scenario.
