package io.bluetape4k.mongodb

import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.result.UpdateResult
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson

/**
 * 필터 조건에 맞는 첫 번째 문서를 반환합니다.
 *
 * `find(filter).limit(1).firstOrNull()`의 축약형입니다.
 *
 * ```kotlin
 * val user = collection.findFirst(Filters.eq("name", "Alice"))
 * ```
 *
 * @param filter 검색 조건
 * @return 첫 번째 일치 문서 또는 `null`
 */
suspend fun <T: Any> MongoCollection<T>.findFirst(filter: Bson): T? =
    find(filter).limit(1).firstOrNull()

/**
 * 필터 조건에 맞는 첫 번째 문서를 반환합니다. [findFirst]의 별칭입니다.
 *
 * @param filter 검색 조건
 * @return 첫 번째 일치 문서 또는 `null`
 */
suspend fun <T: Any> MongoCollection<T>.findFirstOrNull(filter: Bson): T? =
    findFirst(filter)

/**
 * 필터 조건에 맞는 문서가 존재하는지 확인합니다.
 *
 * ```kotlin
 * val exists = collection.exists(Filters.eq("name", "Alice"))
 * ```
 *
 * @param filter 검색 조건
 * @return 일치 문서가 하나 이상 존재하면 `true`
 */
suspend fun <T: Any> MongoCollection<T>.exists(filter: Bson): Boolean =
    countDocuments(filter) > 0

/**
 * 필터 조건에 맞는 문서를 업서트(존재하면 업데이트, 없으면 삽입)합니다.
 *
 * ```kotlin
 * val result = collection.upsert(
 *     filter = Filters.eq("name", "Alice"),
 *     update = Updates.set("age", 30)
 * )
 * ```
 *
 * @param filter 대상 문서 검색 조건
 * @param update 적용할 업데이트 명세
 * @return [UpdateResult] 업서트 결과
 */
suspend fun <T: Any> MongoCollection<T>.upsert(filter: Bson, update: Bson): UpdateResult =
    updateOne(filter, update, UpdateOptions().upsert(true))

/**
 * 필터, skip, limit, sort를 적용하여 문서를 [Flow]로 반환합니다.
 *
 * 네이티브 `find()`가 이미 `Flow<T>`를 구현한 `FindFlow<T>`를 반환하므로,
 * 이 함수는 공통 옵션을 한 번에 설정하는 편의 함수입니다.
 *
 * ```kotlin
 * val flow = collection.findAsFlow(
 *     filter = Filters.gt("age", 20),
 *     skip = 10,
 *     limit = 5,
 *     sort = Sorts.ascending("name")
 * )
 * flow.collect { println(it) }
 * ```
 *
 * @param filter 검색 조건 (기본값: 전체 문서)
 * @param skip 건너뛸 문서 수
 * @param limit 반환할 최대 문서 수
 * @param sort 정렬 조건
 * @return 조건에 맞는 문서의 [Flow]
 */
fun <T: Any> MongoCollection<T>.findAsFlow(
    filter: Bson = Filters.empty(),
    skip: Int? = null,
    limit: Int? = null,
    sort: Bson? = null,
): Flow<T> {
    var findFlow = find(filter)
    if (skip != null) findFlow = findFlow.skip(skip)
    if (limit != null) findFlow = findFlow.limit(limit)
    if (sort != null) findFlow = findFlow.sort(sort)
    return findFlow
}
