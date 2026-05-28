package io.bluetape4k.csv.benchmark

import io.bluetape4k.csv.CsvRecordReader
import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.csv.internal.CsvLexer
import io.bluetape4k.csv.internal.OkioCsvLexer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Resourcex
import okio.buffer
import okio.source
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
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets.UTF_8

/**
 * CSV 파서 성능 벤치마크.
 *
 * 자체 엔진([CsvLexer], [CsvRecordReader])의 처리량을 측정한다.
 * - 소형: product_type.csv 첫 10KB
 * - 중형: product_type.csv 전체
 * - 대형: product_type.csv 반복 결합
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
    private lateinit var largeCsvBytes: ByteArray

    @Setup(Level.Trial)
    fun setup() {
        smallCsvBytes = Resourcex.getInputStream("csv/product_type.csv")!!.use {
            it.readBytes().take(10_000).toByteArray()
        }
        mediumCsvBytes = Resourcex.getInputStream("csv/product_type.csv")!!.use {
            it.readBytes()
        }
        largeCsvBytes = ByteArrayOutputStream(mediumCsvBytes.size * 16).use { output ->
            repeat(16) {
                output.write(mediumCsvBytes)
                output.write('\n'.code)
            }
            output.toByteArray()
        }
    }

    // ── 자체 CsvLexer (내부 엔진 직접 측정)

    @Benchmark
    fun nativeLexer_small(): Int {
        var count = 0
        CsvLexer(smallCsvBytes.inputStream().reader(), CsvSettings.DEFAULT, skipHeaders = true).use { lexer ->
            while (lexer.hasNext()) { lexer.next(); count++ }
        }
        return count
    }

    @Benchmark
    fun nativeLexer_medium(): Int {
        var count = 0
        CsvLexer(mediumCsvBytes.inputStream().reader(), CsvSettings.DEFAULT, skipHeaders = true).use { lexer ->
            while (lexer.hasNext()) { lexer.next(); count++ }
        }
        return count
    }

    @Benchmark
    fun nativeLexer_large(): Int {
        var count = 0
        CsvLexer(largeCsvBytes.inputStream().reader(), CsvSettings.DEFAULT, skipHeaders = true).use { lexer ->
            while (lexer.hasNext()) { lexer.next(); count++ }
        }
        return count
    }

    // ── Okio CsvLexer (segment-backed UTF-8 engine)

    @Benchmark
    fun okioLexer_small(): Int {
        var count = 0
        OkioCsvLexer(smallCsvBytes.inputStream().source().buffer(), CsvSettings.DEFAULT, skipHeaders = true).use { lexer ->
            while (lexer.hasNext()) { lexer.next(); count++ }
        }
        return count
    }

    @Benchmark
    fun okioLexer_medium(): Int {
        var count = 0
        OkioCsvLexer(mediumCsvBytes.inputStream().source().buffer(), CsvSettings.DEFAULT, skipHeaders = true).use { lexer ->
            while (lexer.hasNext()) { lexer.next(); count++ }
        }
        return count
    }

    @Benchmark
    fun okioLexer_large(): Int {
        var count = 0
        OkioCsvLexer(largeCsvBytes.inputStream().source().buffer(), CsvSettings.DEFAULT, skipHeaders = true).use { lexer ->
            while (lexer.hasNext()) { lexer.next(); count++ }
        }
        return count
    }

    // ── 자체 CsvRecordReader (공개 API 측정)

    @Benchmark
    fun nativeCsvRead_small(): Int =
        CsvRecordReader().read(smallCsvBytes.inputStream(), UTF_8, skipHeaders = true).count()

    @Benchmark
    fun nativeCsvRead_medium(): Int =
        CsvRecordReader().read(mediumCsvBytes.inputStream(), UTF_8, skipHeaders = true).count()

    @Benchmark
    fun nativeCsvRead_large(): Int =
        CsvRecordReader().read(largeCsvBytes.inputStream(), UTF_8, skipHeaders = true).count()

}
