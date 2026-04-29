package io.bluetape4k.images.thumbnail

import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.batch.ImageBatchFailureStage
import io.bluetape4k.images.batch.ImageProcessingOptions
import io.bluetape4k.images.coroutines.SuspendJpegWriter
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO

class ThumbnailPipelineTest: AbstractImageTest() {

    @Test
    fun `thumbnail pipeline writes configured size`(
        tempFolder: TempFolder,
    ) = runTest(timeout = 30.seconds) {
        val source = tempFolder.copyResource(LANDSCAPE_JPG, SOURCE_IMAGE_NAME)
        val outputDir = tempFolder.createDirectory(OUTPUT_DIRECTORY_NAME).toPath()
        val pipeline = ThumbnailPipeline.builder()
            .outputDirectory(outputDir)
            .size(TEST_THUMB_WIDTH, TEST_THUMB_HEIGHT, TEST_THUMB_SUFFIX)
            .options(ImageProcessingOptions(parallelism = TEST_PARALLELISM))
            .build()

        val result = pipeline.process(flowOf(source)).single()

        result.status shouldBeInstanceOf ThumbnailStatus.Success::class
        Files.exists(result.output).shouldBeTrue()
        ImageIO.read(result.output.toFile()).width shouldBeEqualTo TEST_THUMB_WIDTH
        ImageIO.read(result.output.toFile()).height shouldBeEqualTo TEST_THUMB_HEIGHT
    }

    @Test
    fun `thumbnail pipeline rejects output path traversal`(
        tempFolder: TempFolder,
    ) = runTest(timeout = 30.seconds) {
        val source = tempFolder.copyResource(LANDSCAPE_JPG, SOURCE_IMAGE_NAME)
        val outputDir = tempFolder.createDirectory(OUTPUT_DIRECTORY_NAME).toPath()
        val failures = mutableListOf<ThumbnailResult>()
        val pipeline = ThumbnailPipeline.builder()
            .outputDirectory(outputDir)
            .size(TEST_THUMB_WIDTH, TEST_THUMB_HEIGHT, TEST_THUMB_SUFFIX)
            .outputName { _, _, _ -> PATH_TRAVERSAL_OUTPUT_NAME }
            .options(ImageProcessingOptions(parallelism = TEST_PARALLELISM, skipFailures = true))
            .onFailure { failures += it }
            .build()

        val results = pipeline.process(flowOf(source)).toList()

        results.single().status shouldBeInstanceOf ThumbnailStatus.Failure::class
        failures.single().stage shouldBeEqualTo ImageBatchFailureStage.VALIDATION
        Files.exists(outputDir.parent.resolve(ESCAPED_OUTPUT_NAME)) shouldBeEqualTo false
    }

    @Test
    fun `thumbnail format rejects blank and path separator extension`() {
        val blankExtension = { ThumbnailFormat(SuspendJpegWriter.Default, BLANK_EXTENSION) }
        val pathExtension = { ThumbnailFormat(SuspendJpegWriter.Default, PATH_EXTENSION) }

        blankExtension shouldThrow IllegalArgumentException::class
        pathExtension shouldThrow IllegalArgumentException::class
    }

    private fun TempFolder.copyResource(resourcePath: String, fileName: String) =
        createFile(fileName).toPath().also { target ->
            val input = Resourcex.getInputStream(resourcePath)
                ?: error("테스트 리소스를 찾을 수 없습니다: $resourcePath")
            input.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
        }

    private companion object {
        private const val SOURCE_IMAGE_NAME = "source.jpg"
        private const val OUTPUT_DIRECTORY_NAME = "thumbs"
        private const val ESCAPED_OUTPUT_NAME = "escape.jpg"
        private const val PATH_TRAVERSAL_OUTPUT_NAME = "../$ESCAPED_OUTPUT_NAME"
        private const val BLANK_EXTENSION = " "
        private const val PATH_EXTENSION = "../jpg"
        private const val TEST_PARALLELISM = 1
        private const val TEST_THUMB_WIDTH = 80
        private const val TEST_THUMB_HEIGHT = 60
        private const val TEST_THUMB_SUFFIX = "small"
    }
}
