package io.bluetape4k.csv

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8

/**
 * 파일을 CSV로 읽어 [Record] 시퀀스를 반환합니다.
 *
 * [skipHeader]가 `true`면 첫 행(헤더)을 결과에서 제외합니다.
 *
 * ```kotlin
 * val names = file.readAsCsvRecords(skipHeader = true).map { it.getString("name") }.toList()
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
 * ```kotlin
 * val ids = file.readAsCsvRecords(skipHeader = true) { it.getLong("id") }.toList()
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
 * ```kotlin
 * val names = file.readAsTsvRecords(skipHeader = true).map { it.getString("name") }.toList()
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
 * ```kotlin
 * val ids = file.readAsTsvRecords(skipHeader = true) { it.getLong("id") }.toList()
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
 * ```kotlin
 * val rows = input.readAsCsvRecords(skipHeader = true).toList()
 * ```
 */
fun InputStream.readAsCsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Sequence<Record> =
    CsvRecordReader().read(this, cs, skipHeader) { it }

/**
 * 입력 스트림을 CSV로 읽어 변환 결과 시퀀스를 반환합니다.
 *
 * ```kotlin
 * val names = input.readAsCsvRecords(skipHeader = true) { it.getString("name") }.toList()
 * ```
 */
fun <T> InputStream.readAsCsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: (Record) -> T,
): Sequence<T> =
    CsvRecordReader().read(this, cs, skipHeader, transform)

/**
 * 입력 스트림을 TSV로 읽어 [Record] 시퀀스를 반환합니다.
 *
 * ```kotlin
 * val rows = input.readAsTsvRecords(skipHeader = true).toList()
 * ```
 */
fun InputStream.readAsTsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Sequence<Record> =
    TsvRecordReader().read(this, cs, skipHeader) { it }

/**
 * 입력 스트림을 TSV로 읽어 변환 결과 시퀀스를 반환합니다.
 *
 * ```kotlin
 * val names = input.readAsTsvRecords(skipHeader = true) { it.getString("name") }.toList()
 * ```
 */
fun <T> InputStream.readAsTsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: (Record) -> T,
): Sequence<T> =
    TsvRecordReader().read(this, cs, skipHeader, transform)
