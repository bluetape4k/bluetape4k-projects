---
title: Validation and invariants
description: Select validation functions for public input and internal invariants.
manualId: bluetape4k-core
chapterId: validation
---

# Validation and invariants

## Problem to solve

Reject invalid values at the boundary while preserving exception meaning and the validated value.

## Mental model

Invalid public arguments map to `IllegalArgumentException`; invalid state in an existing object maps to `IllegalStateException`.

## Smallest API surface

Start with Kotlin `require` and `check`, then use Bluetape `require*` helpers for repeated null, blank, and collection conditions.

## Complete example

Validate a string with `requireNotBlank` and pass the returned receiver directly to the next transformation without another non-null assertion.

## Selection guide

First distinguish caller input from internal state. Do not replace a domain error with a generic precondition exception.

## Failure, cancellation, and lifecycle contract

Validate before side effects and leave no partial state after failure. Error messages must not expose secret values.

## Operations and diagnosis

Record caller validation failures separately from server invariant violations.

## Source and representative tests

[`RequireSupport.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/support/RequireSupport.kt) and its tests define receiver return and exception behavior.

## Next chapter and runnable workshop

Continue with [Bounded collections](./bounded-collections.md) for bounded storage of validated values.
