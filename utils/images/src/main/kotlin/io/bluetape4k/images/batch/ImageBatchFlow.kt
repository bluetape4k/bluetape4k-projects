package io.bluetape4k.images.batch

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.coroutines.flow.extensions.mapParallel
import io.bluetape4k.images.coroutines.SuspendImageWriter
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger {}

/**
 * [Path] 스트림을 이미지 배치 처리 결과 스트림으로 변환합니다.
 */
fun Flow<Path>.processImages(
    options: ImageProcessingOptions = ImageProcessingOptions(),
    block: ImageProcessingDsl.() -> Unit,
): Flow<ImageBatchResult> {
    val dsl = ImageProcessingDsl().apply(block)
    val limiter = PixelPermitLimiter(options.maxInFlightPixels)

    return mapParallel(options.parallelism) { source ->
        processOneImage(source, dsl, options, limiter)
    }
}

/**
 * [File] 스트림을 이미지 배치 처리 결과 스트림으로 변환합니다.
 */
fun Flow<File>.processImageFiles(
    options: ImageProcessingOptions = ImageProcessingOptions(),
    block: ImageProcessingDsl.() -> Unit,
): Flow<ImageBatchResult> =
    map { file -> file.toPath() }.processImages(options, block)

/**
 * 성공적으로 writer가 선택된 이미지 결과만 저장합니다.
 */
fun Flow<ImageBatchResult>.writeImagesTo(
    outputDirectory: Path,
    options: ImageProcessingOptions = ImageProcessingOptions(),
    outputName: (source: Path) -> String = { source -> source.fileName.toString() },
): Flow<Path> =
    filterIsInstance<ImageBatchResult.WritableImage>()
        .mapParallel(options.parallelism) { result ->
            var output: Path? = null
            try {
                output = resolveOutputPath(outputDirectory, outputName(result.source))
                result.writeTo(output, options.ioDispatcher)
                output
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                handleWriteFailure(result, output, e, options)
            }
        }
        .filterNotNull()

/**
 * writer가 선택된 이미지 결과를 지정 경로에 저장합니다.
 */
suspend fun ImageBatchResult.WritableImage.writeTo(
    output: Path,
    ioDispatcher: CoroutineContext,
): Long =
    withContext(ioDispatcher) {
        output.parent?.let(Files::createDirectories)
        Files.newOutputStream(output).use { stream ->
            writer.write(image, stream)
        }
        Files.size(output)
    }

private suspend fun processOneImage(
    source: Path,
    dsl: ImageProcessingDsl,
    options: ImageProcessingOptions,
    limiter: PixelPermitLimiter,
): ImageBatchResult {
    try {
        val probedPixels = runStage(source, ImageBatchFailureStage.VALIDATION) {
            withContext(options.ioDispatcher) { probeImagePixelCount(source) }
        }
        probedPixels?.requireWithinMaxPixels(source, options.maxPixels)

        val permitPixels = probedPixels ?: options.maxPixels
        return limiter.withPermit(permitPixels) {
            val image = runStage(source, ImageBatchFailureStage.LOAD) {
                withContext(options.ioDispatcher) { immutableImageOf(source) }
            }
            image.pixelCount().requireWithinMaxPixels(source, options.maxPixels)

            val transformed = runStage(source, ImageBatchFailureStage.TRANSFORM) {
                withContext(options.transformDispatcher) { dsl.apply(image) }
            }
            transformed.toBatchResult(source, dsl.selectedWriter())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: ImageBatchException) {
        return handleFailure(e, options)
    } catch (e: Throwable) {
        return handleFailure(
            ImageBatchException(
                source = source,
                stage = ImageBatchFailureStage.TRANSFORM,
                message = "이미지 배치 처리에 실패했습니다: $source",
                cause = e,
            ),
            options,
        )
    }
}

private suspend inline fun <T> runStage(
    source: Path,
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
            message = "이미지 배치 처리 단계가 실패했습니다. source=$source, stage=$stage",
            cause = e,
        )
    }

private suspend fun handleFailure(
    exception: ImageBatchException,
    options: ImageProcessingOptions,
): ImageBatchResult.Failure {
    val failure = ImageBatchResult.Failure(
        source = exception.source,
        stage = exception.stage,
        output = exception.output,
        cause = exception.cause ?: exception,
    )

    if (!options.skipFailures) {
        throw exception
    }

    log.warn(exception) { "이미지 배치 실패를 건너뜁니다. source=${exception.source}, stage=${exception.stage}" }
    options.onFailure(failure)
    return failure
}

private suspend fun handleWriteFailure(
    result: ImageBatchResult.WritableImage,
    output: Path?,
    cause: Throwable,
    options: ImageProcessingOptions,
): Path? {
    val exception = ImageBatchException(
        source = result.source,
        stage = ImageBatchFailureStage.WRITE,
        output = output,
        message = "이미지 저장에 실패했습니다. source=${result.source}, output=$output",
        cause = cause,
    )
    val failure = ImageBatchResult.Failure(
        source = result.source,
        stage = ImageBatchFailureStage.WRITE,
        output = output,
        cause = cause,
    )

    if (!options.skipFailures) {
        throw exception
    }

    log.warn(exception) { "이미지 저장 실패를 건너뜁니다. source=${result.source}, output=$output" }
    options.onFailure(failure)
    return null
}

private fun resolveOutputPath(outputDirectory: Path, outputName: String): Path {
    require(outputName.isNotBlank()) { "outputName은 blank일 수 없습니다." }
    val base = outputDirectory.toAbsolutePath().normalize()
    val output = base.resolve(outputName).normalize()
    require(output.startsWith(base)) {
        "출력 경로가 outputDirectory를 벗어날 수 없습니다. outputDirectory=$outputDirectory, outputName=$outputName"
    }
    return output
}

private fun ImmutableImage.toBatchResult(
    source: Path,
    writer: SuspendImageWriter?,
): ImageBatchResult =
    if (writer == null) {
        ImageBatchResult.Image(source, this)
    } else {
        ImageBatchResult.WritableImage(source, this, writer)
    }

private fun ImmutableImage.pixelCount(): Long =
    width.toLong() * height.toLong()

private fun Long.requireWithinMaxPixels(source: Path, maxPixels: Long) {
    if (this > maxPixels) {
        throw ImageBatchException(
            source = source,
            stage = ImageBatchFailureStage.VALIDATION,
            message = "이미지 픽셀 수가 허용 한도를 초과했습니다. source=$source, pixels=$this, maxPixels=$maxPixels",
        )
    }
}
