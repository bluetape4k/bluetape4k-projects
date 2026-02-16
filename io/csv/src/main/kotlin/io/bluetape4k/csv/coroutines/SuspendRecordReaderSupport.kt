package io.bluetape4k.csv.coroutines

import com.univocity.parsers.common.record.Record
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8


/**
 * CSV 파일을 읽어들여서 `Flow<Record>` 로 변환합니다.
 *
 * ```
 * val inputStream = FileInputStream("data.csv")
 * val records = inputStream.readAsCsvRecords(Charsets.UTF_8, skipHeader = true)
 * ```
 *
 * @receiver CSV 파일의 입력 스트림
 * @param cs CSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader CSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @return CSV 파일의 레코드들을 전송하는 Flow
 */
fun InputStream.readAsCsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Flow<Record> =
    SuspendCsvRecordReader()
        .read(this, cs, skipHeader) { it }

/**
 * CSV 입력 스트림에서 레코드를 읽어 [transform] 함수를 통해 원하는 타입으로 변환합니다.
 *
 * ```
 * val inputStream = FileInputStream("data.csv")
 * val items = inputStream.readAsCsvRecords(Charsets.UTF_8, skipHeader = true) { record ->
 *     Item(record.getString("name"), record.getInt("age"))
 * }
 * ```
 *
 * @receiver CSV 파일의 입력 스트림
 * @param cs CSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader CSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @param transform Record를 원하는 타입으로 변환하는 함수
 * @return 변환된 데이터의 시퀀스
 */
fun <T> InputStream.readAsCsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> =
    SuspendCsvRecordReader()
        .read(this, cs, skipHeader, transform)


/**
 * TSV 파일을 읽어들여서 `Flow<Record>` 로 변환합니다.
 *
 * ```
 * val inputStream = FileInputStream("data.tsv")
 * val records = inputStream.readAsTsvRecordsSuspending(Charsets.UTF_8, skipHeader = true)
 * ```
 *
 * @receiver TSV 파일의 입력 스트림
 * @param cs TSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader TSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @return TSV 파일의 레코드들
 */
fun InputStream.readAsTsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Flow<Record> =
    SuspendTsvRecordReader()
        .read(this, cs, skipHeader) { it }

/**
 * TSV 입력 스트림에서 레코드를 읽어 [transform] 함수를 통해 원하는 타입으로 변환합니다.
 *
 * ```
 * val inputStream = FileInputStream("data.tsv")
 * val items = inputStream.readAsTsvRecords(Charsets.UTF_8, skipHeader = true) { record ->
 *     Item(record.getString("name"), record.getInt("age"))
 * }
 * ```
 *
 * @receiver TSV 파일의 입력 스트림
 * @param cs TSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader TSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @param transform Record를 원하는 타입으로 변환하는 함수
 * @return 변환된 데이터의 시퀀스
 */
fun <T> InputStream.readAsTsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> =
    SuspendTsvRecordReader()
        .read(this, cs, skipHeader, transform)

/**
 * CSV 파일을 읽어들여서 `Flow<Record>` 로 변환합니다.
 *
 * ```
 * val file = File("data.csv")
 * val records = file.readAsCsvRecordsSuspending(Charsets.UTF_8, skipHeader = true)
 * records.forEach { println(it) }
 * ```
 *
 * @receiver CSV 파일
 * @param cs CSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader CSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @return CSV 파일의 레코드들
 */
fun File.readAsCsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Flow<Record> =
    FileInputStream(this).buffered().readAsCsvRecordsSuspending(cs, skipHeader)

/**
 * CSV 파일을 읽어 [transform] 함수를 통해 원하는 타입으로 변환합니다.
 *
 * ```
 * val file = File("data.csv")
 * val items = file.readAsCsvRecordsSuspending(Charsets.UTF_8, skipHeader = true) { record ->
 *     Item(record.getString("name"), record.getInt("age"))
 * }
 * ```
 *
 * @receiver CSV 파일
 * @param cs CSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader CSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @param transform Record를 원하는 타입으로 변환하는 함수
 * @return 변환된 데이터의 시퀀스
 */
fun <T> File.readAsCsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> =
    FileInputStream(this).buffered().readAsCsvRecordsSuspending(cs, skipHeader, transform)

/**
 * TSV 파일을 읽어들여서 `Flow<Record>` 로 변환합니다.
 *
 * `sequence { yieldAll() }` 패턴을 사용하여 lazy [Sequence] 소비가 완료될 때까지
 * 스트림이 열려있도록 보장합니다.
 *
 * ```
 * val file = File("data.tsv")
 * val records = file.readAsTsvRecordsSuspending(Charsets.UTF_8, skipHeader = true)
 * records.forEach { println(it) }
 * ```
 *
 * @receiver TSV 파일
 * @param cs TSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader TSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @return TSV 파일의 레코드들
 */
fun File.readAsTsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Flow<Record> =
    FileInputStream(this).buffered().readAsTsvRecordsSuspending(cs, skipHeader)

/**
 * TSV 파일을 읽어 [transform] 함수를 통해 원하는 타입으로 변환합니다.
 *
 * ```
 * val file = File("data.tsv")
 * val items = file.readAsTsvRecordsSuspending(Charsets.UTF_8, skipHeader = true) { record ->
 *     Item(record.getString("name"), record.getInt("age"))
 * }
 * ```
 *
 * @receiver TSV 파일
 * @param cs TSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader TSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @param transform Record를 원하는 타입으로 변환하는 함수
 * @return 변환된 데이터의 시퀀스
 */
fun <T> File.readAsTsvRecordsSuspending(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> =
    FileInputStream(this).buffered().readAsTsvRecordsSuspending(cs, skipHeader, transform)
