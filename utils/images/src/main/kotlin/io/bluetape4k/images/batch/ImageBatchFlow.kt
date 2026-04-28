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
 *
 * DSL 블록으로 변환 파이프라인을 선언하고, [ImageProcessingOptions]로 병렬도·픽셀 한도·
 * 실패 처리 방침을 제어합니다. 처리 결과는 [ImageBatchResult]의 sealed 계층으로 반환됩니다.
 *
 * ```kotlin
 * val outputDir = Path.of("/tmp/output")
 * val options = ImageProcessingOptions(parallelism = 4, skipFailures = true)
 *
 * val writtenPaths: List<Path> = flowOf(Path.of("/images/photo.png"))
 *     .processImages(options) {
 *         resize(1280, 720)
 *         toJpeg(quality = 85)
 *     }
 *     .writeImagesTo(outputDir)
 *     .toList()
 * ```
 *
 * @param options 배치 처리 옵션 (병렬도, 픽셀 한도, 실패 정책 등)
 * @param block 이미지 변환 단계를 선언하는 DSL 람다
 * @return 처리 결과([ImageBatchResult]) 스트림
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
 *
 * 내부적으로 [File.toPath]를 통해 [processImages]에 위임합니다.
 * [java.io.File] 기반 파일 목록에서 바로 배치 처리를 시작할 때 사용합니다.
 *
 * ```kotlin
 * val inputFiles = listOf(File("/images/a.jpg"), File("/images/b.png"))
 * val outputDir = Path.of("/tmp/output")
 *
 * val writtenPaths: List<Path> = inputFiles.asFlow()
 *     .processImageFiles {
 *         fit(800, 600)
 *         toJpeg(quality = 90)
 *     }
 *     .writeImagesTo(outputDir)
 *     .toList()
 * ```
 *
 * @param options 배치 처리 옵션 (병렬도, 픽셀 한도, 실패 정책 등)
 * @param block 이미지 변환 단계를 선언하는 DSL 람다
 * @return 처리 결과([ImageBatchResult]) 스트림
 */
fun Flow<File>.processImageFiles(
    options: ImageProcessingOptions = ImageProcessingOptions(),
    block: ImageProcessingDsl.() -> Unit,
): Flow<ImageBatchResult> =
    map { file -> file.toPath() }.processImages(options, block)

/**
 * 성공적으로 writer가 선택된 이미지 결과만 [outputDirectory]에 저장합니다.
 *
 * [ImageBatchResult.WritableImage] 타입만 필터링하여 저장하므로, writer를 지정하지 않은
 * [ImageBatchResult.Image] 결과와 실패([ImageBatchResult.Failure]) 결과는 조용히 건너뜁니다.
 * 저장 성공 시 출력 파일의 [Path]를 반환합니다.
 *
 * ```kotlin
 * val outputDir = Path.of("/tmp/thumbnails")
 *
 * val savedPaths: List<Path> = flowOf(Path.of("/images/photo.jpg"))
 *     .processImages {
 *         resize(320, 240)
 *         toJpeg(quality = 75)
 *     }
 *     .writeImagesTo(outputDir) { source ->
 *         // 출력 파일명을 원본 이름에서 파생
 *         "thumb_${source.fileName}"
 *     }
 *     .toList()
 * ```
 *
 * @param outputDirectory 이미지를 저장할 출력 디렉터리 (없으면 자동 생성)
 * @param options 병렬도·IO 디스패처 설정에 사용하는 처리 옵션
 * @param outputName 원본 [Path]를 받아 출력 파일명(디렉터리 제외)을 반환하는 함수
 * @return 저장 완료된 파일 경로 스트림
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
