package io.bluetape4k.images.benchmark

import com.sksamuel.scrimage.filter.BlurFilter
import com.sksamuel.scrimage.filter.GrayscaleFilter
import com.sksamuel.scrimage.filter.SepiaFilter
import io.bluetape4k.logging.KLogging
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * scrimage 이미지 필터 처리 성능 측정 벤치마크.
 *
 * 문서 스캔 이미지(1240×1754)에 grayscale, blur, sepia 필터를 적용하는 시간을 측정합니다.
 * 문서 이미지를 사용하는 이유는 실제 OCR/문서 처리 파이프라인과 유사한 크기이기 때문입니다.
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-images-benchmark:benchmark
 * ```
 *
 * ## 측정 지표
 * - scrimage_grayscale: [GrayscaleFilter] 적용 평균 시간
 * - scrimage_blur: [BlurFilter] 적용 평균 시간
 * - scrimage_sepia: [SepiaFilter] 적용 평균 시간
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsPrepend = ["--enable-native-access=ALL-UNNAMED"])
@State(Scope.Benchmark)
class ImageFilterBenchmark {

    companion object : KLogging() {
        private val GRAYSCALE_FILTER = GrayscaleFilter()
        private val BLUR_FILTER = BlurFilter()
        private val SEPIA_FILTER = SepiaFilter()
    }

    /**
     * scrimage GrayscaleFilter 적용 성능 측정.
     *
     * 문서 스캔 이미지(1240×1754)를 흑백으로 변환합니다.
     */
    @Benchmark
    fun scrimage_grayscale(bh: Blackhole) {
        val result = BenchmarkImageSets.document.filter(GRAYSCALE_FILTER)
        bh.consume(result)
    }

    /**
     * scrimage BlurFilter 적용 성능 측정.
     *
     * 문서 스캔 이미지(1240×1754)에 기본 블러를 적용합니다.
     */
    @Benchmark
    fun scrimage_blur(bh: Blackhole) {
        val result = BenchmarkImageSets.document.filter(BLUR_FILTER)
        bh.consume(result)
    }

    /**
     * scrimage SepiaFilter 적용 성능 측정.
     *
     * 문서 스캔 이미지(1240×1754)에 세피아 톤을 적용합니다.
     */
    @Benchmark
    fun scrimage_sepia(bh: Blackhole) {
        val result = BenchmarkImageSets.document.filter(SEPIA_FILTER)
        bh.consume(result)
    }
}
