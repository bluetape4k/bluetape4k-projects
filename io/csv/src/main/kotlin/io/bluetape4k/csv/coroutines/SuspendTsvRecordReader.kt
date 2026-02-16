package io.bluetape4k.csv.coroutines

import com.univocity.parsers.common.record.Record
import com.univocity.parsers.tsv.TsvParser
import com.univocity.parsers.tsv.TsvParserSettings
import io.bluetape4k.csv.DefaultTsvParserSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Coroutines 환경에서 TSV 파일을 읽어 [Flow]로 변환하는 [SuspendRecordReader] 구현체입니다.
 *
 * ```
 * val reader = SuspendTsvRecordReader()
 * val items:Flow<Item> = reader.read(input, Charsets.UTF_8, skipHeaders = true) { record ->
 *     // record 처리
 *     // record to item by recordMapper
 *     val name = record.getString("name")
 *     val age = record.getInt("age")
 *     // ...
 *     Item(name, age)
 * }
 * ```
 *
 * @property settings TSV 파서 설정
 */
class SuspendTsvRecordReader(
    private val settings: TsvParserSettings = DefaultTsvParserSettings,
): SuspendRecordReader {

    companion object: KLoggingChannel()

    /**
     * TSV 입력 스트림에서 레코드를 읽어 [Flow]로 방출합니다.
     *
     * @param input TSV 파일의 입력 스트림
     * @param encoding TSV 파일의 인코딩
     * @param skipHeaders TSV 파일의 헤더를 건너뛸지 여부
     * @param transform Record 를 원하는 타입으로 변환하는 함수
     * @return 변환된 데이터의 Flow
     */
    override fun <T: Any> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        transform: (Record) -> T,
    ): Flow<T> {
        return TsvParser(settings)
            .iterateRecords(input, encoding)
            .asFlow()
            .drop(if (skipHeaders) 1 else 0)
            .map { transform(it) }
    }

    /**
     * CSV/TSV 처리 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        // Nothing to do
    }
}
