package io.bluetape4k.images.thumbnail

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.coroutines.flow.extensions.mapParallel
import io.bluetape4k.images.batch.ImageBatchFailureStage
import io.bluetape4k.images.batch.ImageBatchException
import io.bluetape4k.images.batch.ImageProcessingOptions
import io.bluetape4k.images.batch.PixelPermitLimiter
import io.bluetape4k.images.batch.probeImagePixelCount
import io.bluetape4k.images.coroutines.SuspendImageWriter
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.images.transforms.smartCropTo
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * 여러 크기의 썸네일을 동시에 생성하는 코루틴 기반 파이프라인입니다.
 *
 * [builder]로 파이프라인을 구성하고 [process]에 이미지 경로 Flow를 넘기면
 * 각 원본 이미지에 대해 등록된 모든 크기의 썸네일을 병렬로 생성합니다.
 *
 * 픽셀 한도([ImageProcessingOptions.maxPixels])를 초과하는 이미지나 출력 경로가 중복되는
 * 경우는 VALIDATION 단계 실패로 처리됩니다. [ImageProcessingOptions.skipFailures]가
 * `true`이면 실패 항목을 건너뛰고 계속 처리하며, `false`(기본값)이면 예외를 던져 중단합니다.
 *
 * ```kotlin
 * import io.bluetape4k.images.batch.ImageProcessingOptions
 * import io.bluetape4k.images.coroutines.SuspendJpegWriter
 * import io.bluetape4k.images.thumbnail.*
 * import kotlinx.coroutines.flow.asFlow
 * import java.nio.file.Path
 *
 * // 1. 파이프라인 구성
 * val pipeline = ThumbnailPipeline.builder()
 *     .outputDirectory(Path.of("output/thumbs"))
 *     .size(width = 1280, height = 720, suffix = "hd")
 *     .size(width = 640, height = 360, suffix = "md")
 *     .size(width = 320, height = 180, suffix = "sm")
 *     .format(ThumbnailFormat(SuspendJpegWriter.Default.withCompression(85), "jpg"))
 *     .crop(ThumbnailCrop.Smart())
 *     .options(ImageProcessingOptions(parallelism = 4, skipFailures = true))
 *     .onFailure { result ->
 *         println("썸네일 생성 실패: source=${result.source}, stage=${result.status}")
 *     }
 *     .build()
 *
 * // 2. Flow<Path> 입력을 넘겨 처리
 * val results = pipeline
 *     .process(listOf(Path.of("photo1.jpg"), Path.of("photo2.png")).asFlow())
 *     .toList()
 *
 * results.forEach { result ->
 *     when (val status = result.status) {
 *         is ThumbnailStatus.Success -> println("생성 완료: ${result.output} (${status.bytes} bytes)")
 *         is ThumbnailStatus.Failure -> println("생성 실패: ${result.source} at ${status.stage}")
 *     }
 * }
 * ```
 *
 * @see builder
 * @see process
 */
class ThumbnailPipeline private constructor(
    private val outputDirectory: Path,
    private val sizes: List<ThumbnailSize>,
    private val crop: ThumbnailCrop,
    private val format: ThumbnailFormat,
    private val outputName: ThumbnailOutputName,
    private val ioDispatcher: CoroutineContext,
    private val transformDispatcher: CoroutineContext,
    private val parallelism: Int,
    private val maxPixels: Long,
    private val maxInFlightPixels: Long,
    private val skipFailures: Boolean,
    private val onFailure: suspend (ThumbnailResult) -> Unit,
) {
    /**
     * 입력 이미지 경로 스트림을 썸네일 결과 스트림으로 처리합니다.
     *
     * 동일한 출력 경로는 이 파이프라인 인스턴스 내에서 한 번만 처리됩니다.
     * 같은 출력 경로로 두 번 이상 시도하면 VALIDATION 단계 실패로 처리됩니다.
     *
     * 각 소스 이미지에 대해 등록된 모든 크기의 썸네일이 병렬로 생성되며,
     * 결과는 [ThumbnailResult]의 스트림으로 반환됩니다.
     *
     * ```kotlin
     * import kotlinx.coroutines.flow.asFlow
     * import kotlinx.coroutines.flow.collect
     * import java.nio.file.Path
     *
     * val pipeline = ThumbnailPipeline.builder()
     *     .outputDirectory(Path.of("output/thumbs"))
     *     .size(320, 240, suffix = "small")
     *     .size(640, 480, suffix = "medium")
     *     .build()
     *
     * // Flow<Path> 입력 — 파일 목록을 asFlow()로 변환
     * val sourcePaths = listOf(
     *     Path.of("images/photo1.jpg"),
     *     Path.of("images/photo2.png"),
     * ).asFlow()
     *
     * pipeline.process(sourcePaths).collect { result ->
     *     when (val s = result.status) {
     *         is ThumbnailStatus.Success ->
     *             println("[OK] ${result.output.fileName} — ${s.bytes} bytes")
     *         is ThumbnailStatus.Failure ->
     *             println("[FAIL] ${result.source.fileName} stage=${s.stage}")
     *     }
     * }
     * ```
     *
     * @param sourceImages 처리할 원본 이미지 경로의 Flow
     * @return 각 (소스 × 크기) 조합에 대한 [ThumbnailResult] Flow
     */
    fun process(sourceImages: Flow<Path>): Flow<ThumbnailResult> {
        val limiter = PixelPermitLimiter(maxInFlightPixels)
        val seenOutputs = ConcurrentHashMap.newKeySet<Path>()

        return sourceImages
            .flatMapConcat { source -> sizes.asFlow().map { size -> source to size } }
            .mapParallel(parallelism) { (source, size) ->
                processOne(source, size, limiter, seenOutputs)
            }
    }

    private suspend fun processOne(
        source: Path,
        size: ThumbnailSize,
        limiter: PixelPermitLimiter,
        seenOutputs: MutableSet<Path>,
    ): ThumbnailResult {
        var output = outputDirectory
        try {
            val rawOutputName = runStage(source, output, ImageBatchFailureStage.VALIDATION) {
                outputName.create(source, size, format)
            }
            output = runStage(source, output, ImageBatchFailureStage.VALIDATION) {
                resolveOutputPath(rawOutputName)
            }
            runStage(source, output, ImageBatchFailureStage.VALIDATION) {
                if (!seenOutputs.add(output)) {
                    throw IllegalArgumentException("썸네일 출력 경로가 중복됩니다: $output")
                }
            }

            val probedPixels = runStage(source, output, ImageBatchFailureStage.VALIDATION) {
                withContext(ioDispatcher) { probeImagePixelCount(source) }
            }
            runStage(source, output, ImageBatchFailureStage.VALIDATION) {
                probedPixels?.requireWithinMaxPixels(source)
            }
            val permitPixels = probedPixels ?: maxPixels

            return limiter.withPermit(permitPixels) {
                val image = runStage(source, output, ImageBatchFailureStage.LOAD) {
                    withContext(ioDispatcher) { immutableImageOf(source) }
                }
                runStage(source, output, ImageBatchFailureStage.VALIDATION) {
                    image.pixelCount().requireWithinMaxPixels(source)
                }
                val thumbnail = runStage(source, output, ImageBatchFailureStage.TRANSFORM) {
                    withContext(transformDispatcher) { image.toThumbnail(size, crop) }
                }
                val bytes = runStage(source, output, ImageBatchFailureStage.WRITE) {
                    writeThumbnail(thumbnail, output)
                }
                ThumbnailResult(source, output, size, ThumbnailStatus.Success(bytes), thumbnail)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ImageBatchException) {
            return handleFailure(e, source, output, size)
        } catch (e: Throwable) {
            return handleFailure(
                ImageBatchException(
                    source = source,
                    stage = ImageBatchFailureStage.TRANSFORM,
                    output = output,
                    message = "썸네일 생성에 실패했습니다. source=$source, output=$output",
                    cause = e,
                ),
                source,
                output,
                size,
            )
        }
    }

    private suspend fun handleFailure(
        exception: ImageBatchException,
        source: Path,
        output: Path,
        size: ThumbnailSize,
    ): ThumbnailResult {
        val result = ThumbnailResult(
            source = source,
            output = output,
            size = size,
            status = ThumbnailStatus.Failure(exception.stage, exception.cause ?: exception),
        )

        if (!skipFailures) {
            throw exception
        }

        log.warn(exception) { "썸네일 생성을 건너뜁니다. source=$source, output=$output, stage=${exception.stage}" }
        onFailure(result)
        return result
    }

    private suspend inline fun <T> runStage(
        source: Path,
        output: Path,
        stage: ImageBatchFailureStage,
        crossinline block: suspend () -> T,
    ): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ImageBatchException) {
            throw e
        } catch (e: Throwable) {
            throw ImageBatchException(
                source = source,
                stage = stage,
                output = output,
                message = "썸네일 처리 단계가 실패했습니다. source=$source, output=$output, stage=$stage",
                cause = e,
            )
        }

    private suspend fun writeThumbnail(image: ImmutableImage, output: Path): Long =
        withContext(ioDispatcher) {
            output.parent?.let(Files::createDirectories)
            Files.newOutputStream(output).use { stream ->
                format.writer.write(image, stream)
            }
            Files.size(output)
        }

    private fun ImmutableImage.toThumbnail(size: ThumbnailSize, crop: ThumbnailCrop): ImmutableImage =
        when (crop) {
            ThumbnailCrop.Fit -> scaleTo(size.width, size.height)
            is ThumbnailCrop.Smart -> smartCropTo(size.width, size.height, crop.strategy)
        }

    private fun Long.requireWithinMaxPixels(source: Path) {
        if (this > maxPixels) {
            throw ImageBatchException(
                source = source,
                stage = ImageBatchFailureStage.VALIDATION,
                message = "이미지 픽셀 수가 허용 한도를 초과했습니다. source=$source, pixels=$this, maxPixels=$maxPixels",
            )
        }
    }

    private fun ImmutableImage.pixelCount(): Long =
        width.toLong() * height.toLong()

    private fun resolveOutputPath(outputName: String): Path {
        require(outputName.isNotBlank()) { "outputName은 blank일 수 없습니다." }
        val base = outputDirectory.toAbsolutePath().normalize()
        val output = base.resolve(outputName).normalize()
        require(output.startsWith(base)) {
            "썸네일 출력 경로가 outputDirectory를 벗어날 수 없습니다. outputDirectory=$outputDirectory, outputName=$outputName"
        }
        return output
    }

    /**
     * [ThumbnailPipeline] 빌더입니다.
     */
    class Builder {
        private var outputDirectory: Path? = null
        private val sizes = mutableListOf<ThumbnailSize>()
        private var crop: ThumbnailCrop = ThumbnailCrop.Fit
        private var format: ThumbnailFormat = ThumbnailFormat.Jpeg
        private var outputName: ThumbnailOutputName = ThumbnailOutputName.Default
        private var options: ImageProcessingOptions = ImageProcessingOptions()
        private var onFailure: suspend (ThumbnailResult) -> Unit = {}

        /**
         * 출력 디렉터리를 지정합니다.
         */
        fun outputDirectory(path: Path): Builder = apply {
            outputDirectory = path
        }

        /**
         * 썸네일 크기를 추가합니다.
         *
         * 여러 번 호출하여 다양한 크기를 한 번에 등록할 수 있습니다.
         * [suffix]를 생략하면 `"${width}x${height}"` 형식이 기본값으로 사용됩니다.
         *
         * ```kotlin
         * ThumbnailPipeline.builder()
         *     .outputDirectory(Path.of("thumbs"))
         *     .size(width = 1920, height = 1080, suffix = "fhd")   // Full HD
         *     .size(width = 1280, height = 720,  suffix = "hd")    // HD
         *     .size(width = 640,  height = 360,  suffix = "sd")    // SD
         *     .size(width = 320,  height = 180)                    // 기본 suffix = "320x180"
         *     .build()
         * ```
         *
         * @param width 썸네일 너비 (픽셀)
         * @param height 썸네일 높이 (픽셀)
         * @param suffix 출력 파일명에 추가될 크기 접미사 (기본값: `"${width}x${height}"`)
         */
        fun size(width: Int, height: Int, suffix: String = "${width}x$height"): Builder = apply {
            sizes += ThumbnailSize(width, height, suffix)
        }

        /**
         * 썸네일 크롭 전략을 지정합니다.
         */
        fun crop(crop: ThumbnailCrop): Builder = apply {
            this.crop = crop
        }

        /**
         * 출력 포맷을 지정합니다.
         */
        fun format(format: ThumbnailFormat): Builder = apply {
            this.format = format
        }

        /**
         * writer를 직접 지정합니다.
         */
        fun writer(writer: SuspendImageWriter): Builder = apply {
            this.format = format.copy(writer = writer)
        }

        /**
         * 출력 파일명 생성기를 지정합니다.
         */
        fun outputName(outputName: ThumbnailOutputName): Builder = apply {
            this.outputName = outputName
        }

        /**
         * 배치 처리 옵션을 지정합니다.
         */
        fun options(options: ImageProcessingOptions): Builder = apply {
            this.options = options
        }

        /**
         * 실패 관측 콜백을 지정합니다.
         */
        fun onFailure(onFailure: suspend (ThumbnailResult) -> Unit): Builder = apply {
            this.onFailure = onFailure
        }

        /**
         * 설정된 옵션으로 [ThumbnailPipeline]을 생성합니다.
         *
         * [outputDirectory]는 필수이며, [size]를 한 번도 호출하지 않은 경우
         * 기본 크기(320×240)가 자동으로 사용됩니다.
         *
         * ```kotlin
         * import io.bluetape4k.images.batch.ImageProcessingOptions
         * import io.bluetape4k.images.coroutines.SuspendWebpWriter
         * import io.bluetape4k.images.thumbnail.*
         * import java.nio.file.Path
         *
         * val pipeline = ThumbnailPipeline.builder()
         *     .outputDirectory(Path.of("output/thumbs"))
         *     .size(640, 480, suffix = "medium")
         *     .size(320, 240, suffix = "small")
         *     .format(ThumbnailFormat(SuspendWebpWriter.Default, "webp"))
         *     .crop(ThumbnailCrop.Smart())
         *     .options(ImageProcessingOptions(parallelism = 4, skipFailures = true))
         *     .onFailure { result ->
         *         println("실패: ${result.source} → ${result.status}")
         *     }
         *     .build()   // ThumbnailPipeline 인스턴스 반환
         * ```
         *
         * @return 구성된 [ThumbnailPipeline] 인스턴스
         * @throws IllegalArgumentException [outputDirectory]가 설정되지 않은 경우
         */
        fun build(): ThumbnailPipeline {
            val directory = requireNotNull(outputDirectory) { "outputDirectory를 지정해야 합니다." }
            val thumbnailSizes = sizes.ifEmpty { listOf(DEFAULT_THUMBNAIL_SIZE) }
            options.parallelism.requirePositiveNumber("parallelism")

            return ThumbnailPipeline(
                outputDirectory = directory,
                sizes = thumbnailSizes,
                crop = crop,
                format = format,
                outputName = outputName,
                ioDispatcher = options.ioDispatcher,
                transformDispatcher = options.transformDispatcher,
                parallelism = options.parallelism,
                maxPixels = options.maxPixels,
                maxInFlightPixels = options.maxInFlightPixels,
                skipFailures = options.skipFailures,
                onFailure = onFailure,
            )
        }
    }

    companion object: KLoggingChannel() {
        private const val DEFAULT_THUMBNAIL_WIDTH = 320
        private const val DEFAULT_THUMBNAIL_HEIGHT = 240
        private val DEFAULT_THUMBNAIL_SIZE = ThumbnailSize(DEFAULT_THUMBNAIL_WIDTH, DEFAULT_THUMBNAIL_HEIGHT)

        /**
         * [ThumbnailPipeline] 빌더를 생성합니다.
         */
        fun builder(): Builder = Builder()
    }
}
