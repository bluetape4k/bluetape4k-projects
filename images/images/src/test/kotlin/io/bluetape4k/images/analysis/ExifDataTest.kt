package io.bluetape4k.images.analysis

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO

class ExifDataTest {

    companion object: KLoggingChannel() {
        private const val HOMER_JPG = "images/homer.jpg"
        private const val CAFE_JPG = "images/cafe.jpg"

        /** 프로그래밍으로 생성한 JPEG — EXIF 없음. */
        fun noExifJpegBytes(width: Int = 10, height: Int = 10): ByteArray {
            val buf = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
            val gfx = buf.createGraphics()
            gfx.color = java.awt.Color(100, 150, 200)
            gfx.fillRect(0, 0, width, height)
            gfx.dispose()
            val baos = ByteArrayOutputStream()
            ImageIO.write(buf, "jpg", baos)
            return baos.toByteArray()
        }

        private fun resourceFile(path: String): File? =
            Thread.currentThread().contextClassLoader.getResource(path)
                ?.let { url ->
                    runCatching { File(url.toURI()) }.getOrNull()?.takeIf { it.exists() }
                }

        private fun resourcePath(path: String): Path? = resourceFile(path)?.toPath()
    }

    // ─── readExif(ByteArray) ─────────────────────────────────────────────────

    @Test
    fun `readExif on empty byte array returns EMPTY`() {
        val result = readExif(ByteArray(0))
        result shouldBeEqualTo ExifData.EMPTY
    }

    @Test
    fun `readExif on invalid bytes returns EMPTY`() {
        val result = readExif(ByteArray(100) { 0xFF.toByte() })
        result shouldBeEqualTo ExifData.EMPTY
    }

    @Test
    fun `readExif on programmatic no-exif jpeg has no GPS`() {
        val bytes = noExifJpegBytes()
        val result = readExif(bytes)
        log.debug { "no-exif readExif: $result" }
        result.hasGps.shouldBeFalse()
        result.gpsLatitude.shouldBeNull()
        result.gpsLongitude.shouldBeNull()
    }

    @Test
    fun `readExif 50MB guard throws`() {
        val oversized = ByteArray(50 * 1024 * 1024 + 1)
        invoking {
            readExif(oversized)
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `readExif exactly 50MB does not throw`() {
        // 유효한 JPEG가 아니므로 파싱 실패 → EMPTY 반환 (예외 아님)
        val maxSized = ByteArray(50 * 1024 * 1024)
        val result = readExif(maxSized)
        result shouldBeEqualTo ExifData.EMPTY
    }

    @Test
    fun `readExif on real photo bytes succeeds`() {
        val bytes = Resourcex.getInputStream(HOMER_JPG)!!.use { it.readBytes() }
        val result = readExif(bytes)
        log.debug { "homer readExif via bytes: $result" }
        // 예외 없이 ExifData를 반환해야 한다
        result.hasGps.shouldBeFalse()  // homer는 만화 이미지 → GPS 없음
    }

    // ─── File.readExif() ─────────────────────────────────────────────────────

    @Test
    fun `File readExif on nonexistent file returns EMPTY`() {
        val file = File("/tmp/does-not-exist-99999.jpg")
        val result = file.readExif()
        result shouldBeEqualTo ExifData.EMPTY
    }

    @Test
    fun `File readExif on real photo file succeeds`() {
        val file = resourceFile(HOMER_JPG) ?: return  // classpath 파일 없으면 skip
        val result = file.readExif()
        log.debug { "homer File.readExif: $result" }
        result.hasGps.shouldBeFalse()
    }

    @Test
    fun `File readExif on cafe succeeds`() {
        val file = resourceFile(CAFE_JPG) ?: return
        val result = file.readExif()
        log.debug { "cafe File.readExif: make=${result.cameraMake}, model=${result.cameraModel}, iso=${result.iso}" }
        // 카페 이미지는 EXIF가 있을 수도 없을 수도 있으므로 예외만 없으면 통과
    }

    // ─── Path.readExif() ─────────────────────────────────────────────────────

    @Test
    fun `Path readExif on real photo file`() {
        val path = resourcePath(HOMER_JPG) ?: return
        val result = path.readExif()
        log.debug { "homer Path.readExif: $result" }
        result.hasGps.shouldBeFalse()
    }

    // ─── InputStream.readExif() ─────────────────────────────────────────────

    @Test
    fun `InputStream readExif on programmatic jpeg`() {
        val bytes = noExifJpegBytes()
        val result = ByteArrayInputStream(bytes).readExif()
        log.debug { "no-exif InputStream.readExif: $result" }
        result.hasGps.shouldBeFalse()
    }

    @Test
    fun `InputStream readExif on real photo`() {
        val result = Resourcex.getInputStream(HOMER_JPG)!!.readExif()
        log.debug { "homer InputStream.readExif: $result" }
        result.hasGps.shouldBeFalse()
    }

    // ─── ExifData model ─────────────────────────────────────────────────────

    @Test
    fun `hasGps is false when both lat and lon are null`() {
        val data = ExifData(gpsLatitude = null, gpsLongitude = null)
        data.hasGps.shouldBeFalse()
    }

    @Test
    fun `hasGps is false when only latitude is set`() {
        val data = ExifData(gpsLatitude = 37.5, gpsLongitude = null)
        data.hasGps.shouldBeFalse()
    }

    @Test
    fun `hasGps is true when both lat and lon are set`() {
        val data = ExifData(gpsLatitude = 37.5, gpsLongitude = 127.0)
        data.hasGps.shouldBeTrue()
    }

    @Test
    fun `withoutGps removes all GPS fields`() {
        val data = ExifData(
            gpsLatitude = 37.5665,
            gpsLongitude = 126.9780,
            gpsAltitude = 38.0,
            cameraMake = "Canon",
        )
        data.hasGps.shouldBeTrue()

        val stripped = data.withoutGps()
        stripped.gpsLatitude.shouldBeNull()
        stripped.gpsLongitude.shouldBeNull()
        stripped.gpsAltitude.shouldBeNull()
        stripped.cameraMake shouldBeEqualTo "Canon"  // 다른 필드는 유지
    }

    @Test
    fun `EMPTY instance has all null fields`() {
        val empty = ExifData.EMPTY
        empty.gpsLatitude.shouldBeNull()
        empty.gpsLongitude.shouldBeNull()
        empty.dateTimeOriginal.shouldBeNull()
        empty.cameraMake.shouldBeNull()
        empty.iso.shouldBeNull()
        empty.hasGps.shouldBeFalse()
    }

    @Test
    fun `ExifData data class equality works`() {
        val a = ExifData(cameraMake = "Canon", iso = 100)
        val b = ExifData(cameraMake = "Canon", iso = 100)
        val c = ExifData(cameraMake = "Nikon", iso = 200)
        (a == b).shouldBeTrue()
        (a == c).shouldBeFalse()
    }

    @Test
    fun `ExifData copy preserves non-overridden fields`() {
        val original = ExifData(
            cameraMake = "Sony",
            iso = 400,
            aperture = 2.8,
        )
        val modified = original.copy(iso = 800)
        modified.cameraMake shouldBeEqualTo "Sony"
        modified.iso shouldBeEqualTo 800
        modified.aperture shouldBeEqualTo 2.8
    }
}
