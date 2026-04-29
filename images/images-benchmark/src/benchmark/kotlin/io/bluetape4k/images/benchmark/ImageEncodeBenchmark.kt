package io.bluetape4k.images.benchmark

import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import io.bluetape4k.images.vips.VipsImageFormat
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
 * scrimage vs vips 이미지 인코딩(JPEG/PNG) 성능 비교 벤치마크.
 *
 * 4K 사진(3840×2160)을 JPEG와 PNG로 인코딩하는 시간을 비교합니다.
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-images-benchmark:benchmark
 * ```
 *
 * ## 측정 지표
 * - scrimage_encodeJpeg: [JpegWriter]로 JPEG 인코딩 평균 시간
 * - scrimage_encodePng: [PngWriter]로 PNG 인코딩 평균 시간
 * - vips_encodeJpeg: vips JPEG 인코딩 평균 시간 (vips 미가용 시 skip)
 * - vips_encodePng: vips PNG 인코딩 평균 시간 (vips 미가용 시 skip)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsPrepend = ["--enable-native-access=ALL-UNNAMED"])
@State(Scope.Benchmark)
class ImageEncodeBenchmark {

    companion object : KLogging() {
        private val JPEG_WRITER = JpegWriter(80, false)
        private val PNG_WRITER = PngWriter(6)
    }

    /**
     * scrimage JPEG 인코딩 성능 측정.
     *
     * 4K 사진(3840×2160)을 quality=80으로 JPEG 인코딩합니다.
     */
    @Benchmark
    fun scrimage_encodeJpeg(bh: Blackhole) {
        val bytes = BenchmarkImageSets.photo4k.bytes(JPEG_WRITER)
        bh.consume(bytes)
    }

    /**
     * scrimage PNG 인코딩 성능 측정.
     *
     * 4K 사진(3840×2160)을 compression=6으로 PNG 인코딩합니다.
     */
    @Benchmark
    fun scrimage_encodePng(bh: Blackhole) {
        val bytes = BenchmarkImageSets.photo4k.bytes(PNG_WRITER)
        bh.consume(bytes)
    }

    /**
     * vips JPEG 인코딩 성능 측정.
     *
     * vips가 가용하지 않은 환경(CI 등)에서는 즉시 반환합니다.
     */
    @Benchmark
    fun vips_encodeJpeg(state: VipsBenchmarkState, bh: Blackhole) {
        if (!state.vipsAvailable) {
            bh.consume(null)
            return
        }
        state.createVipsImage(state.photo4kJpegBytes).use { img ->
            val bytes = img.toBytes(VipsImageFormat.JPEG)
            bh.consume(bytes)
        }
    }

    /**
     * vips PNG 인코딩 성능 측정.
     *
     * vips가 가용하지 않은 환경(CI 등)에서는 즉시 반환합니다.
     */
    @Benchmark
    fun vips_encodePng(state: VipsBenchmarkState, bh: Blackhole) {
        if (!state.vipsAvailable) {
            bh.consume(null)
            return
        }
        state.createVipsImage(state.photo4kJpegBytes).use { img ->
            val bytes = img.toBytes(VipsImageFormat.PNG)
            bh.consume(bytes)
        }
    }
}
