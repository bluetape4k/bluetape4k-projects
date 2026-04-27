package io.bluetape4k.images.analysis

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.jpeg.JpegDirectory
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 이미지 파일에서 추출된 EXIF 메타데이터.
 *
 * 모든 필드는 nullable이다. EXIF 정보가 없거나 지원되지 않는 포맷의 경우 null이 된다.
 *
 * **주의**: [dateTimeOriginal]은 timezone 정보가 없다.
 * EXIF DateTimeOriginal 태그는 카메라 현지 시각(wall-clock)을 기록하며 timezone을 포함하지 않는다.
 * UTC 변환이 필요하면 GPS 좌표 또는 카메라 설정을 기반으로 호출자가 직접 수행해야 한다.
 *
 * **GPS 주의**: [gpsLatitude]와 [gpsLongitude]는 개인 위치 정보(PII)에 해당한다.
 * 외부 API에 노출하기 전 명시적 동의 또는 제거가 필요하다. [withoutGps]를 사용하라.
 */
data class ExifData(
    /** GPS 위도 (decimal degrees, 북위 양수/남위 음수) */
    val gpsLatitude: Double? = null,
    /** GPS 경도 (decimal degrees, 동경 양수/서경 음수) */
    val gpsLongitude: Double? = null,
    /** GPS 고도 (미터) */
    val gpsAltitude: Double? = null,
    /** 촬영 일시 (EXIF DateTimeOriginal — timezone 정보 없는 wall-clock) */
    val dateTimeOriginal: LocalDateTime? = null,
    /** 카메라 제조사 (e.g. "Canon") */
    val cameraMake: String? = null,
    /** 카메라 모델 (e.g. "EOS R5") */
    val cameraModel: String? = null,
    /** 렌즈 모델 */
    val lensModel: String? = null,
    /** ISO 감도 */
    val iso: Int? = null,
    /** 셔터 스피드 (e.g. "1/250") */
    val shutterSpeed: String? = null,
    /** 조리개 f-number (e.g. 2.8) */
    val aperture: Double? = null,
    /** 초점 거리 (mm) */
    val focalLength: Double? = null,
    /** 35mm 환산 초점 거리 */
    val focalLength35mm: Int? = null,
    /** EXIF 방향 (1..8) */
    val orientation: Int? = null,
    /** 이미지 너비 (px) */
    val width: Int? = null,
    /** 이미지 높이 (px) */
    val height: Int? = null,
    /** 플래시 발광 여부 */
    val flashFired: Boolean? = null,
    /** 화이트 밸런스 설명 */
    val whiteBalance: String? = null,
) {
    /** GPS 좌표가 존재하면 true */
    val hasGps: Boolean get() = gpsLatitude != null && gpsLongitude != null

    /**
     * GPS 좌표를 제거한 복사본을 반환한다 (PII 제거).
     */
    fun withoutGps(): ExifData = copy(
        gpsLatitude = null,
        gpsLongitude = null,
        gpsAltitude = null,
    )

    companion object {
        /** EXIF 정보가 없거나 파싱 실패 시 반환되는 빈 인스턴스 */
        val EMPTY = ExifData()
    }
}

private val log = KotlinLogging.logger(ExifData::class)

private val EXIF_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

/**
 * ByteArray에서 EXIF 메타데이터를 읽는다.
 *
 * @param bytes 이미지 바이트 배열. 50MB 초과 시 [IllegalArgumentException].
 * @return EXIF 데이터. EXIF 없거나 파싱 실패 시 [ExifData.EMPTY].
 */
fun readExif(bytes: ByteArray): ExifData {
    require(bytes.size <= 50 * 1024 * 1024) {
        "이미지 바이트 배열이 50MB를 초과합니다: ${bytes.size} bytes"
    }
    return try {
        ImageMetadataReader.readMetadata(ByteArrayInputStream(bytes)).toExifData()
    } catch (e: IOException) {
        log.warn(e) { "EXIF 읽기 I/O 오류 (ByteArray ${bytes.size} bytes)" }
        ExifData.EMPTY
    } catch (e: Exception) {
        log.debug(e) { "EXIF 파싱 실패 (ByteArray ${bytes.size} bytes)" }
        ExifData.EMPTY
    }
}

/**
 * [File]에서 EXIF 메타데이터를 읽는다.
 *
 * **주의**: 신뢰할 수 없는 경로를 직접 전달하지 말 것. 사용자 입력 경로는 정규화 필요.
 *
 * @return EXIF 데이터. EXIF 없거나 파싱 실패 시 [ExifData.EMPTY].
 */
fun File.readExif(): ExifData =
    try {
        ImageMetadataReader.readMetadata(this).toExifData()
    } catch (e: IOException) {
        log.warn(e) { "EXIF 읽기 I/O 오류: $absolutePath" }
        ExifData.EMPTY
    } catch (e: Exception) {
        log.debug(e) { "EXIF 파싱 실패: $absolutePath" }
        ExifData.EMPTY
    }

/**
 * [Path]에서 EXIF 메타데이터를 읽는다.
 *
 * jar/zip 내부 경로도 지원하기 위해 [Files.newInputStream]을 사용한다.
 *
 * @return EXIF 데이터. EXIF 없거나 파싱 실패 시 [ExifData.EMPTY].
 */
fun Path.readExif(): ExifData =
    try {
        Files.newInputStream(this).use { it.readExif() }
    } catch (e: IOException) {
        log.warn(e) { "EXIF 파일 열기 실패: $this" }
        ExifData.EMPTY
    }

/**
 * [InputStream]에서 EXIF 메타데이터를 읽는다.
 *
 * 스트림 close는 호출자가 관리한다.
 *
 * @return EXIF 데이터. EXIF 없거나 파싱 실패 시 [ExifData.EMPTY].
 */
fun InputStream.readExif(): ExifData =
    try {
        ImageMetadataReader.readMetadata(this).toExifData()
    } catch (e: IOException) {
        log.warn(e) { "EXIF 읽기 I/O 오류 (InputStream)" }
        ExifData.EMPTY
    } catch (e: Exception) {
        log.debug(e) { "EXIF 파싱 실패 (InputStream)" }
        ExifData.EMPTY
    }

/**
 * [File]에서 EXIF 메타데이터를 비동기로 읽는다.
 *
 * 파일 I/O를 포함하므로 [Dispatchers.IO]를 사용한다.
 */
suspend fun File.suspendReadExif(): ExifData =
    withContext(Dispatchers.IO) { readExif() }

/**
 * [Path]에서 EXIF 메타데이터를 비동기로 읽는다.
 *
 * 파일 I/O를 포함하므로 [Dispatchers.IO]를 사용한다.
 */
suspend fun Path.suspendReadExif(): ExifData =
    withContext(Dispatchers.IO) { readExif() }

// ─── 내부 변환 ───────────────────────────────────────────────────────────────

private inline fun <T> runCatchingDebug(tag: String, block: () -> T?): T? =
    runCatching(block).getOrElse { e ->
        log.debug(e) { "EXIF 필드 파싱 실패: $tag" }
        null
    }

private fun Metadata.toExifData(): ExifData {
    val ifd0 = getFirstDirectoryOfType(ExifIFD0Directory::class.java)
    val subIfd = getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
    val gps = getFirstDirectoryOfType(GpsDirectory::class.java)
    val jpeg = getFirstDirectoryOfType(JpegDirectory::class.java)

    val geoLocation = runCatchingDebug("gps.geoLocation") { gps?.geoLocation }

    return ExifData(
        gpsLatitude = geoLocation?.latitude,
        gpsLongitude = geoLocation?.longitude,
        gpsAltitude = runCatchingDebug("gpsAltitude") {
            gps?.getRational(GpsDirectory.TAG_ALTITUDE)?.toDouble()
        },
        dateTimeOriginal = runCatchingDebug("dateTimeOriginal") {
            subIfd?.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
                ?.let { LocalDateTime.parse(it.trim(), EXIF_DATE_FORMATTER) }
        },
        cameraMake = runCatchingDebug("cameraMake") { ifd0?.getString(ExifIFD0Directory.TAG_MAKE)?.trim() },
        cameraModel = runCatchingDebug("cameraModel") { ifd0?.getString(ExifIFD0Directory.TAG_MODEL)?.trim() },
        lensModel = runCatchingDebug("lensModel") {
            subIfd?.getString(ExifSubIFDDirectory.TAG_LENS_MODEL)?.trim()
        },
        iso = runCatchingDebug("iso") {
            subIfd?.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)
        },
        shutterSpeed = runCatchingDebug("shutterSpeed") {
            subIfd?.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)
                ?.let { "${it.numerator}/${it.denominator}" }
        },
        aperture = runCatchingDebug("aperture") {
            subIfd?.getRational(ExifSubIFDDirectory.TAG_FNUMBER)?.toDouble()
        },
        focalLength = runCatchingDebug("focalLength") {
            subIfd?.getRational(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)?.toDouble()
        },
        focalLength35mm = runCatchingDebug("focalLength35mm") {
            subIfd?.getInteger(ExifSubIFDDirectory.TAG_35MM_FILM_EQUIV_FOCAL_LENGTH)
        },
        orientation = runCatchingDebug("orientation") {
            ifd0?.getInteger(ExifIFD0Directory.TAG_ORIENTATION)
        },
        width = runCatchingDebug("width") {
            jpeg?.getInteger(JpegDirectory.TAG_IMAGE_WIDTH)
                ?: subIfd?.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH)
        },
        height = runCatchingDebug("height") {
            jpeg?.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT)
                ?: subIfd?.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT)
        },
        flashFired = runCatchingDebug("flashFired") {
            subIfd?.getInteger(ExifSubIFDDirectory.TAG_FLASH)?.let { it and 0x1 == 1 }
        },
        whiteBalance = runCatchingDebug("whiteBalance") {
            subIfd?.getDescription(ExifSubIFDDirectory.TAG_WHITE_BALANCE)?.trim()
        },
    )
}
