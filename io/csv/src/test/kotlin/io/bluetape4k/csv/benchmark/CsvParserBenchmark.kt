package io.bluetape4k.csv.benchmark

import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.csv.internal.CsvLexer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Resourcex
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets.UTF_8

/**
 * CSV 파서 성능 벤치마크.
 *
 * 자체 [CsvLexer]와 univocity 파서의 처리량을 비교한다.
 * - 소형: product_type.csv 첫 10KB
 * - 중형: product_type.csv 전체
 *
 * PR 2 완료 후 Task 2.8에서 nativeCsvRead_* 를 CsvRecordReader 호출로 교체 예정.
 * PR 5에서 univocityCsvRead_* 삭제 예정.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class CsvParserBenchmark {

    companion object : KLogging()

    private lateinit var smallCsvBytes: ByteArray
    private lateinit var mediumCsvBytes: ByteArray

    @Setup(Level.Trial)
    fun setup() {
        smallCsvBytes = Resourcex.getInputStream("csv/product_type.csv")!!.use {
            it.readBytes().take(10_000).toByteArray()
        }
        mediumCsvBytes = Resourcex.getInputStream("csv/product_type.csv")!!.use {
            it.readBytes()
        }
    }

    // ── 자체 CsvLexer (PR 1: 내부 엔진 직접 측정)

    @Benchmark
    fun nativeCsvRead_small(): Int {
        var count = 0
        CsvLexer(smallCsvBytes.inputStream().reader(), CsvSettings.DEFAULT, skipHeaders = true).use { lexer ->
            while (lexer.hasNext()) { lexer.next(); count++ }
        }
        return count
    }

    @Benchmark
    fun nativeCsvRead_medium(): Int {
        var count = 0
        CsvLexer(mediumCsvBytes.inputStream().reader(), CsvSettings.DEFAULT, skipHeaders = true).use { lexer ->
            while (lexer.hasNext()) { lexer.next(); count++ }
        }
        return count
    }

    // ── univocity baseline (PR 5에서 삭제 예정)

    @Benchmark
    fun univocityCsvRead_small(): Int {
        var count = 0
        val parser = com.univocity.parsers.csv.CsvParser(
            com.univocity.parsers.csv.CsvParserSettings().apply { maxCharsPerColumn = 100_000 }
        )
        parser.iterateRecords(smallCsvBytes.inputStream(), UTF_8).forEach { count++ }
        return count
    }

    @Benchmark
    fun univocityCsvRead_medium(): Int {
        var count = 0
        val parser = com.univocity.parsers.csv.CsvParser(
            com.univocity.parsers.csv.CsvParserSettings().apply { maxCharsPerColumn = 100_000 }
        )
        parser.iterateRecords(mediumCsvBytes.inputStream(), UTF_8).forEach { count++ }
        return count
    }
}
