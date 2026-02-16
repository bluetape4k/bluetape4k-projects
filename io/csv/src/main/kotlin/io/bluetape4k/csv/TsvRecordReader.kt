package io.bluetape4k.csv

import com.univocity.parsers.common.record.Record
import com.univocity.parsers.tsv.TsvParser
import com.univocity.parsers.tsv.TsvParserSettings
import io.bluetape4k.logging.KLogging
import java.io.InputStream
import java.nio.charset.Charset

/**
 * TSV(Tab-Separated Values) 포맷 파일을 읽어 [Record]로 변환하는 [RecordReader] 구현체입니다.
 *
 * ```
 * val reader = TsvRecordReader()
 * val items:Sequence<Item> = reader.read(input, Charsets.UTF_8, skipHeaders = true) { record ->
 *     // record 처리
 *     // record to item by recordMapper
 *     record.getString("name")
 *     record.getInt("age")
 *     // ...
 *     Item(record.getString("name"), record.getInt("age"))
 * }
 * ```
 *
 * @property settings TSV 파서 설정
 */
class TsvRecordReader(
    private val settings: TsvParserSettings = DefaultTsvParserSettings,
): RecordReader {

    companion object: KLogging()

    /**
     * TSV 입력 스트림에서 레코드를 순차적으로 읽어 [Sequence]로 반환합니다.
     *
     * @param input TSV 파일의 입력 스트림
     * @param encoding TSV 파일의 인코딩
     * @param skipHeaders TSV 파일의 헤더를 건너뛸지 여부
     * @param transform Record 를 원하는 타입으로 변환하는 함수
     * @return 변환된 데이터의 시퀀스
     */
    override fun <T> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        transform: (Record) -> T,
    ): Sequence<T> {
        return TsvParser(settings).iterateRecords(input, encoding)
            .asSequence()
            .drop(if (skipHeaders) 1 else 0)
            .map { record -> transform(record) }
    }
}
