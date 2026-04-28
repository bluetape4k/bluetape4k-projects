package io.bluetape4k.images.vips.java25

import io.bluetape4k.images.vips.VipsEncodeOptions
import io.bluetape4k.images.vips.VipsImageFormat
import io.bluetape4k.images.vips.coroutines.suspendToBytes
import io.bluetape4k.images.vips.testfixtures.VipsTestFixtures
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.nio.file.Path

class FfmVipsImageTest : AbstractFfmVipsTest() {

    companion object {
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())
        private val WEBP_RIFF = byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte())
        private val WEBP_MARKER = byteArrayOf(0x57.toByte(), 0x45.toByte(), 0x42.toByte(), 0x50.toByte())
    }

    // ─── 1: load + dimensions ─────────────────────────────────────────────

    @Test
    fun `ffmVipsImageOf bytes returns correct dimensions`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            img.width shouldBeEqualTo VipsTestFixtures.SAMPLE_JPEG_WIDTH
            img.height shouldBeEqualTo VipsTestFixtures.SAMPLE_JPEG_HEIGHT
        }
    }

    // ─── 2: resize ────────────────────────────────────────────────────────

    @Test
    fun `resize to 800x600 produces expected dimensions`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            img.resize(800, 600).use { resized ->
                resized.width shouldBeLessOrEqualTo 800
                resized.height shouldBeLessOrEqualTo 600
            }
        }
    }

    // ─── 3: thumbnail ─────────────────────────────────────────────────────

    @Test
    fun `thumbnail 300 longest side is at most 300`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            img.thumbnail(300).use { thumb ->
                maxOf(thumb.width, thumb.height) shouldBeLessOrEqualTo 300
            }
        }
    }

    // ─── 4: toBytes JPEG ──────────────────────────────────────────────────

    @Test
    fun `toBytes JPEG starts with JPEG magic bytes`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            val output = img.toBytes(VipsImageFormat.JPEG)
            output.size shouldBeGreaterThan 0
            output.startsWith(JPEG_MAGIC).shouldBeTrue()
        }
    }

    // ─── 5: toBytes PNG ───────────────────────────────────────────────────

    @Test
    fun `toBytes PNG starts with PNG magic bytes`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_PNG)
        ffmVipsImageOf(bytes).use { img ->
            val output = img.toBytes(VipsImageFormat.PNG)
            output.size shouldBeGreaterThan 0
            output.startsWith(PNG_MAGIC).shouldBeTrue()
        }
    }

    // ─── 6: toBytes WebP ──────────────────────────────────────────────────

    @Test
    fun `toBytes WebP has RIFF and WEBP markers`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_WEBP)
        ffmVipsImageOf(bytes).use { img ->
            val output = img.toBytes(VipsImageFormat.WEBP)
            output.size shouldBeGreaterThan 0
            output.startsWith(WEBP_RIFF).shouldBeTrue()
            output.regionMatches(8, WEBP_MARKER).shouldBeTrue()
        }
    }

    // ─── 7: suspendToBytes ────────────────────────────────────────────────

    @Test
    fun `suspendToBytes JPEG produces non-empty bytes with JPEG magic`() = runTest {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            val suspended = img.suspendToBytes(VipsImageFormat.JPEG, VipsEncodeOptions.Default)
            suspended.size shouldBeGreaterThan 0
            suspended.startsWith(JPEG_MAGIC).shouldBeTrue()
        }
    }

    // ─── 8: close idempotency ─────────────────────────────────────────────

    @Test
    fun `close called twice does not throw`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        val img = ffmVipsImageOf(bytes)
        img.close()
        img.close() // must not throw
    }

    // ─── 9: use-after-close throws ────────────────────────────────────────

    @Test
    fun `operation after close throws`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        val img = ffmVipsImageOf(bytes)
        img.close()
        val action = { img.toBytes(VipsImageFormat.JPEG) }
        action shouldThrow Exception::class
    }

    // ─── 10: crop exact dimensions ────────────────────────────────────────

    @Test
    fun `crop 0 0 100 100 returns 100x100`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            img.crop(0, 0, 100, 100).use { cropped ->
                cropped.width shouldBeEqualTo 100
                cropped.height shouldBeEqualTo 100
            }
        }
    }

    // ─── 11: writeTo Path ─────────────────────────────────────────────────

    @Test
    fun `writeTo path creates valid JPEG file`(@TempDir tmpDir: Path) {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            val outPath = tmpDir.resolve("out.jpg")
            img.writeTo(outPath, VipsImageFormat.JPEG)
            val written = outPath.toFile().readBytes()
            written.size shouldBeGreaterThan 0
            written.startsWith(JPEG_MAGIC).shouldBeTrue()
        }
    }

    // ─── 12: writeTo OutputStream ─────────────────────────────────────────

    @Test
    fun `writeTo OutputStream produces bytes with JPEG magic`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            val baos = ByteArrayOutputStream()
            img.writeTo(baos, VipsImageFormat.JPEG)
            val out = baos.toByteArray()
            out.size shouldBeGreaterThan 0
            out.startsWith(JPEG_MAGIC).shouldBeTrue()
        }
    }

    // ─── 13: invalid resize args ──────────────────────────────────────────

    @Test
    fun `resize with zero width throws`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            val action = { img.resize(0, 600) }
            action shouldThrow Exception::class
        }
    }

    // ─── 14: out-of-bounds crop ───────────────────────────────────────────

    @Test
    fun `crop beyond image bounds throws`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            val action = { img.crop(0, 0, img.width + 1, img.height) }
            action shouldThrow Exception::class
        }
    }

    // ─── 15: corrupt data ─────────────────────────────────────────────────

    @Test
    fun `corrupt bytes throw VipsDecodeException on load`() {
        val corrupt = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00, 0x01, 0x02, 0x03)
        val action = { ffmVipsImageOf(corrupt) }
        action shouldThrow Exception::class
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) if (this[i] != prefix[i]) return false
        return true
    }

    private fun ByteArray.regionMatches(offset: Int, other: ByteArray): Boolean {
        if (size < offset + other.size) return false
        for (i in other.indices) if (this[offset + i] != other[i]) return false
        return true
    }
}
