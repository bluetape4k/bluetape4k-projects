package io.bluetape4k.spring.cassandra

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import org.springframework.data.cassandra.core.ReactiveCassandraBatchOperations
import org.springframework.data.cassandra.core.cql.WriteOptions

/**
 * [Flow] 엔티티를 수집해 배치 insert 대상으로 추가합니다.
 *
 * ## 동작/계약
 * - `Flow`를 `toList()`로 모두 수집한 뒤 Reactor `mono`로 감싸 `insert`에 전달합니다.
 * - 반환값은 동일한 [ReactiveCassandraBatchOperations] 체인 객체입니다.
 *
 * ```kotlin
 * val entities = flowOf(group1, group2)
 * // result == batchOps.insertFlow(entities)
 * ```
 */
fun ReactiveCassandraBatchOperations.insertFlow(entities: Flow<*>): ReactiveCassandraBatchOperations =
    insert(mono { entities.toList() })

/**
 * [Flow] 엔티티를 수집해 [WriteOptions]와 함께 배치 insert 대상으로 추가합니다.
 *
 * ## 동작/계약
 * - `Flow`를 리스트로 수집한 뒤 `insert(mono { ... }, options)`에 위임합니다.
 * - `options`는 LWT/TTL 등 Spring Data 배치 옵션 해석 규칙을 그대로 따릅니다.
 *
 * ```kotlin
 * val options = writeOptions { ttl(30) }
 * // result == batchOps.insertFlow(flowOf(group1, group2), options)
 * ```
 */
fun ReactiveCassandraBatchOperations.insertFlow(
    entities: Flow<*>,
    options: WriteOptions,
): ReactiveCassandraBatchOperations =
    insert(mono { entities.toList() }, options)

/**
 * [Flow] 엔티티를 수집해 배치 update 대상으로 추가합니다.
 *
 * ## 동작/계약
 * - `Flow`를 리스트로 수집한 뒤 `update(mono { ... })`를 호출합니다.
 * - 반환값은 후속 배치 체이닝이 가능한 동일 객체입니다.
 *
 * ```kotlin
 * val entities = flowOf(group1, group2)
 * // result == batchOps.updateFlow(entities)
 * ```
 */
fun ReactiveCassandraBatchOperations.updateFlow(entities: Flow<*>): ReactiveCassandraBatchOperations =
    update(mono { entities.toList() })

/**
 * [Flow] 엔티티를 수집해 [WriteOptions]와 함께 배치 update 대상으로 추가합니다.
 *
 * ## 동작/계약
 * - `Flow`를 리스트로 수집한 뒤 `update(mono { ... }, options)`에 전달합니다.
 * - 옵션 적용 여부는 Spring Data의 배치 update 구현에 위임됩니다.
 *
 * ```kotlin
 * val options = writeOptions { ttl(30) }
 * // result == batchOps.updateFlow(flowOf(group1, group2), options)
 * ```
 */
fun ReactiveCassandraBatchOperations.updateFlow(
    entities: Flow<*>,
    options: WriteOptions,
): ReactiveCassandraBatchOperations =
    update(mono { entities.toList() }, options)

/**
 * [Flow] 엔티티를 수집해 배치 delete 대상으로 추가합니다.
 *
 * ## 동작/계약
 * - `Flow`를 리스트로 수집한 뒤 `delete(mono { ... })`를 호출합니다.
 * - 반환값은 동일한 [ReactiveCassandraBatchOperations] 체인 객체입니다.
 *
 * ```kotlin
 * val entities = flowOf(group1, group2)
 * // result == batchOps.deleteFlow(entities)
 * ```
 */
fun ReactiveCassandraBatchOperations.deleteFlow(entities: Flow<*>): ReactiveCassandraBatchOperations =
    delete(mono { entities.toList() })

/**
 * [Flow] 엔티티를 수집해 [WriteOptions]와 함께 배치 delete 대상으로 추가합니다.
 *
 * ## 동작/계약
 * - `Flow`를 리스트로 수집한 뒤 `delete(mono { ... }, options)`에 전달합니다.
 * - 옵션 해석 및 예외 처리는 Spring Data 배치 delete 구현에 위임됩니다.
 *
 * ```kotlin
 * val options = writeOptions { timestamp(1234) }
 * // result == batchOps.deleteFlow(flowOf(group1, group2), options)
 * ```
 */
fun ReactiveCassandraBatchOperations.deleteFlow(
    entities: Flow<*>,
    options: WriteOptions,
): ReactiveCassandraBatchOperations =
    delete(mono { entities.toList() }, options)
