package io.bluetape4k.images.vips

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireInRange
import java.io.InvalidObjectException
import java.io.Serializable

/**
 * libvips 이미지 인코딩 옵션.
 *
 * @param quality JPEG/WebP 손실 압축 품질 (0–100). PNG 인코딩 시 무시됩니다.
 * @param effort 인코딩 노력 수준 (1–9). 높을수록 느리지만 파일 크기가 작아집니다.
 * @param lossless WebP 무손실 모드 활성화 여부.
 * @param stripMetadata 인코딩 결과에서 EXIF/XMP 메타데이터를 제거할지 여부.
 */
data class VipsEncodeOptions(
    val quality: Int = 85,
    val effort: Int = 4,
    val lossless: Boolean = false,
    val stripMetadata: Boolean = true,
) : Serializable {

    init {
        quality.requireInRange(0, 100, "quality")
        effort.requireInRange(1, 9, "effort")
    }

    companion object : KLogging() {
        @JvmStatic
        private val serialVersionUID: Long = 1L

        /** 기본 옵션 (quality=85, effort=4) */
        val Default = VipsEncodeOptions()

        /** 고품질 옵션 (quality=95, effort=6) */
        val HighQuality = VipsEncodeOptions(quality = 95, effort = 6)

        /** 저대역폭 옵션 (quality=60, effort=3) */
        val LowBandwidth = VipsEncodeOptions(quality = 60, effort = 3)
    }

    /**
     * 역직렬화 유효성 검증.
     * 역직렬화 스트림에서 범위 위반이 감지되면 [InvalidObjectException]을 던집니다.
     */
    @Suppress("unused")
    private fun readResolve(): Any {
        if (quality !in 0..100) throw InvalidObjectException("quality out of range: $quality")
        if (effort !in 1..9) throw InvalidObjectException("effort out of range: $effort")
        return this
    }
}
