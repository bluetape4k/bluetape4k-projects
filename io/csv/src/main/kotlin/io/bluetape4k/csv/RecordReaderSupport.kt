package io.bluetape4k.csv

import com.univocity.parsers.common.record.Record
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8

/**
 * 파일을 CSV로 읽어 [Record] 시퀀스를 반환합니다.
 *
 * ## 동작/계약
 * - `sequence {}` 내부에서 파일 스트림을 열고, 시퀀스 소비가 끝나면 자동으로 닫습니다.
 * - [skipHeader]가 `true`면 첫 행(헤더)을 결과에서 제외합니다.
 * - 반환값은 lazy [Sequence]입니다.
 *
 * ```kotlin
 * val names = file.readAsCsvRecords(skipHeader = true).map { it.getString("name") }.toList()
 * // names == listOf("Alice", "Bob")
 * ```
 */
fun File.readAsCsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Sequence<Record> = sequence {
    FileInputStream(this@readAsCsvRecords).buffered().use { inputStream ->
        yieldAll(inputStream.readAsCsvRecords(cs, skipHeader))
    }
}

/**
 * 파일을 CSV로 읽어 변환 결과 시퀀스를 반환합니다.
 *
 * ## 동작/계약
 * - 파일 스트림을 열어 [InputStream.readAsCsvRecords] 변환 오버로드에 위임합니다.
 * - [transform] 예외는 호출자에게 전파됩니다.
 * - 시퀀스 소비가 종료되면 파일 스트림이 닫힙니다.
 *
 * ```kotlin
 * val ids = file.readAsCsvRecords(skipHeader = true) { it.getLong("id") }.toList()
 * // ids == listOf(1L, 2L)
 * ```
 */
fun <T> File.readAsCsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: (Record) -> T,
): Sequence<T> = sequence {
    FileInputStream(this@readAsCsvRecords).buffered().use { inputStream ->
        yieldAll(inputStream.readAsCsvRecords(cs, skipHeader, transform))
    }
}

/**
 * 파일을 TSV로 읽어 [Record] 시퀀스를 반환합니다.
 *
 * ## 동작/계약
 * - `sequence {}` 내부에서 파일 스트림을 열고 소비 완료 시 닫습니다.
 * - [skipHeader]가 `true`면 첫 행을 제외합니다.
 * - 반환값은 lazy [Sequence]입니다.
 *
 * ```kotlin
 * val names = file.readAsTsvRecords(skipHeader = true).map { it.getString("name") }.toList()
 * // names == listOf("Alice", "Bob")
 * ```
 */
fun File.readAsTsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Sequence<Record> = sequence {
    FileInputStream(this@readAsTsvRecords).buffered().use { inputStream ->
        yieldAll(inputStream.readAsTsvRecords(cs, skipHeader))
    }
}

/**
 * 파일을 TSV로 읽어 변환 결과 시퀀스를 반환합니다.
 *
 * ## 동작/계약
 * - 파일 스트림을 열어 [InputStream.readAsTsvRecords] 변환 오버로드에 위임합니다.
 * - [transform] 예외는 호출자에게 전파됩니다.
 * - 시퀀스 소비가 종료되면 파일 스트림이 닫힙니다.
 *
 * ```kotlin
 * val ids = file.readAsTsvRecords(skipHeader = true) { it.getLong("id") }.toList()
 * // ids == listOf(1L, 2L)
 * ```
 */
fun <T> File.readAsTsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: (Record) -> T,
): Sequence<T> = sequence {
    FileInputStream(this@readAsTsvRecords).buffered().use { inputStream ->
        yieldAll(inputStream.readAsTsvRecords(cs, skipHeader, transform))
    }
}

/**
 * 입력 스트림을 CSV로 읽어 [Record] 시퀀스를 반환합니다.
 *
 * ## 동작/계약
 * - 새 [CsvRecordReader]를 생성해 파싱합니다.
 * - 스트림 닫기는 호출자가 관리해야 합니다.
 *
 * ```kotlin
 * val rows = input.readAsCsvRecords(skipHeader = true).toList()
 * // rows.size == 2
 * ```
 */
fun InputStream.readAsCsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Sequence<Record> =
    CsvRecordReader().read(this@readAsCsvRecords, cs, skipHeader) { it }

/**
 * 입력 스트림을 CSV로 읽어 변환 결과 시퀀스를 반환합니다.
 *
 * ## 동작/계약
 * - 새 [CsvRecordReader]를 생성해 [transform]을 적용합니다.
 * - 파싱/변환 예외는 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val names = input.readAsCsvRecords(skipHeader = true) { it.getString("name") }.toList()
 * // names == listOf("Alice", "Bob")
 * ```
 */
fun <T> InputStream.readAsCsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: (Record) -> T,
): Sequence<T> =
    CsvRecordReader().read(this@readAsCsvRecords, cs, skipHeader, transform)

/**
 * 입력 스트림을 TSV로 읽어 [Record] 시퀀스를 반환합니다.
 *
 * ## 동작/계약
 * - 새 [TsvRecordReader]를 생성해 파싱합니다.
 * - 스트림 닫기는 호출자가 관리해야 합니다.
 *
 * ```kotlin
 * val rows = input.readAsTsvRecords(skipHeader = true).toList()
 * // rows.size == 2
 * ```
 */
fun InputStream.readAsTsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Sequence<Record> =
    TsvRecordReader().read(this@readAsTsvRecords, cs, skipHeader) { it }

/**
 * 입력 스트림을 TSV로 읽어 변환 결과 시퀀스를 반환합니다.
 *
 * ## 동작/계약
 * - 새 [TsvRecordReader]를 생성해 [transform]을 적용합니다.
 * - 파싱/변환 예외는 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val names = input.readAsTsvRecords(skipHeader = true) { it.getString("name") }.toList()
 * // names == listOf("Alice", "Bob")
 * ```
 */
fun <T> InputStream.readAsTsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: (Record) -> T,
): Sequence<T> =
    TsvRecordReader().read(this@readAsTsvRecords, cs, skipHeader, transform)
