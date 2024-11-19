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
 * ```
 * val file = File("data.csv")
 * val records = file.readAsCsvRecords(Charsets.UTF_8, skipHeader = true)
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
): Sequence<Record> {
    return FileInputStream(this).buffered().use { inputStream ->
        inputStream.readAsCsvRecords(cs, skipHeader)
    }
}

/**
 * TSV 파일을 읽어들여서 [Record] 로 변환합니다.
 *
 * ```
 * val file = File("data.tsv")
 * val records = file.readAsTsvRecords(Charsets.UTF_8, skipHeader = true)
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
): Sequence<Record> {
    return FileInputStream(this).buffered().use { inputStream ->
        inputStream.readAsTsvRecords(cs, skipHeader)
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
