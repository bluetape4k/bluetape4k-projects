---
title: Structured concurrency policies
description: Match fail-fast, first-success, and supervised partial-result policies to the business result.
manualId: bluetape4k-coroutines
chapterId: structured-concurrency
---

# Structured concurrency policies

Child-failure handling is part of the final result semantics. Decide whether every branch is required, one success is enough, or partial data remains valid.

![Comparison of taskScope, firstSuccessTaskScope, and supervisedTaskScope](../../../assets/coroutines/structured-policies.svg)

## Choose the policy

| Business result | API | Child failure | Read results with |
| --- | --- | --- | --- |
| atomic: all required | `taskScope` / `failFastTaskScope` | cancel unfinished siblings | `join().throwIfFailed()`, then `get()` |
| any: one success is enough | `firstSuccessTaskScope` | keep candidates until winner | `join().result(mapper)` |
| partial: independent results | `supervisedTaskScope` | siblings continue | `results()`, `successfulResults()`, `failedExceptions()` |

All helpers execute a `StructuredTaskScope` through the virtual-thread dispatcher bridge. Caller cancellation must not leave orphan tasks beyond the scope boundary.

## Fail-fast atomic composition

```kotlin
suspend fun loadCheckout(id: String): Checkout = taskScope("checkout") {
    val cart = fork { cartClient.load(id) }
    val price = fork { pricingClient.calculate(id) }
    val stock = fork { inventoryClient.reserve(id) }

    join().throwIfFailed()
    Checkout(cart.get(), price.get(), stock.get())
}
```

One failure invalidates the whole checkout. Do not read subtasks before `throwIfFailed()` or swallow individual failures.

## First-success fallback

```kotlin
suspend fun resolveAddress(query: String): Address = firstSuccessTaskScope {
    fork { primary.resolve(query) }
    fork { secondary.resolve(query) }
    fork { archive.resolve(query) }
    join().result { cause -> AddressNotFound(query, cause) }
}
```

The first failure is not the final failure. The mapper runs only when all providers fail. Resolve idempotency before racing side-effecting providers.

## Supervised partial results

```kotlin
suspend fun loadWidgets(userId: String): List<Result<Widget>> =
    supervisedTaskScope<Widget, List<Result<Widget>>>("widgets") {
        fork { recommendations.load(userId) }
        fork { messages.load(userId) }
        fork { promotions.load(userId) }
        join()
        results()
    }
```

Supervision is not failure suppression. Define how failed widgets appear in the response and which exceptions are recorded in metrics and logs.

## Async bridge

`CoroutineScope.asyncTaskScope` and `asyncSupervisedTaskScope` return a `Deferred` immediately. Use them to compose multiple structured scopes in a larger coroutine operation. The receiver scope owns the returned deferred.

## Verification checklist

1. sibling state after failure matches the chosen policy;
2. first-success chooses a success rather than the first failure;
3. all-provider failure maps to a domain exception;
4. supervised results expose successes and failures;
5. no unfinished task remains after parent cancellation or deadline;
6. deployment JDK/runtime supports the virtual-thread bridge.

## Source and representative tests

- [`StructuredConcurrency.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/StructuredConcurrency.kt)
- [`StructuredConcurrencyTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/StructuredConcurrencyTest.kt)
- Core virtual-thread scope implementations live in the `bluetape4k-core` concurrency package.

Continue with [Operations and observability](./operations.md) to operate and shut down the selected policy.
