---
title: Aggregation pipeline construction
description: Build BSON stages with the pipeline DSL while keeping native aggregate execution, field prefixes, and validation boundaries explicit.
manualId: bluetape4k-mongodb
chapterId: aggregation-pipelines
---

# Aggregation pipeline construction

## The DSL builds a stage list

`pipeline` applies a builder to `MutableList<Bson>` and returns it as `List<Bson>`. It does not execute a database command or convert results to `Flow`.

```kotlin
val stages = pipeline {
    add(matchStage(Filters.gte("score", 80)))
    add(groupStage("city", Accumulators.sum("count", 1)))
    add(sortStage(Sorts.descending("count")))
    add(limitStage(5))
}

val results = collection.aggregate<Document>(stages).toList()
```

Execution, codecs, cancellation, server errors, and the result flow remain native `aggregate` behavior.

## Provided stage helpers

| Helper | Delegated driver API | Input boundary |
| --- | --- | --- |
| `matchStage(filter)` | `Aggregates.match` | Uses filter BSON as-is |
| `groupStage(id, ...)` | `Aggregates.group` | Adds `$` before the id |
| `sortStage(sort)` | `Aggregates.sort` | Uses sort BSON as-is |
| `limitStage(limit)` | `Aggregates.limit` | No local range validation |
| `skipStage(skip)` | `Aggregates.skip` | No local range validation |
| `projectStage(projection)` | `Aggregates.project` | Uses projection BSON as-is |
| `unwindStage(field)` | `Aggregates.unwind` | Adds `$` before the field |

Pass field names without `$` to `groupStage` and `unwindStage`. Supplying an already prefixed value makes the helper add another `$`. Use native `Aggregates` for complex expressions or document-form unwind options.

## Stage order carries meaning

MongoDB executes the list in order. An early `$match` and a projection that removes unused fields can reduce work, but confirm index use and optimizer behavior through explain output.

A page with `$sort`, `$skip`, and `$limit` differs from a report that groups before sorting. The helper neither reorders stages nor selects a safe sequence.

## BSON structure unit tests

`AggregationSupportTest` runs without MongoDB. It converts each helper result to `BsonDocument` and inspects `$match`, `$group`, `$sort`, `$limit`, `$skip`, `$project`, and `$unwind`. It also checks composite pipeline size.

Those tests do not prove server results, index use, memory limits, disk spill, or latency. Testcontainer-backed `AggregationExamples` covers representative server results.

## Result types and codecs

Grouping and projection can change the document shape. Prefer a result-specific type or `Document` over decoding directly into the original entity, and validate required fields.

Configure `allowDiskUse`, batch size, max time, collation, and hints on the returned native `AggregateFlow`. The stage builder has no execution options.

## Operational validation

Run explain against representative production filters and data distributions. Observe scanned documents, returned rows, stage memory, disk spill, and execution time. Assign a bounded query ID instead of logging entire pipelines with sensitive literals.

## Sources and tests

- [`AggregationSupport.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupport.kt)
- [`AggregationSupportTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupportTest.kt)
- [`AggregationExamples.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/AggregationExamples.kt)
