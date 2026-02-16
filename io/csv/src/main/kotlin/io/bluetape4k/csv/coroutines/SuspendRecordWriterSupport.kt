package io.bluetape4k.csv.coroutines

import com.univocity.parsers.csv.CsvWriterSettings
import com.univocity.parsers.tsv.TsvWriterSettings
import io.bluetape4k.csv.DefaultCsvWriterSettings
import io.bluetape4k.csv.DefaultTsvWriterSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8

/**
 * CSV 파일에 데이터 행을 기록합니다.
 *
 * ```
 * val file = File("output.csv")
 * file.writeCsvRecords(
 *     headers = listOf("name", "age"),
 *     rows = listOf(listOf("Alice", 20), listOf("Bob", 30))
 * )
 * ```
 *
 * @receiver 출력 CSV 파일
 * @param headers 헤더 이름들 (null이면 헤더를 기록하지 않음)
 * @param rows 기록할 데이터 행들
 * @param cs 파일 인코딩 (기본: UTF-8)
 * @param settings CSV writer 설정
 */
suspend fun File.writeCsvRecordsSuspending(
    headers: List<String>? = null,
    rows: Iterable<Iterable<*>>,
    cs: Charset = UTF_8,
    settings: CsvWriterSettings = DefaultCsvWriterSettings,
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
 * CSV 파일에 엔티티를 변환하여 기록합니다.
 *
 * ```
 * val file = File("output.csv")
 * val items = listOf(Item("Alice", 20), Item("Bob", 30))
 * file.writeCsvRecords(
 *     headers = listOf("name", "age"),
 *     entities = items,
 * ) { item -> listOf(item.name, item.age) }
 * ```
 *
 * @receiver 출력 CSV 파일
 * @param headers 헤더 이름들 (null이면 헤더를 기록하지 않음)
 * @param entities 기록할 엔티티들
 * @param cs 파일 인코딩 (기본: UTF-8)
 * @param settings CSV writer 설정
 * @param transform 엔티티를 데이터 행으로 변환하는 함수
 */
suspend fun <T> File.writeCsvRecordsSuspending(
    headers: List<String>? = null,
    entities: Iterable<T>,
    cs: Charset = UTF_8,
    settings: CsvWriterSettings = DefaultCsvWriterSettings,
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
 * TSV 파일에 데이터 행을 기록합니다.
 *
 * ```
 * val file = File("output.tsv")
 * file.writeTsvRecords(
 *     headers = listOf("name", "age"),
 *     rows = listOf(listOf("Alice", 20), listOf("Bob", 30))
 * )
 * ```
 *
 * @receiver 출력 TSV 파일
 * @param headers 헤더 이름들 (null이면 헤더를 기록하지 않음)
 * @param rows 기록할 데이터 행들
 * @param cs 파일 인코딩 (기본: UTF-8)
 * @param settings TSV writer 설정
 */
suspend fun File.writeTsvRecordsSuspending(
    headers: List<String>? = null,
    rows: Iterable<Iterable<*>>,
    cs: Charset = UTF_8,
    settings: TsvWriterSettings = DefaultTsvWriterSettings,
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
 * TSV 파일에 엔티티를 변환하여 기록합니다.
 *
 * ```
 * val file = File("output.tsv")
 * val items = listOf(Item("Alice", 20), Item("Bob", 30))
 * file.writeTsvRecords(
 *     headers = listOf("name", "age"),
 *     entities = items,
 * ) { item -> listOf(item.name, item.age) }
 * ```
 *
 * @receiver 출력 TSV 파일
 * @param headers 헤더 이름들 (null이면 헤더를 기록하지 않음)
 * @param entities 기록할 엔티티들
 * @param cs 파일 인코딩 (기본: UTF-8)
 * @param settings TSV writer 설정
 * @param transform 엔티티를 데이터 행으로 변환하는 함수
 */
suspend fun <T> File.writeTsvRecordsSuspending(
    headers: List<String>? = null,
    entities: Iterable<T>,
    cs: Charset = UTF_8,
    settings: TsvWriterSettings = DefaultTsvWriterSettings,
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
