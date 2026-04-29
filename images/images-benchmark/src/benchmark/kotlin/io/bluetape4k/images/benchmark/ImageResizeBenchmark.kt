package io.bluetape4k.images.benchmark

import io.bluetape4k.logging.KLogging
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * scrimage vs vips 이미지 리사이즈 성능 비교 벤치마크.
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-images-benchmark:benchmark
 * ```
 *
 * ## 측정 지표
 * - scrimage: [ImmutableImage.scaleTo] 호출 평균 시간
 * - vips: [VipsImage.resize] 호출 평균 시간 (vips 미가용 시 skip)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsPrepend = ["--enable-native-access=ALL-UNNAMED"])
@State(Scope.Benchmark)
class ImageResizeBenchmark {

    companion object : KLogging()

    /**
     * 리사이즈 대상 해상도 (WxH 형식, 16:9 비율만 포함).
     *
     * 크로스곱 방지를 위해 단일 파라미터로 WxH 쌍을 표현합니다.
     */
    @Param("1920x1080", "1280x720")
    var resolution: String = "1920x1080"

    private var targetWidth: Int = 1920
    private var targetHeight: Int = 1080

    @Setup
    fun parseResolution() {
        val parts = resolution.split("x")
        targetWidth = parts[0].toInt()
        targetHeight = parts[1].toInt()
    }

    /**
     * scrimage ImmutableImage.scaleTo() 리사이즈 성능 측정.
     *
     * 4K 사진(3840×2160)을 [targetWidth]×[targetHeight]로 리사이즈합니다.
     */
    @Benchmark
    fun scrimage_scaleTo(bh: Blackhole) {
        val resized = BenchmarkImageSets.photo4k.scaleTo(targetWidth, targetHeight)
        bh.consume(resized)
    }

    /**
     * vips VipsImage.resize() 리사이즈 성능 측정.
     *
     * vips가 가용하지 않은 환경(CI 등)에서는 즉시 반환합니다.
     */
    @Benchmark
    fun vips_resize(state: VipsBenchmarkState, bh: Blackhole) {
        if (!state.vipsAvailable) {
            bh.consume(null)
            return
        }
        state.createVipsImage(state.photo4kJpegBytes).use { img ->
            val resized = img.resize(targetWidth, targetHeight)
            bh.consume(resized)
        }
    }
}
