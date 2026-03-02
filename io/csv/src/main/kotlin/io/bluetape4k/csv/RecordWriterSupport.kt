package io.bluetape4k.csv

import com.univocity.parsers.csv.CsvWriterSettings
import com.univocity.parsers.tsv.TsvWriterSettings
import java.io.File
import java.io.FileWriter
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8

/**
 * 파일에 CSV 행 데이터를 기록합니다.
 *
 * ## 동작/계약
 * - [headers]가 있으면 먼저 1회 기록한 뒤 [rows]를 순차 기록합니다.
 * - 파일 쓰기는 즉시 실행되며 함수 반환 시 writer가 닫힙니다.
 * - [rows]는 순서대로 소비됩니다.
 *
 * ```kotlin
 * file.writeCsvRecords(headers = listOf("name", "age"), rows = listOf(listOf("Alice", 20)))
 * // 파일 첫 두 행: name,age / Alice,20
 * ```
 */
fun File.writeCsvRecords(
    headers: List<String>? = null,
    rows: Iterable<Iterable<*>>,
    cs: Charset = UTF_8,
    settings: CsvWriterSettings = DefaultCsvWriterSettings,
) {
    FileWriter(this, cs).use { fw ->
        CsvRecordWriter(fw, settings).use { writer ->
            headers?.let { writer.writeHeaders(it) }
            writer.writeAll(rows)
        }
    }
}

/**
 * 파일에 엔티티 컬렉션을 CSV 행으로 변환해 기록합니다.
 *
 * ## 동작/계약
 * - [transform] 결과를 행으로 사용합니다.
 * - [headers]가 있으면 데이터 행보다 먼저 기록합니다.
 * - 변환/쓰기 예외는 호출자에게 전파됩니다.
 *
 * ```kotlin
 * file.writeCsvRecords(headers = listOf("name"), entities = listOf("Alice")) { listOf(it) }
 * // 파일 첫 두 행: name / Alice
 * ```
 */
fun <T> File.writeCsvRecords(
    headers: List<String>? = null,
    entities: Iterable<T>,
    cs: Charset = UTF_8,
    settings: CsvWriterSettings = DefaultCsvWriterSettings,
    transform: (T) -> Iterable<*>,
) {
    FileWriter(this, cs).use { fw ->
        CsvRecordWriter(fw, settings).use { writer ->
            headers?.let { writer.writeHeaders(it) }
            writer.writeAll(entities, transform)
        }
    }
}

/**
 * 파일에 TSV 행 데이터를 기록합니다.
 *
 * ## 동작/계약
 * - [headers]가 있으면 먼저 기록하고, 이후 [rows]를 순차 기록합니다.
 * - 함수 반환 시 writer가 닫힙니다.
 * - [rows] 순서가 파일 기록 순서와 동일합니다.
 *
 * ```kotlin
 * file.writeTsvRecords(headers = listOf("name", "age"), rows = listOf(listOf("Alice", 20)))
 * // 파일 첫 두 행: name\tage / Alice\t20
 * ```
 */
fun File.writeTsvRecords(
    headers: List<String>? = null,
    rows: Iterable<Iterable<*>>,
    cs: Charset = UTF_8,
    settings: TsvWriterSettings = DefaultTsvWriterSettings,
) {
    FileWriter(this, cs).use { fw ->
        TsvRecordWriter(fw, settings).use { writer ->
            headers?.let { writer.writeHeaders(it) }
            writer.writeAll(rows)
        }
    }
}

/**
 * 파일에 엔티티 컬렉션을 TSV 행으로 변환해 기록합니다.
 *
 * ## 동작/계약
 * - [transform]으로 변환한 결과를 행으로 기록합니다.
 * - [headers]가 지정되면 데이터 행보다 먼저 기록합니다.
 * - 변환/쓰기 예외는 전파됩니다.
 *
 * ```kotlin
 * file.writeTsvRecords(headers = listOf("name"), entities = listOf("Alice")) { listOf(it) }
 * // 파일 첫 두 행: name / Alice
 * ```
 */
fun <T> File.writeTsvRecords(
    headers: List<String>? = null,
    entities: Iterable<T>,
    cs: Charset = UTF_8,
    settings: TsvWriterSettings = DefaultTsvWriterSettings,
    transform: (T) -> Iterable<*>,
) {
    FileWriter(this, cs).use { fw ->
        TsvRecordWriter(fw, settings).use { writer ->
            headers?.let { writer.writeHeaders(it) }
            writer.writeAll(entities, transform)
        }
    }
}
