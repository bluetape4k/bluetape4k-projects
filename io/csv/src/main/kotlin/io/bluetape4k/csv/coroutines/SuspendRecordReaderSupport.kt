package io.bluetape4k.csv.coroutines

import com.univocity.parsers.common.record.Record
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8

/**
 * 입력 스트림을 CSV로 읽어 [Flow]를 반환합니다.
 *
 * ## 동작/계약
 * - 새 [SuspendCsvRecordReader]를 생성해 파싱합니다.
 * - [skipHeader]가 `true`면 첫 행을 제외합니다.
 * - 스트림 닫기는 호출자가 관리합니다.
 *
 * ```kotlin
 * val rows = input.readAsCsvRecordsSuspending(skipHeader = true).toList()
 * // rows.size == 2
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
 * ## 동작/계약
 * - [transform]이 각 레코드마다 suspend로 실행됩니다.
 * - 파싱/변환 예외는 collect 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val names = input.readAsCsvRecordsSuspending(skipHeader = true) { it.getString("name") }.toList()
 * // names == listOf("Alice", "Bob")
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
 * ## 동작/계약
 * - 새 [SuspendTsvRecordReader]를 생성해 파싱합니다.
 * - [skipHeader]가 `true`면 첫 행을 제외합니다.
 * - 스트림 닫기는 호출자가 관리합니다.
 *
 * ```kotlin
 * val rows = input.readAsTsvRecordsSuspending(skipHeader = true).toList()
 * // rows.size == 2
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
 * ## 동작/계약
 * - [transform]이 각 레코드마다 suspend로 실행됩니다.
 * - 파싱/변환 예외는 collect 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val names = input.readAsTsvRecordsSuspending(skipHeader = true) { it.getString("name") }.toList()
 * // names == listOf("Alice", "Bob")
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
 * ## 동작/계약
 * - 파일 스트림을 열고 [InputStream.readAsCsvRecordsSuspending]에 위임합니다.
 * - 반환된 Flow 수집이 끝나면 스트림이 닫힙니다.
 *
 * ```kotlin
 * val rows = file.readAsCsvRecordsSuspending(skipHeader = true).toList()
 * // rows.size == 2
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
 * ## 동작/계약
 * - 파일 스트림을 열어 변환 오버로드에 위임합니다.
 * - [transform] 예외는 collect 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val ids = file.readAsCsvRecordsSuspending(skipHeader = true) { it.getLong("id") }.toList()
 * // ids == listOf(1L, 2L)
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
 * ## 동작/계약
 * - 파일 스트림을 열고 [InputStream.readAsTsvRecordsSuspending]에 위임합니다.
 * - 반환 Flow 수집 종료 후 스트림이 닫힙니다.
 *
 * ```kotlin
 * val rows = file.readAsTsvRecordsSuspending(skipHeader = true).toList()
 * // rows.size == 2
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
 * ## 동작/계약
 * - 파일 스트림을 열어 변환 오버로드에 위임합니다.
 * - [transform] 예외는 collect 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val ids = file.readAsTsvRecordsSuspending(skipHeader = true) { it.getLong("id") }.toList()
 * // ids == listOf(1L, 2L)
 * ```
 */
fun <T> File.readAsTsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> =
    FileInputStream(this).buffered().readAsTsvRecordsSuspending(cs, skipHeader, transform)
