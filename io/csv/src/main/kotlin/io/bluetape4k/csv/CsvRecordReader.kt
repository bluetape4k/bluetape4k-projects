package io.bluetape4k.csv

import com.univocity.parsers.common.record.Record
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import io.bluetape4k.logging.KLogging
import java.io.InputStream
import java.nio.charset.Charset

/**
 * univocity CSV 파서를 사용하는 [RecordReader] 구현체입니다.
 *
 * ## 동작/계약
 * - [settings]로 생성한 [CsvParser]가 입력을 순차 파싱합니다.
 * - [skipHeaders]가 `true`면 파싱 결과에서 첫 레코드를 drop 합니다.
 * - 반환 시퀀스는 lazy이며 소비 시점에 변환 람다가 실행됩니다.
 *
 * ```kotlin
 * val names = CsvRecordReader()
 *     .read(input, skipHeaders = true) { it.getString("name") }
 *     .toList()
 * // names == listOf("Alice", "Bob")
 * ```
 */
class CsvRecordReader(
    private val settings: CsvParserSettings = DefaultCsvParserSettings,
): RecordReader {

    companion object: KLogging()

    /**
     * CSV 입력 스트림을 읽어 변환 결과 시퀀스를 반환합니다.
     *
     * ## 동작/계약
     * - `iterateRecords(input, encoding)` 결과를 [Sequence]로 감싸 반환합니다.
     * - `drop(1)`로 헤더 스킵을 처리하므로 `skipHeaders=true`면 첫 행은 결과에 포함되지 않습니다.
     * - 파싱 또는 [transform] 실패 예외는 호출자에게 전파됩니다.
     *
     * ```kotlin
     * val ids = CsvRecordReader().read(input, skipHeaders = true) { it.getLong("id") }.toList()
     * // ids == listOf(1L, 2L)
     * ```
     */
    override fun <T> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        transform: (Record) -> T,
    ): Sequence<T> {
        return CsvParser(settings)
            .iterateRecords(input, encoding)
            .asSequence()
            .drop(if (skipHeaders) 1 else 0)
            .map { record -> transform(record) }
    }
}
