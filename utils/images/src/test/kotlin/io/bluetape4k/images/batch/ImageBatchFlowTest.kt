package io.bluetape4k.images.batch

import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

class ImageBatchFlowTest: AbstractImageTest() {

    companion object: KLoggingChannel() {
        private const val SOURCE_IMAGE_NAME = "source.jpg"
        private const val OUTPUT_IMAGE_NAME = "out.jpg"
        private const val ESCAPED_OUTPUT_NAME = "escape.jpg"
        private const val PATH_TRAVERSAL_OUTPUT_NAME = "../$ESCAPED_OUTPUT_NAME"
        private const val BROKEN_IMAGE_NAME = "broken.txt"
        private const val BROKEN_IMAGE_TEXT = "not an image"
        private const val TEST_PARALLELISM = 1
        private const val TEST_THUMB_WIDTH = 96
        private const val TEST_THUMB_HEIGHT = 64
        private const val TEST_JPEG_QUALITY = 85
        private const val TEST_IMAGE_FORMAT = "jpg"
        private const val PERFORMANCE_SOURCE_WIDTH = 48
        private const val PERFORMANCE_SOURCE_HEIGHT = 48
        private const val PERFORMANCE_SAMPLE_WIDTH = 32
        private const val PERFORMANCE_SAMPLE_HEIGHT = 32
    }

    @Test
    fun `processImages applies transform dispatcher path and writes with selected writer`(
        tempFolder: TempFolder,
    ) = runTest(timeout = 30.seconds) {
        val source = tempFolder.copyResource(CAFE_JPG, SOURCE_IMAGE_NAME)
        val output = tempFolder.root.toPath().resolve(OUTPUT_IMAGE_NAME)

        val result = flowOf(source)
            .processImages(ImageProcessingOptions(parallelism = TEST_PARALLELISM)) {
                resize(TEST_THUMB_WIDTH, TEST_THUMB_HEIGHT)
                toJpeg(quality = TEST_JPEG_QUALITY)
            }
            .single()

        result shouldBeInstanceOf ImageBatchResult.WritableImage::class
        val writable = result as ImageBatchResult.WritableImage
        writable.image.width shouldBeEqualTo TEST_THUMB_WIDTH
        writable.image.height shouldBeEqualTo TEST_THUMB_HEIGHT

        val written = writable.writeTo(output, ImageProcessingOptions().ioDispatcher)

        written shouldBeGreaterThan 0L
        ImageIO.read(output.toFile()).width shouldBeEqualTo TEST_THUMB_WIDTH
    }

    @Test
    fun `processImages emits failure and invokes callback when skipFailures is true`(
        tempFolder: TempFolder,
    ) = runTest(timeout = 30.seconds) {
        val source = tempFolder.createFile(BROKEN_IMAGE_NAME).toPath()
        Files.writeString(source, BROKEN_IMAGE_TEXT)
        val failures = mutableListOf<ImageBatchResult.Failure>()

        val results = flowOf(source)
            .processImages(
                ImageProcessingOptions(
                    parallelism = TEST_PARALLELISM,
                    skipFailures = true,
                    onFailure = { failures += it },
                )
            ) {
                resize(TEST_THUMB_WIDTH, TEST_THUMB_HEIGHT)
            }
            .toList()

        results.single() shouldBeInstanceOf ImageBatchResult.Failure::class
        failures.single().stage shouldBeEqualTo ImageBatchFailureStage.LOAD
    }

    @Test
    fun `writeImagesTo emits write failure callback when skipFailures is true`(
        tempFolder: TempFolder,
    ) = runTest(timeout = 30.seconds) {
        val source = tempFolder.copyResource(CAFE_JPG, SOURCE_IMAGE_NAME)
        val blockedOutput = tempFolder.createDirectory(OUTPUT_IMAGE_NAME).toPath()
        val failures = mutableListOf<ImageBatchResult.Failure>()
        val options = ImageProcessingOptions(
            parallelism = TEST_PARALLELISM,
            skipFailures = true,
            onFailure = { failures += it },
        )

        val outputs = flowOf(source)
            .processImages(options) {
                resize(TEST_THUMB_WIDTH, TEST_THUMB_HEIGHT)
                toJpeg(quality = TEST_JPEG_QUALITY)
            }
            .writeImagesTo(
                outputDirectory = tempFolder.root.toPath(),
                options = options,
                outputName = { blockedOutput.fileName.toString() },
            )
            .toList()

        outputs.size shouldBeEqualTo 0
        failures.single().stage shouldBeEqualTo ImageBatchFailureStage.WRITE
        failures.single().output shouldBeEqualTo blockedOutput
    }

    @Test
    fun `writeImagesTo rejects output path traversal`(
        tempFolder: TempFolder,
    ) = runTest(timeout = 30.seconds) {
        val source = tempFolder.copyResource(CAFE_JPG, SOURCE_IMAGE_NAME)
        val failures = mutableListOf<ImageBatchResult.Failure>()
        val options = ImageProcessingOptions(
            parallelism = TEST_PARALLELISM,
            skipFailures = true,
            onFailure = { failures += it },
        )

        val outputs = flowOf(source)
            .processImages(options) {
                resize(TEST_THUMB_WIDTH, TEST_THUMB_HEIGHT)
                toJpeg(quality = TEST_JPEG_QUALITY)
            }
            .writeImagesTo(
                outputDirectory = tempFolder.root.toPath(),
                options = options,
                outputName = { PATH_TRAVERSAL_OUTPUT_NAME },
            )
            .toList()

        outputs.size shouldBeEqualTo 0
        failures.single().stage shouldBeEqualTo ImageBatchFailureStage.WRITE
        Files.exists(tempFolder.root.toPath().parent.resolve(ESCAPED_OUTPUT_NAME)) shouldBeEqualTo false
    }

    @Test
    fun `batch defaults are named constants`() {
        (DEFAULT_MAX_PIXELS > 0L).shouldBeTrue()
        (DEFAULT_MAX_IN_FLIGHT_PIXELS >= DEFAULT_MAX_PIXELS).shouldBeTrue()
        (DEFAULT_MAX_TILE_COUNT > 0).shouldBeTrue()
        JPEG_QUALITY_MIN shouldBeEqualTo 0
        JPEG_QUALITY_MAX shouldBeEqualTo 100
        PERFORMANCE_SAMPLE_IMAGE_COUNT shouldBeEqualTo 100
        defaultImageBatchParallelism() shouldBeGreaterThan 0
    }

    @Test
    fun `largeJobs option raises pixel limits explicitly`() {
        val options = ImageProcessingOptions.largeJobs(parallelism = TEST_PARALLELISM)

        options.maxPixels shouldBeEqualTo LARGE_JOB_MAX_PIXELS
        options.maxInFlightPixels shouldBeEqualTo LARGE_JOB_MAX_IN_FLIGHT_PIXELS
        (options.maxPixels > DEFAULT_MAX_PIXELS).shouldBeTrue()
        (options.maxInFlightPixels > DEFAULT_MAX_IN_FLIGHT_PIXELS).shouldBeTrue()
    }

    @Test
    fun `hundred image batch performance sample is logged without threshold gating`(
        tempFolder: TempFolder,
    ) = runTest(timeout = 60.seconds) {
        val source = tempFolder.createTinyImage(SOURCE_IMAGE_NAME)
        val sources = List(PERFORMANCE_SAMPLE_IMAGE_COUNT) { source }
        val options = ImageProcessingOptions(parallelism = TEST_PARALLELISM)
        lateinit var results: List<ImageBatchResult>

        val elapsedMillis = measureTimeMillis {
            results = sources.asFlow()
                .processImages(options) {
                    resize(PERFORMANCE_SAMPLE_WIDTH, PERFORMANCE_SAMPLE_HEIGHT)
                    toJpeg()
                }
                .toList()
        }

        results.size shouldBeEqualTo PERFORMANCE_SAMPLE_IMAGE_COUNT
        log.info { "$PERFORMANCE_SAMPLE_IMAGE_COUNT image batch performance sample completed in ${elapsedMillis}ms" }
    }

    private fun TempFolder.copyResource(resourcePath: String, fileName: String) =
        createFile(fileName).toPath().also { target ->
            val input = Resourcex.getInputStream(resourcePath)
                ?: error("테스트 리소스를 찾을 수 없습니다: $resourcePath")
            input.use { Files.copy(it, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
        }

    private fun TempFolder.createTinyImage(fileName: String) =
        createFile(fileName).toPath().also { target ->
            val image = BufferedImage(PERFORMANCE_SOURCE_WIDTH, PERFORMANCE_SOURCE_HEIGHT, BufferedImage.TYPE_INT_RGB)
            val graphics = image.createGraphics()
            try {
                graphics.color = Color.CYAN
                graphics.fillRect(0, 0, image.width, image.height)
            } finally {
                graphics.dispose()
            }
            ImageIO.write(image, TEST_IMAGE_FORMAT, target.toFile())
        }

}
