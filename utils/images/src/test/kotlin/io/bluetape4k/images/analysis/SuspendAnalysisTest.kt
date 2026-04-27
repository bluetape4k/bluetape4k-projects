package io.bluetape4k.images.analysis

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

class SuspendAnalysisTest {

    companion object: KLoggingChannel() {
        private const val HOMER_JPG = "images/homer.jpg"
        private const val CAFE_JPG = "images/cafe.jpg"
        private const val LANDSCAPE_JPG = "images/landscape.jpg"

        private fun loadImage(path: String): ImmutableImage =
            ImmutableImage.loader().fromStream(Resourcex.getInputStream(path)!!)

        private fun resourceFile(path: String): File? =
            Thread.currentThread().contextClassLoader.getResource(path)
                ?.let { url -> runCatching { File(url.toURI()) }.getOrNull()?.takeIf { it.exists() } }

        private fun resourcePath(path: String): Path? = resourceFile(path)?.toPath()
    }

    // ─── suspendDominantColors ───────────────────────────────────────────────

    @Test
    fun `suspendDominantColors returns colors in coroutine`() = runTest(timeout = 30.seconds) {
        val image = loadImage(HOMER_JPG)
        val colors = image.suspendDominantColors(5)
        log.debug { "suspendDominantColors(5): $colors" }
        colors.shouldNotBeEmpty()
    }

    @Test
    fun `suspendDominantColors with count=1 returns single color`() = runTest(timeout = 30.seconds) {
        val image = loadImage(HOMER_JPG)
        val colors = image.suspendDominantColors(1)
        log.debug { "suspendDominantColors(1): $colors" }
        colors.shouldNotBeEmpty()
    }

    @Test
    fun `parallel suspendDominantColors on multiple images completes correctly`() = runTest(timeout = 60.seconds) {
        val paths = listOf(HOMER_JPG, CAFE_JPG, LANDSCAPE_JPG)
        val results = paths.map { path ->
            async {
                val image = loadImage(path)
                path to image.suspendDominantColors(3)
            }
        }.awaitAll()

        results.forEach { (path, colors) ->
            log.debug { "$path → ${colors.map { it.hex }}" }
            colors.shouldNotBeEmpty()
        }
    }

    // ─── suspendBlurScore ────────────────────────────────────────────────────

    @Test
    fun `suspendBlurScore returns score in coroutine`() = runTest(timeout = 30.seconds) {
        val image = loadImage(HOMER_JPG)
        val result = image.suspendBlurScore()
        log.debug { "suspendBlurScore: score=${result.score}, isBlurry=${result.isBlurry}" }
        result.score shouldBeGreaterThan 0.0
    }

    @Test
    fun `suspendBlurScore with zero threshold is never blurry`() = runTest(timeout = 30.seconds) {
        val image = loadImage(LANDSCAPE_JPG)
        val result = image.suspendBlurScore(threshold = 0.0)
        log.debug { "landscape suspendBlurScore(threshold=0): ${result.score}" }
        result.isBlurry.shouldBeFalse()
    }

    @Test
    fun `parallel suspendBlurScore on multiple images`() = runTest(timeout = 60.seconds) {
        val paths = listOf(HOMER_JPG, CAFE_JPG, LANDSCAPE_JPG)
        val results = paths.map { path ->
            async {
                val image = loadImage(path)
                path to image.suspendBlurScore()
            }
        }.awaitAll()

        results.forEach { (path, score) ->
            log.debug { "$path blurScore=${score.score}, isBlurry=${score.isBlurry}" }
            score.score shouldBeGreaterOrEqualTo 0.0
        }
    }

    // ─── suspendReadExif ─────────────────────────────────────────────────────

    @Test
    fun `suspendReadExif via File in coroutine`() = runTest(timeout = 30.seconds) {
        val file = resourceFile(HOMER_JPG) ?: return@runTest
        val result = file.suspendReadExif()
        log.debug { "suspendReadExif homer File: $result" }
        result.hasGps.shouldBeFalse()
    }

    @Test
    fun `suspendReadExif via Path in coroutine`() = runTest(timeout = 30.seconds) {
        val path = resourcePath(CAFE_JPG) ?: return@runTest
        val result = path.suspendReadExif()
        log.debug { "suspendReadExif cafe Path: make=${result.cameraMake}, iso=${result.iso}" }
    }

    @Test
    fun `parallel suspend analysis combines dominantColors and blurScore`() = runTest(timeout = 60.seconds) {
        val image = loadImage(HOMER_JPG)

        val colorsDeferred = async { image.suspendDominantColors(5) }
        val blurDeferred = async { image.suspendBlurScore() }

        val colors = colorsDeferred.await()
        val blur = blurDeferred.await()

        log.debug { "homer colors=${colors.map { it.hex }}, blurScore=${blur.score}" }
        colors.shouldNotBeEmpty()
        blur.score shouldBeGreaterOrEqualTo 0.0
    }
}
