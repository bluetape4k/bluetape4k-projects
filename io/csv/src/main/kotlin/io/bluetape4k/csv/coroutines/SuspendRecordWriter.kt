package io.bluetape4k.csv.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import java.io.Closeable

/**
 * CSV/TSV 행 데이터를 코루틴으로 기록하는 Writer 계약입니다.
 *
 * ## 동작/계약
 * - 헤더/행은 호출 순서대로 기록됩니다.
 * - `Sequence`/`Iterable`/`Flow` 입력은 순차 소비합니다.
 * - 변환 람다 예외는 호출자에게 전파됩니다.
 *
 * ```kotlin
 * SuspendCsvRecordWriter(output).use { writer ->
 *     writer.writeHeaders("name", "age")
 *     writer.writeAll(listOf(listOf("Alice", 20), listOf("Bob", 30)))
 * }
 * // output 첫 두 행: name,age / Alice,20
 * ```
 */
interface SuspendRecordWriter: Closeable {

    /**
     * 헤더 행을 기록합니다.
     *
     * ## 동작/계약
     * - [headers] 순서가 출력 컬럼 순서가 됩니다.
     *
     * ```kotlin
     * writer.writeHeaders(listOf("id", "name"))
     * // 첫 행 기록 완료
     * ```
     */
    suspend fun writeHeaders(headers: Iterable<String>)

    /**
     * 가변 인자 헤더를 기록합니다.
     *
     * ## 동작/계약
     * - `headers.toList()`로 변환해 [writeHeaders]에 위임합니다.
     *
     * ```kotlin
     * writer.writeHeaders("id", "name")
     * // 첫 행 기록 완료
     * ```
     */
    suspend fun writeHeaders(vararg headers: String) {
        writeHeaders(headers.toList())
    }

    /**
     * 데이터 행 1건을 기록합니다.
     *
     * ## 동작/계약
     * - [row]는 호출 순서대로 출력됩니다.
     *
     * ```kotlin
     * writer.writeRow(listOf("Alice", 20))
     * // 데이터 행 1건 기록됨
     * ```
     */
    suspend fun writeRow(row: Iterable<*>)

    /**
     * 엔티티 1건을 행으로 변환해 기록합니다.
     *
     * ## 동작/계약
     * - [mapper] 결과를 [writeRow]에 위임합니다.
     *
     * ```kotlin
     * writer.writeRow(User("Alice", 20)) { listOf(it.name, it.age) }
     * // 데이터 행 1건 기록됨
     * ```
     */
    suspend fun <T> writeRow(entity: T, mapper: suspend (T) -> Iterable<*>) {
        writeRow(mapper(entity))
    }

    /**
     * 여러 행을 순차 기록합니다.
     *
     * ## 동작/계약
     * - [rows]를 순서대로 소비하고 각 항목을 기록합니다.
     *
     * ```kotlin
     * writer.writeAll(sequenceOf(listOf("A", 1), listOf("B", 2)))
     * // 데이터 행 2건 기록됨
     * ```
     */
    suspend fun writeAll(rows: Sequence<Iterable<*>>)

    /**
     * 여러 엔티티를 변환해 순차 기록합니다.
     *
     * ## 동작/계약
     * - `entities.asFlow().map(transform)`을 [writeAll]에 위임합니다.
     *
     * ```kotlin
     * writer.writeAll(sequenceOf(User("A", 1))) { listOf(it.name, it.age) }
     * // 데이터 행 1건 기록됨
     * ```
     */
    suspend fun <T> writeAll(entities: Sequence<T>, transform: suspend (T) -> Iterable<*>) {
        writeAll(entities.asFlow().map(transform))
    }

    /**
     * [Iterable] 행 컬렉션을 순차 기록합니다.
     *
     * ## 동작/계약
     * - `rows.asFlow()`로 변환해 [writeAll]에 위임합니다.
     *
     * ```kotlin
     * writer.writeAll(listOf(listOf("A", 1), listOf("B", 2)))
     * // 데이터 행 2건 기록됨
     * ```
     */
    suspend fun writeAll(rows: Iterable<Iterable<*>>) {
        writeAll(rows.asFlow())
    }

    /**
     * [Iterable] 엔티티 컬렉션을 변환해 순차 기록합니다.
     *
     * ## 동작/계약
     * - `entities.asFlow().map(transform)`을 [writeAll]에 위임합니다.
     *
     * ```kotlin
     * writer.writeAll(listOf(User("A", 1))) { listOf(it.name, it.age) }
     * // 데이터 행 1건 기록됨
     * ```
     */
    suspend fun <T> writeAll(entities: Iterable<T>, transform: suspend (T) -> Iterable<*>) {
        writeAll(entities.asFlow().map(transform))
    }

    /**
     * [Flow]로 전달되는 행을 수집해 기록합니다.
     *
     * ## 동작/계약
     * - collect 순서대로 기록됩니다.
     *
     * ```kotlin
     * writer.writeAll(kotlinx.coroutines.flow.flowOf(listOf("A", 1)))
     * // 데이터 행 1건 기록됨
     * ```
     */
    suspend fun writeAll(rows: Flow<Iterable<*>>)

    /**
     * [Flow] 엔티티를 변환해 수집 기록합니다.
     *
     * ## 동작/계약
     * - `entities.map(transform)`을 [writeAll]에 위임합니다.
     *
     * ```kotlin
     * writer.writeAll(kotlinx.coroutines.flow.flowOf(User("A", 1))) { listOf(it.name, it.age) }
     * // 데이터 행 1건 기록됨
     * ```
     */
    suspend fun <T> writeAll(entities: Flow<T>, transform: suspend (T) -> Iterable<*>) {
        writeAll(entities.map(transform))
    }
}
