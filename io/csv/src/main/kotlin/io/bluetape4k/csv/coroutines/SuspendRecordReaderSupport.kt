package io.bluetape4k.csv.coroutines

import io.bluetape4k.csv.Record
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8

/**
 * 입력 스트림을 CSV로 읽어 [Flow]를 반환합니다.
 *
 * ```kotlin
 * val rows = input.readAsCsvRecordsSuspending(skipHeader = true).toList()
 * ```
 */
fun InputStream.readAsCsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Flow<Record> =
    SuspendCsvRecordReader().read(this, cs, skipHeader) { it }

/**
 * 입력 스트림을 CSV로 읽어 변환 결과 [Flow]를 반환합니다.
 *
 * ```kotlin
 * val names = input.readAsCsvRecordsSuspending(skipHeader = true) { it.getString("name") }.toList()
 * ```
 */
fun <T> InputStream.readAsCsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> =
    SuspendCsvRecordReader().read(this, cs, skipHeader, transform)

/**
 * 입력 스트림을 TSV로 읽어 [Flow]를 반환합니다.
 *
 * ```kotlin
 * val rows = input.readAsTsvRecordsSuspending(skipHeader = true).toList()
 * ```
 */
fun InputStream.readAsTsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Flow<Record> =
    SuspendTsvRecordReader().read(this, cs, skipHeader) { it }

/**
 * 입력 스트림을 TSV로 읽어 변환 결과 [Flow]를 반환합니다.
 *
 * ```kotlin
 * val names = input.readAsTsvRecordsSuspending(skipHeader = true) { it.getString("name") }.toList()
 * ```
 */
fun <T> InputStream.readAsTsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> =
    SuspendTsvRecordReader().read(this, cs, skipHeader, transform)

/**
 * 파일을 CSV로 읽어 [Flow]를 반환합니다.
 *
 * ```kotlin
 * val rows = file.readAsCsvRecordsSuspending(skipHeader = true).toList()
 * ```
 */
fun File.readAsCsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Flow<Record> =
    FileInputStream(this).buffered().readAsCsvRecordsSuspending(cs, skipHeader)

/**
 * 파일을 CSV로 읽어 변환 결과 [Flow]를 반환합니다.
 *
 * ```kotlin
 * val ids = file.readAsCsvRecordsSuspending(skipHeader = true) { it.getLong("id") }.toList()
 * ```
 */
fun <T> File.readAsCsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> =
    FileInputStream(this).buffered().readAsCsvRecordsSuspending(cs, skipHeader, transform)

/**
 * 파일을 TSV로 읽어 [Flow]를 반환합니다.
 *
 * ```kotlin
 * val rows = file.readAsTsvRecordsSuspending(skipHeader = true).toList()
 * ```
 */
fun File.readAsTsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Flow<Record> =
    FileInputStream(this).buffered().readAsTsvRecordsSuspending(cs, skipHeader)

/**
 * 파일을 TSV로 읽어 변환 결과 [Flow]를 반환합니다.
 *
 * ```kotlin
 * val ids = file.readAsTsvRecordsSuspending(skipHeader = true) { it.getLong("id") }.toList()
 * ```
 */
fun <T> File.readAsTsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> =
    FileInputStream(this).buffered().readAsTsvRecordsSuspending(cs, skipHeader, transform)
