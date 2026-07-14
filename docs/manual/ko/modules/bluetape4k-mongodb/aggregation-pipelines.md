---
title: Aggregation pipeline 구성
description: pipeline DSL과 stage helper가 만드는 BSON, native aggregate 실행, field prefix와 검증 경계를 설명합니다.
manualId: bluetape4k-mongodb
chapterId: aggregation-pipelines
---

# Aggregation pipeline 구성

## DSL은 stage 목록을 만든다

`pipeline`은 `MutableList<Bson>`에 builder를 적용하고 `List<Bson>`으로 반환합니다. database command를 실행하거나 결과를 `Flow`로 바꾸지 않습니다.

```kotlin
val stages = pipeline {
    add(matchStage(Filters.gte("score", 80)))
    add(groupStage("city", Accumulators.sum("count", 1)))
    add(sortStage(Sorts.descending("count")))
    add(limitStage(5))
}

val results = collection.aggregate<Document>(stages).toList()
```

실행, codec, cancellation, server error와 result flow는 native `aggregate`의 계약입니다.

## 제공하는 stage helper

| helper | 위임하는 driver API | 입력 경계 |
| --- | --- | --- |
| `matchStage(filter)` | `Aggregates.match` | filter BSON을 그대로 사용 |
| `groupStage(id, ...)` | `Aggregates.group` | id 앞에 `$`를 붙임 |
| `sortStage(sort)` | `Aggregates.sort` | sort BSON을 그대로 사용 |
| `limitStage(limit)` | `Aggregates.limit` | local range 검증 없음 |
| `skipStage(skip)` | `Aggregates.skip` | local range 검증 없음 |
| `projectStage(projection)` | `Aggregates.project` | projection BSON을 그대로 사용 |
| `unwindStage(field)` | `Aggregates.unwind` | field 앞에 `$`를 붙임 |

`groupStage`와 `unwindStage`에는 `$` 없는 field 이름을 전달합니다. `"$city"`처럼 이미 prefix가 있는 값을 넣으면 helper가 다시 `$`를 붙입니다. 복잡한 expression이나 document-form unwind option이 필요하면 native `Aggregates` API를 사용합니다.

## stage 순서가 의미다

pipeline은 list 순서대로 실행됩니다. early `$match`와 필요한 field만 남기는 projection은 처리량을 줄일 수 있지만 index 사용과 optimizer 동작을 실제 explain으로 확인해야 합니다.

`$sort` 뒤 `$skip`·`$limit`을 두는 page와 group 뒤 정렬하는 report는 의미가 다릅니다. helper가 stage를 재배열하거나 안전한 순서를 골라 주지 않습니다.

## BSON 구조 unit test

`AggregationSupportTest`는 MongoDB 없이 각 helper를 `BsonDocument`로 변환해 `$match`, `$group`, `$sort`, `$limit`, `$skip`, `$project`, `$unwind` 구조를 확인합니다. 복합 pipeline에서는 stage 개수도 검증합니다.

이 테스트는 server에서의 결과, index 사용, memory limit, disk spill과 latency를 보장하지 않습니다. `AggregationExamples`가 Testcontainer에서 실제 결과를 확인해 이 간극을 일부 보완합니다.

## 결과 type과 codec

group과 projection을 거치면 원본 document shape가 달라질 수 있습니다. 원본 entity type으로 바로 decode하기보다 결과 전용 type 또는 `Document`를 사용하고 필요한 field를 검증합니다.

`allowDiskUse`, batch size, max time, collation과 hint는 returned `AggregateFlow`에 native driver API로 설정합니다. stage builder에는 이 실행 option이 없습니다.

## 운영 검증

대표 production filter와 data distribution으로 `explain`을 확인합니다. scanned document 수, returned row, stage별 memory, disk spill, execution time을 관찰합니다. pipeline 전체와 민감한 literal을 log에 남기기보다 안정적인 query ID를 부여합니다.

## Source와 tests

- [`AggregationSupport.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupport.kt)
- [`AggregationSupportTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupportTest.kt)
- [`AggregationExamples.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/AggregationExamples.kt)
