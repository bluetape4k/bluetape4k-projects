package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.querybuilder.delete.Delete
import com.datastax.oss.driver.api.querybuilder.delete.DeleteSelection
import com.datastax.oss.driver.api.querybuilder.insert.Insert
import com.datastax.oss.driver.api.querybuilder.update.Update
import com.datastax.oss.driver.api.querybuilder.update.UpdateStart
import org.springframework.data.cassandra.core.DeleteOptions
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.data.cassandra.core.cql.QueryOptions
import org.springframework.data.cassandra.core.cql.WriteOptions

/**
 * [QueryOptions]를 빌더 DSL로 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `QueryOptions.builder().apply(builder).build()`를 호출합니다.
 * - 인자 검증/예외는 Spring Data Cassandra의 빌더 구현에 위임됩니다.
 *
 * ```kotlin
 * val options = queryOptions { pageSize(100) }
 * // result == (options.pageSize == 100)
 * ```
 */
inline fun queryOptions(
    builder: QueryOptions.QueryOptionsBuilder.() -> Unit,
): QueryOptions =
    QueryOptions.builder().apply(builder).build()

/**
 * [InsertOptions]를 빌더 DSL로 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `InsertOptions.builder().apply(builder).build()`를 호출합니다.
 * - `withIfNotExists()` 같은 LWT 옵션 조합은 전달한 `builder` 내용 그대로 반영됩니다.
 *
 * ```kotlin
 * val options = insertOptions { withIfNotExists() }
 * // result == options.ifNotExists
 * ```
 */
inline fun insertOptions(
    builder: InsertOptions.InsertOptionsBuilder.() -> Unit,
): InsertOptions =
    InsertOptions.builder().apply(builder).build()

/**
 * [UpdateOptions]를 빌더 DSL로 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `UpdateOptions.builder().apply(builder).build()`를 호출합니다.
 * - 인자 검증/예외는 Spring Data Cassandra의 빌더 구현에 위임됩니다.
 *
 * ```kotlin
 * val options = updateOptions { timeout(500) }
 * // result == (options.timeout?.toMillis() == 500L)
 * ```
 */
inline fun updateOptions(
    builder: UpdateOptions.UpdateOptionsBuilder.() -> Unit,
): UpdateOptions =
    UpdateOptions.builder().apply(builder).build()

/**
 * [WriteOptions]를 빌더 DSL로 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `WriteOptions.builder().apply(builder).build()`를 호출합니다.
 * - `ttl`, `timestamp` 설정 여부는 이후 `addWriteOptions` 확장에서 그대로 사용됩니다.
 *
 * ```kotlin
 * val options = writeOptions { ttl(30) }
 * // result == options.isPositiveTtl
 * ```
 */
inline fun writeOptions(
    builder: WriteOptions.WriteOptionsBuilder.() -> Unit,
): WriteOptions =
    WriteOptions.builder().apply(builder).build()

/**
 * [DeleteOptions]를 빌더 DSL로 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `DeleteOptions.builder().apply(builder).build()`를 호출합니다.
 * - 인자 검증/예외는 Spring Data Cassandra의 빌더 구현에 위임됩니다.
 *
 * ```kotlin
 * val options = deleteOptions { timeout(300) }
 * // result == (options.timeout?.toMillis() == 300L)
 * ```
 */
inline fun deleteOptions(
    builder: DeleteOptions.DeleteOptionsBuilder.() -> Unit,
): DeleteOptions =
    DeleteOptions.builder().apply(builder).build()


/**
 * [Insert] 문에 [WriteOptions]의 TTL/타임스탬프를 반영한 새 Statement를 반환합니다.
 *
 * ## 동작/계약
 * - [WriteOptions.isPositiveTtl]가 `true`이면 `usingTtl`을 적용합니다.
 * - `timestamp`가 존재하면 `usingTimestamp`를 적용합니다.
 * - 원본 인스턴스를 직접 변경하지 않고 적용된 Statement를 새로 반환합니다.
 *
 * ```kotlin
 * val options = writeOptions { ttl(30) }
 * // result == options.isPositiveTtl
 * ```
 */
fun Insert.addWriteOptions(writeOptions: WriteOptions): Insert {
    var applied = this

    if (writeOptions.isPositiveTtl) {
        applied = applied.usingTtl(writeOptions.ttl!!.seconds.toInt())
    }
    writeOptions.timestamp?.run {
        applied = applied.usingTimestamp(this)
    }
    return applied
}

/**
 * [Update] 문이 [UpdateStart]인 경우에만 [WriteOptions]의 TTL/타임스탬프를 반영합니다.
 *
 * ## 동작/계약
 * - 수신 객체가 [UpdateStart]가 아니면 아무 옵션도 적용하지 않고 그대로 반환합니다.
 * - [WriteOptions.isPositiveTtl]가 `true`이면 `usingTtl`을 적용합니다.
 * - `timestamp`가 존재하면 `usingTimestamp`를 적용합니다.
 *
 * ```kotlin
 * val options = writeOptions { ttl(30) }
 * // result == options.isPositiveTtl
 * ```
 */
fun Update.addWriteOptions(writeOptions: WriteOptions): Update {
    var applied = this

    if (applied is UpdateStart) {
        if (writeOptions.isPositiveTtl) {
            applied = applied.usingTtl(writeOptions.ttl!!.seconds.toInt()) as Update
        }
        if (writeOptions.timestamp != null) {
            applied = (applied as UpdateStart).usingTimestamp(writeOptions.timestamp!!) as Update
        }
    }
    return applied
}

/**
 * [UpdateStart] 문에 [WriteOptions]의 TTL/타임스탬프를 순서대로 반영합니다.
 *
 * ## 동작/계약
 * - [WriteOptions.isPositiveTtl]가 `true`이면 `usingTtl`을 먼저 적용합니다.
 * - `timestamp`가 존재하면 `usingTimestamp`를 추가 적용합니다.
 * - 적용된 [UpdateStart] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val options = writeOptions { ttl(30) }
 * // result == options.isPositiveTtl
 * ```
 */
fun UpdateStart.addWriteOptions(writeOptions: WriteOptions): UpdateStart {
    var applied: UpdateStart = this

    if (writeOptions.isPositiveTtl) {
        applied = applied.usingTtl(writeOptions.ttl!!.seconds.toInt())
    }
    if (writeOptions.timestamp != null) {
        applied = applied.usingTimestamp(writeOptions.timestamp!!)
    }
    return applied
}

/**
 * [Delete] 문이 [DeleteSelection]인 경우 `timestamp`를 반영합니다.
 *
 * ## 동작/계약
 * - 수신 객체가 [DeleteSelection]이고 `timestamp`가 존재할 때만 `usingTimestamp`를 적용합니다.
 * - 조건을 만족하지 않으면 원본 Delete를 그대로 반환합니다.
 *
 * ```kotlin
 * val options = writeOptions { timestamp(1234) }
 * // result == (options.timestamp == 1234L)
 * ```
 */
fun Delete.addWriteOptions(writeOptions: WriteOptions): Delete {
    var applied = this

    if (applied is DeleteSelection && writeOptions.timestamp != null) {
        applied = applied.usingTimestamp(writeOptions.timestamp!!) as Delete
    }
    return applied
}

/**
 * TTL이 설정되어 있고 음수가 아니면 `true`를 반환합니다.
 *
 * ## 동작/계약
 * - `ttl == null`이면 `false`를 반환합니다.
 * - `ttl`이 존재하고 `isNegative == false`이면 `true`를 반환합니다.
 *
 * ```kotlin
 * val options = writeOptions { ttl(30) }
 * // result == options.isPositiveTtl
 * ```
 */
val WriteOptions.isPositiveTtl: Boolean get() = ttl != null && !ttl!!.isNegative
