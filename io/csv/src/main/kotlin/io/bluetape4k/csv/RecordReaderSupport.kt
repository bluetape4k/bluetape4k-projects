package io.bluetape4k.csv

import com.univocity.parsers.common.record.Record
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8

/**
 * CSV 파일을 읽어들여서 [Record] 로 변환합니다.
 *
 * `sequence { yieldAll() }` 패턴을 사용하여 lazy [Sequence] 소비가 완료될 때까지
 * 스트림이 열려있도록 보장합니다.
 *
 * ```
 * val file = File("data.csv")
 * val records = file.readAsCsvRecords(Charsets.UTF_8, skipHeader = true)
 * records.forEach { println(it) }
 * ```
 *
 * @receiver CSV 파일
 * @param cs CSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader CSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @return CSV 파일의 레코드들
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
 * CSV 파일을 읽어 [transform] 함수를 통해 원하는 타입으로 변환합니다.
 *
 * ```
 * val file = File("data.csv")
 * val items = file.readAsCsvRecords(Charsets.UTF_8, skipHeader = true) { record ->
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
 * TSV 파일을 읽어들여서 [Record] 로 변환합니다.
 *
 * `sequence { yieldAll() }` 패턴을 사용하여 lazy [Sequence] 소비가 완료될 때까지
 * 스트림이 열려있도록 보장합니다.
 *
 * ```
 * val file = File("data.tsv")
 * val records = file.readAsTsvRecords(Charsets.UTF_8, skipHeader = true)
 * records.forEach { println(it) }
 * ```
 *
 * @receiver TSV 파일
 * @param cs TSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader TSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @return TSV 파일의 레코드들
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
 * TSV 파일을 읽어 [transform] 함수를 통해 원하는 타입으로 변환합니다.
 *
 * ```
 * val file = File("data.tsv")
 * val items = file.readAsTsvRecords(Charsets.UTF_8, skipHeader = true) { record ->
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
 * CSV 파일을 읽어들여서 [Record] 로 변환합니다.
 *
 * ```
 * val inputStream = FileInputStream("data.csv")
 * val records = inputStream.readAsCsvRecords(Charsets.UTF_8, skipHeader = true)
 * ```
 *
 * @receiver CSV 파일의 입력 스트림
 * @param cs CSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader CSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @return CSV 파일의 레코드들
 */
fun InputStream.readAsCsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Sequence<Record> =
    CsvRecordReader().read(this@readAsCsvRecords, cs, skipHeader) { it }

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
fun <T> InputStream.readAsCsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: (Record) -> T,
): Sequence<T> =
    CsvRecordReader().read(this@readAsCsvRecords, cs, skipHeader, transform)

/**
 * TSV 파일을 읽어들여서 [Record] 로 변환합니다.
 *
 * ```
 * val inputStream = FileInputStream("data.tsv")
 * val records = inputStream.readAsTsvRecords(Charsets.UTF_8, skipHeader = true)
 * ```
 *
 * @receiver TSV 파일의 입력 스트림
 * @param cs TSV 파일의 인코딩 (기본: UTF-8)
 * @param skipHeader TSV 파일의 헤더를 건너뛸지 여부 (기본: true)
 * @return TSV 파일의 레코드들
 */
fun InputStream.readAsTsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
): Sequence<Record> =
    TsvRecordReader().read(this@readAsTsvRecords, cs, skipHeader) { it }

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
fun <T> InputStream.readAsTsvRecords(
    cs: Charset = UTF_8,
    skipHeader: Boolean = true,
    transform: (Record) -> T,
): Sequence<T> =
    TsvRecordReader().read(this@readAsTsvRecords, cs, skipHeader, transform)
