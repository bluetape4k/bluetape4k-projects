package io.bluetape4k.mongodb.aggregation

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.BsonField
import org.bson.conversions.Bson

/**
 * Aggregation Pipeline을 DSL 빌더로 구성합니다.
 *
 * 네이티브 `aggregate(pipeline)` 함수가 이미 `AggregateFlow<T>`(`Flow<T>` 구현체)를
 * 반환하므로, 이 함수는 파이프라인 스테이지 목록 구성에만 집중합니다.
 *
 * ```kotlin
 * val stages = pipeline {
 *     add(matchStage(Filters.gt("age", 20)))
 *     add(groupStage("city", Accumulators.sum("count", 1)))
 *     add(sortStage(Sorts.descending("count")))
 *     add(limitStage(10))
 * }
 * val flow = collection.aggregate<Document>(stages)
 * ```
 *
 * @param builder [MutableList]에 스테이지를 추가하는 람다
 * @return 구성된 파이프라인 스테이지 목록
 */
fun pipeline(builder: MutableList<Bson>.() -> Unit): List<Bson> =
    mutableListOf<Bson>().apply(builder)

/**
 * `$match` 스테이지를 생성합니다.
 *
 * @param filter 필터 조건
 * @return `$match` 스테이지 [Bson]
 */
fun matchStage(filter: Bson): Bson = Aggregates.match(filter)

/**
 * `$group` 스테이지를 생성합니다.
 *
 * `id`는 그룹화 기준 필드명입니다. 필드명을 전달하면 `$fieldName` 형식으로 변환됩니다.
 * 복잡한 표현식이 필요한 경우 직접 `Aggregates.group()`을 사용하세요.
 *
 * ```kotlin
 * groupStage("city", Accumulators.sum("count", 1))
 * ```
 *
 * @param id 그룹화 기준 필드명 (`$` 접두어 없이 전달)
 * @param accumulators 누산기 필드 목록
 * @return `$group` 스테이지 [Bson]
 */
fun groupStage(id: String, vararg accumulators: BsonField): Bson =
    Aggregates.group("\$$id", *accumulators)

/**
 * `$sort` 스테이지를 생성합니다.
 *
 * @param sort 정렬 조건
 * @return `$sort` 스테이지 [Bson]
 */
fun sortStage(sort: Bson): Bson = Aggregates.sort(sort)

/**
 * `$limit` 스테이지를 생성합니다.
 *
 * @param limit 반환할 최대 문서 수
 * @return `$limit` 스테이지 [Bson]
 */
fun limitStage(limit: Int): Bson = Aggregates.limit(limit)

/**
 * `$skip` 스테이지를 생성합니다.
 *
 * @param skip 건너뛸 문서 수
 * @return `$skip` 스테이지 [Bson]
 */
fun skipStage(skip: Int): Bson = Aggregates.skip(skip)

/**
 * `$project` 스테이지를 생성합니다.
 *
 * @param projection 프로젝션 조건
 * @return `$project` 스테이지 [Bson]
 */
fun projectStage(projection: Bson): Bson = Aggregates.project(projection)

/**
 * `$unwind` 스테이지를 생성합니다.
 *
 * `field`는 배열 필드명입니다. `$` 접두어 없이 전달하면 자동으로 추가됩니다.
 *
 * ```kotlin
 * unwindStage("tags") // -> $unwind: "$tags"
 * ```
 *
 * @param field 언와인드할 배열 필드명 (`$` 접두어 없이 전달)
 * @return `$unwind` 스테이지 [Bson]
 */
fun unwindStage(field: String): Bson = Aggregates.unwind("\$$field")
