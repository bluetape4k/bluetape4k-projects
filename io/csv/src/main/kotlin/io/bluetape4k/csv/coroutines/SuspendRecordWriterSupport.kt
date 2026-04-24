package io.bluetape4k.csv.coroutines

import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.csv.TsvSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8

/**
 * 파일에 CSV 행 데이터를 코루틴으로 기록합니다.
 *
 * ## 동작/계약
 * - 전체 쓰기 작업은 `Dispatchers.IO`에서 실행됩니다.
 * - [headers]가 있으면 먼저 기록 후 [rows]를 순차 기록합니다.
 * - 함수 반환 시 writer가 닫힙니다.
 *
 * ```kotlin
 * file.writeCsvRecordsSuspending(headers = listOf("name"), rows = listOf(listOf("Alice")))
 * // 파일 첫 두 행: name / Alice
 * ```
 */
suspend fun File.writeCsvRecordsSuspending(
    headers: List<String>? = null,
    rows: Iterable<Iterable<*>>,
    cs: Charset = UTF_8,
    settings: CsvSettings = CsvSettings.DEFAULT,
) {
    withContext(Dispatchers.IO) {
        FileWriter(this@writeCsvRecordsSuspending, cs).buffered().use { bufferedWriter ->
            SuspendCsvRecordWriter(bufferedWriter, settings).use { writer ->
                headers?.let { writer.writeHeaders(it) }
                writer.writeAll(rows)
            }
        }
    }
}

/**
 * 파일에 엔티티 컬렉션을 CSV 행으로 변환해 기록합니다.
 *
 * ## 동작/계약
 * - 전체 쓰기는 `Dispatchers.IO`에서 실행됩니다.
 * - [transform] 결과를 행으로 기록합니다.
 * - 변환/쓰기 예외는 호출자에게 전파됩니다.
 *
 * ```kotlin
 * file.writeCsvRecordsSuspending(headers = listOf("name"), entities = listOf("Alice")) { listOf(it) }
 * // 파일 첫 두 행: name / Alice
 * ```
 */
suspend fun <T> File.writeCsvRecordsSuspending(
    headers: List<String>? = null,
    entities: Iterable<T>,
    cs: Charset = UTF_8,
    settings: CsvSettings = CsvSettings.DEFAULT,
    transform: (T) -> Iterable<*>,
) {
    withContext(Dispatchers.IO) {
        FileWriter(this@writeCsvRecordsSuspending, cs).buffered().use { bufferedWriter ->
            SuspendCsvRecordWriter(bufferedWriter, settings).use { writer ->
                headers?.let { writer.writeHeaders(it) }
                writer.writeAll(entities, transform)
            }
        }
    }
}

/**
 * 파일에 TSV 행 데이터를 코루틴으로 기록합니다.
 *
 * ## 동작/계약
 * - 전체 쓰기 작업은 `Dispatchers.IO`에서 실행됩니다.
 * - [headers]가 있으면 먼저 기록 후 [rows]를 순차 기록합니다.
 * - 함수 반환 시 writer가 닫힙니다.
 *
 * ```kotlin
 * file.writeTsvRecordsSuspending(headers = listOf("name"), rows = listOf(listOf("Alice")))
 * // 파일 첫 두 행: name / Alice
 * ```
 */
suspend fun File.writeTsvRecordsSuspending(
    headers: List<String>? = null,
    rows: Iterable<Iterable<*>>,
    cs: Charset = UTF_8,
    settings: TsvSettings = TsvSettings.DEFAULT,
) {
    withContext(Dispatchers.IO) {
        FileWriter(this@writeTsvRecordsSuspending, cs).buffered().use { bufferedWriter ->
            SuspendTsvRecordWriter(bufferedWriter, settings).use { writer ->
                headers?.let { writer.writeHeaders(it) }
                writer.writeAll(rows)
            }
        }
    }
}

/**
 * 파일에 엔티티 컬렉션을 TSV 행으로 변환해 기록합니다.
 *
 * ## 동작/계약
 * - 전체 쓰기는 `Dispatchers.IO`에서 실행됩니다.
 * - [transform] 결과를 행으로 기록합니다.
 * - 변환/쓰기 예외는 전파됩니다.
 *
 * ```kotlin
 * file.writeTsvRecordsSuspending(headers = listOf("name"), entities = listOf("Alice")) { listOf(it) }
 * // 파일 첫 두 행: name / Alice
 * ```
 */
suspend fun <T> File.writeTsvRecordsSuspending(
    headers: List<String>? = null,
    entities: Iterable<T>,
    cs: Charset = UTF_8,
    settings: TsvSettings = TsvSettings.DEFAULT,
    transform: (T) -> Iterable<*>,
) {
    withContext(Dispatchers.IO) {
        FileWriter(this@writeTsvRecordsSuspending, cs).buffered().use { bufferedWriter ->
            SuspendTsvRecordWriter(bufferedWriter, settings).use { writer ->
                headers?.let { writer.writeHeaders(it) }
                writer.writeAll(entities, transform)
            }
        }
    }
}
