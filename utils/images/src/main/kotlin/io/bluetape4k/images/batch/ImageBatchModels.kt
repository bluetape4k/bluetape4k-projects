package io.bluetape4k.images.batch

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.coroutines.SuspendImageWriter
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.Dispatchers
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

/**
 * 이미지 배치 처리 실패 단계입니다.
 */
enum class ImageBatchFailureStage {
    VALIDATION,
    LOAD,
    TRANSFORM,
    WRITE,
}

/**
 * 배치 이미지 처리 실패를 원본 경로와 처리 단계와 함께 전달하는 예외입니다.
 */
class ImageBatchException(
    val source: Path,
    val stage: ImageBatchFailureStage,
    val output: Path? = null,
    message: String,
    cause: Throwable? = null,
): RuntimeException(message, cause)

/**
 * 배치 이미지 처리 결과입니다.
 */
sealed interface ImageBatchResult {
    /**
     * 입력 이미지 경로입니다.
     */
    val source: Path

    /**
     * 이미지 변환에 성공했지만 writer가 선택되지 않은 결과입니다.
     */
    data class Image(
        override val source: Path,
        val image: ImmutableImage,
    ): ImageBatchResult

    /**
     * 이미지 변환과 writer 선택에 성공한 결과입니다.
     */
    data class WritableImage(
        override val source: Path,
        val image: ImmutableImage,
        val writer: SuspendImageWriter,
    ): ImageBatchResult

    /**
     * `skipFailures=true`에서 스트림 안으로 전달되는 실패 결과입니다.
     */
    data class Failure(
        override val source: Path,
        val stage: ImageBatchFailureStage,
        val output: Path? = null,
        val cause: Throwable,
    ): ImageBatchResult
}

/**
 * 이미지 배치 처리 옵션입니다.
 *
 * [processImages] / [processImageFiles] / [writeImagesTo]의 동작을 제어합니다.
 * 기본값으로 간단히 생성하거나, 특수 상황에 맞게 각 파라미터를 조정하세요.
 *
 * ```kotlin
 * // 기본 옵션 — 대부분의 경우에 적합
 * val defaultOptions = ImageProcessingOptions()
 *
 * // 커스텀 옵션 — 병렬도 4, 실패 시 건너뜀, 실패 로그 기록
 * val options = ImageProcessingOptions(
 *     parallelism = 4,
 *     maxPixels = 4_000L * 3_000L,           // 12 MP 제한
 *     maxInFlightPixels = 100_000_000L,       // 동시 픽셀 1억
 *     skipFailures = true,
 *     onFailure = { failure ->
 *         println("처리 실패: ${failure.source} — ${failure.cause.message}")
 *     },
 * )
 *
 * flowOf(imagePath)
 *     .processImages(options) {
 *         resize(800, 600)
 *         toJpeg(quality = 85)
 *     }
 *     .writeImagesTo(outputDir)
 * ```
 *
 * @property ioDispatcher 이미지 읽기/쓰기 작업에 사용할 컨텍스트
 * @property transformDispatcher 이미지 변환 작업에 사용할 컨텍스트
 * @property parallelism 동시에 처리할 이미지 수
 * @property maxPixels 단일 이미지 허용 픽셀 수
 * @property maxInFlightPixels 동시에 처리 중인 이미지의 픽셀 총량
 * @property skipFailures 실패를 결과로 흘려보낼지 여부
 * @property onFailure 실패 관측 콜백
 */
data class ImageProcessingOptions(
    val ioDispatcher: CoroutineContext = Dispatchers.IO,
    val transformDispatcher: CoroutineContext = Dispatchers.Default,
    val parallelism: Int = defaultImageBatchParallelism(),
    val maxPixels: Long = DEFAULT_MAX_PIXELS,
    val maxInFlightPixels: Long = DEFAULT_MAX_IN_FLIGHT_PIXELS,
    val skipFailures: Boolean = false,
    val onFailure: suspend (ImageBatchResult.Failure) -> Unit = {},
) {
    init {
        parallelism.requirePositiveNumber("parallelism")
        maxPixels.requirePositiveNumber("maxPixels")
        maxInFlightPixels.requirePositiveNumber("maxInFlightPixels")
    }

    companion object {
        /**
         * 큰 이미지/대용량 배치를 위한 옵션을 생성합니다.
         *
         * 기본 [ImageProcessingOptions]보다 픽셀 한도를 크게 잡되, 호출자가 명시적으로
         * `maxPixels`와 `maxInFlightPixels`를 더 조정할 수 있도록 열어둡니다.
         *
         * 고해상도 RAW 사진이나 의료·위성 이미지처럼 단일 파일이 수천만 픽셀에 달하는
         * 워크로드에 적합합니다.
         *
         * ```kotlin
         * // 대용량 배치: 픽셀 한도를 확장하고 실패를 건너뜀
         * val options = ImageProcessingOptions.largeJobs(
         *     parallelism = 2,
         *     skipFailures = true,
         *     onFailure = { failure ->
         *         logger.warn { "대용량 이미지 처리 실패: ${failure.source}" }
         *     },
         * )
         *
         * flowOf(highResImagePath)
         *     .processImages(options) {
         *         resize(3840, 2160)   // 4K 다운스케일
         *         toJpeg(quality = 92)
         *     }
         *     .writeImagesTo(outputDir)
         * ```
         */
        fun largeJobs(
            ioDispatcher: CoroutineContext = Dispatchers.IO,
            transformDispatcher: CoroutineContext = Dispatchers.Default,
            parallelism: Int = defaultImageBatchParallelism(),
            maxPixels: Long = LARGE_JOB_MAX_PIXELS,
            maxInFlightPixels: Long = LARGE_JOB_MAX_IN_FLIGHT_PIXELS,
            skipFailures: Boolean = false,
            onFailure: suspend (ImageBatchResult.Failure) -> Unit = {},
        ): ImageProcessingOptions =
            ImageProcessingOptions(
                ioDispatcher = ioDispatcher,
                transformDispatcher = transformDispatcher,
                parallelism = parallelism,
                maxPixels = maxPixels,
                maxInFlightPixels = maxInFlightPixels,
                skipFailures = skipFailures,
                onFailure = onFailure,
            )
    }
}
