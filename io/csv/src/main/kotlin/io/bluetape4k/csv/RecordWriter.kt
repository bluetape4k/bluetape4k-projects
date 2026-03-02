package io.bluetape4k.csv

import java.io.Closeable

/**
 * CSV/TSV 행 데이터를 기록하는 동기 Writer 계약입니다.
 *
 * ## 동작/계약
 * - 헤더/데이터 행은 호출 순서대로 출력됩니다.
 * - `Sequence`/`Iterable` 입력은 순차 소비되며, 기본 구현은 행 단위로 위임 호출합니다.
 * - 변환 람다 예외는 호출자에게 전파됩니다.
 *
 * ```kotlin
 * CsvRecordWriter(output).use { writer ->
 *     writer.writeHeaders("name", "age")
 *     writer.writeAll(listOf(listOf("Alice", 20), listOf("Bob", 30)))
 * }
 * // output 첫 두 행: name,age / Alice,20
 * ```
 */
interface RecordWriter: Closeable {

    /**
     * 헤더 행을 기록합니다.
     *
     * ## 동작/계약
     * - [headers] 순서가 출력 컬럼 순서가 됩니다.
     * - 구현체는 현재 포맷(CSV/TSV) 구분자로 헤더를 1행 기록합니다.
     *
     * ```kotlin
     * writer.writeHeaders(listOf("id", "name"))
     * // 첫 행 == "id,name" (CSV 기준)
     * ```
     */
    fun writeHeaders(headers: Iterable<String>)

    /**
     * 가변 인자를 받아 헤더 행을 기록합니다.
     *
     * ## 동작/계약
     * - `headers.toList()`로 변환해 [writeHeaders]에 위임합니다.
     *
     * ```kotlin
     * writer.writeHeaders("id", "name")
     * // 첫 행 == "id,name" (CSV 기준)
     * ```
     */
    fun writeHeaders(vararg headers: String) {
        writeHeaders(headers.toList())
    }

    /**
     * 데이터 행 1건을 기록합니다.
     *
     * ## 동작/계약
     * - [rows]의 순서가 컬럼 순서로 기록됩니다.
     * - 입력 행은 변형하지 않고 필요 시 구현체가 새 리스트로 복사할 수 있습니다.
     *
     * ```kotlin
     * writer.writeRow(listOf("Alice", 20))
     * // 다음 행 == "Alice,20" (CSV 기준)
     * ```
     */
    fun writeRow(rows: Iterable<*>)

    /**
     * 엔티티 1건을 행으로 변환해 기록합니다.
     *
     * ## 동작/계약
     * - [transform] 결과를 [writeRow]에 그대로 전달합니다.
     * - [transform] 예외는 감싸지 않고 전파됩니다.
     *
     * ```kotlin
     * writer.writeRow(User("Alice", 20)) { listOf(it.name, it.age) }
     * // 다음 행 == "Alice,20" (CSV 기준)
     * ```
     */
    fun <T> writeRow(entity: T, transform: (T) -> Iterable<*>) {
        writeRow(transform(entity))
    }

    /**
     * 여러 행을 순차적으로 기록합니다.
     *
     * ## 동작/계약
     * - [Sequence]를 앞에서부터 순서대로 소비합니다.
     * - 각 행은 [writeRow]에 위임됩니다.
     *
     * ```kotlin
     * writer.writeAll(sequenceOf(listOf("A", 1), listOf("B", 2)))
     * // 데이터 행 2건이 순서대로 기록됨
     * ```
     */
    fun writeAll(rows: Sequence<Iterable<*>>)

    /**
     * 여러 엔티티를 변환해 순차 기록합니다.
     *
     * ## 동작/계약
     * - `entities.map(transform)`을 생성해 [writeAll]에 위임합니다.
     * - 변환/기록 중 예외는 전파됩니다.
     *
     * ```kotlin
     * writer.writeAll(sequenceOf(User("A", 1))) { listOf(it.name, it.age) }
     * // 데이터 행 1건 기록됨
     * ```
     */
    fun <T> writeAll(entities: Sequence<T>, transform: (T) -> Iterable<*>) {
        writeAll(entities.map(transform))
    }

    /**
     * [Iterable] 행 컬렉션을 순차 기록합니다.
     *
     * ## 동작/계약
     * - `rows.asSequence()`로 변환해 [writeAll]에 위임합니다.
     *
     * ```kotlin
     * writer.writeAll(listOf(listOf("A", 1), listOf("B", 2)))
     * // 데이터 행 2건 기록됨
     * ```
     */
    fun writeAll(rows: Iterable<Iterable<*>>) {
        writeAll(rows.asSequence())
    }

    /**
     * [Iterable] 엔티티 컬렉션을 변환해 순차 기록합니다.
     *
     * ## 동작/계약
     * - `entities.asSequence().map(transform)`을 생성해 [writeAll]에 위임합니다.
     *
     * ```kotlin
     * writer.writeAll(listOf(User("A", 1))) { listOf(it.name, it.age) }
     * // 데이터 행 1건 기록됨
     * ```
     */
    fun <T> writeAll(entities: Iterable<T>, transform: (T) -> Iterable<*>) {
        writeAll(entities.asSequence().map(transform))
    }
}
