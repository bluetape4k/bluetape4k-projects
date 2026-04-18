package io.bluetape4k.csv.v2

import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Path

/**
 * CSV/TSV V2 비동기 리더 인터페이스.
 *
 * [kotlinx.coroutines.flow.Flow]를 반환하므로 대용량 파일을 스트리밍 방식으로
 * 처리할 수 있으며 코루틴 취소(cooperative cancellation)를 지원합니다.
 *
 * ## 사용 예
 * ```kotlin
 * val reader = csvReader { trimValues = true }
 * reader.read(inputStream, skipHeaders = true).collect { row ->
 *     println("${row.getString("name")}, ${row.getInt("age")}")
 * }
 * ```
 *
 * @see csvReader
 * @see tsvReader
 */
interface FlowCsvReader {

    /** 이 리더의 설정. */
    val config: CsvReaderConfig

    /**
     * [InputStream]을 읽어 [CsvRow] [Flow]를 반환한다.
     *
     * 반환된 Flow는 `flowOn(Dispatchers.IO)` 위에서 실행되므로
     * blocking IO가 IO 디스패처에서 처리됩니다.
     *
     * @param input 입력 스트림
     * @param encoding 문자 인코딩 (기본값: UTF-8)
     * @param skipHeaders `true`이면 첫 번째 행을 헤더로 저장하고 이후 행부터 반환
     */
    fun read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = false,
    ): Flow<CsvRow>

    /**
     * 파일을 스트리밍 방식으로 읽어 [CsvRow] [Flow]를 반환한다.
     *
     * RFC 4180 멀티라인 인용 필드를 올바르게 처리하기 위해
     * line-based reader 대신 `FileInputStream`으로 직접 읽습니다.
     *
     * @param path 읽을 파일 경로
     * @param encoding 문자 인코딩 (기본값: UTF-8)
     * @param skipHeaders `true`이면 첫 번째 행을 헤더로 저장하고 이후 행부터 반환
     */
    fun readFile(
        path: Path,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = false,
    ): Flow<CsvRow>
}
