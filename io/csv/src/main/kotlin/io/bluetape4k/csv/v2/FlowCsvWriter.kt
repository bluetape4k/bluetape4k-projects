package io.bluetape4k.csv.v2

import kotlinx.coroutines.flow.Flow
import java.io.Closeable
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Path

/**
 * CSV/TSV V2 비동기 라이터 인터페이스.
 *
 * Mutex로 동시성 보호가 적용되어 있으며,
 * [writeAll] 은 Flow를 순차적으로 collect하여 기록합니다.
 *
 * ## 사용 예
 * ```kotlin
 * val writer = csvWriter(outputWriter) { quoteAll = true }
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 *
 * val dataFlow: Flow<List<Any?>> = flowOf(listOf("Bob", 30))
 * writer.writeAll(dataFlow)
 * writer.close()
 * ```
 *
 * @see csvWriter
 * @see tsvWriter
 */
interface FlowCsvWriter : Closeable {

    /** 이 라이터의 설정. */
    val config: CsvWriterConfig

    /**
     * 헤더 행을 출력한다.
     *
     * @param headers 헤더명 목록
     */
    suspend fun writeHeaders(headers: Iterable<String>)

    /**
     * 한 행을 출력한다.
     *
     * @param row 필드 값 목록 (null 가능)
     */
    suspend fun writeRow(row: Iterable<*>)

    /**
     * [Flow]의 각 원소를 행으로 출력한다.
     *
     * Flow가 lazy이므로 메모리 폭발 없이 스트리밍 처리 가능합니다.
     * 단, 쓰기 중 예외 시 부분 파일이 남을 수 있습니다.
     *
     * @param rows 행 시퀀스를 나타내는 Flow
     */
    suspend fun writeAll(rows: Flow<Iterable<*>>)

    /**
     * [path]에 Flow의 내용을 CSV로 저장한다.
     *
     * @param path 출력 파일 경로
     * @param encoding 문자 인코딩 (기본값: UTF-8)
     * @param append `true`이면 파일 끝에 추가, `false`이면 덮어쓰기
     * @param skipHeaders `true`이면 헤더 행을 쓰지 않음
     * @param headers 헤더명 목록 (skipHeaders=false일 때 사용)
     * @param rows 출력할 행들의 Flow
     * @return 기록된 데이터 행 수
     */
    suspend fun writeFile(
        path: Path,
        encoding: Charset = Charsets.UTF_8,
        append: Boolean = false,
        skipHeaders: Boolean = true,
        headers: List<String> = emptyList(),
        rows: Flow<Iterable<*>>,
    ): Long
}

/**
 * [Writer]로부터 [FlowCsvWriter]를 생성하는 DSL 빌더.
 *
 * @param writer 출력 대상 Writer
 * @param block [CsvWriterConfig] 설정 블록
 */
fun csvWriter(writer: Writer, block: CsvWriterConfig.() -> Unit = {}): FlowCsvWriter =
    FlowCsvWriterImpl(writer, CsvWriterConfig().apply(block))

/**
 * [Writer]로부터 TSV 전용 [FlowCsvWriter]를 생성하는 DSL 빌더.
 * `delimiter`는 항상 `'\t'`로 강제됩니다.
 *
 * @param writer 출력 대상 Writer
 * @param block [CsvWriterConfig] 설정 블록 (delimiter 설정은 무시됨)
 */
fun tsvWriter(writer: Writer, block: CsvWriterConfig.() -> Unit = {}): FlowCsvWriter =
    FlowCsvWriterImpl(writer, CsvWriterConfig().apply(block).also { it.delimiter = '\t'; it.lineSeparator = "\n" })
