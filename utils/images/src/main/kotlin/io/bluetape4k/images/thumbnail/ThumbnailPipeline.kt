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
import io.bluetape4k.logging.KotlinLogging
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

private val log = KotlinLogging.logger {}

/**
 * 여러 크기의 썸네일을 생성하는 파이프라인입니다.
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
     * 입력 이미지 스트림을 썸네일 결과 스트림으로 처리합니다.
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
            throw IllegalArgumentException("이미지 픽셀 수가 허용 한도를 초과했습니다. source=$source, pixels=$this, maxPixels=$maxPixels")
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
         * 파이프라인을 생성합니다.
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

    companion object {
        private const val DEFAULT_THUMBNAIL_WIDTH = 320
        private const val DEFAULT_THUMBNAIL_HEIGHT = 240
        private val DEFAULT_THUMBNAIL_SIZE = ThumbnailSize(DEFAULT_THUMBNAIL_WIDTH, DEFAULT_THUMBNAIL_HEIGHT)

        /**
         * [ThumbnailPipeline] 빌더를 생성합니다.
         */
        fun builder(): Builder = Builder()
    }
}
