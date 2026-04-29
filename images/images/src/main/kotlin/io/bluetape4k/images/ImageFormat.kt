package io.bluetape4k.images

import io.bluetape4k.images.ImageFormat.Companion.parse


/**
 * 지원하는 이미지 포맷 열거형입니다.
 *
 * ## 동작/계약
 * - `GIF`, `JPG`, `PNG`, `WEBP`, `TIFF`, `SVG` 포맷은 Java ImageIO 기반 writer로 쓸 수 있습니다.
 * - `AVIF`, `HEIC`는 인터페이스 정의만 제공하며 구현은 별도 모듈에 위임됩니다.
 * - [parse]는 대소문자를 무시하고 매칭하며, 공백/미지원 문자열은 `null`을 반환합니다.
 * - [isWritableByImageIO]가 `false`인 포맷은 Java ImageIO 기반 writer로 직접 쓸 수 없습니다.
 *
 * ```kotlin
 * val png = ImageFormat.parse("png")
 * // png == ImageFormat.PNG
 * val tiff = ImageFormat.parse("tiff")
 * // tiff == ImageFormat.TIFF
 * val avif = ImageFormat.AVIF
 * // avif.isWritableByImageIO() == false
 * ```
 */
enum class ImageFormat(val ioName: String) {
    GIF("gif"),
    JPG("jpeg"),
    PNG("png"),
    WEBP("webp"),
    TIFF("tiff"),
    SVG("svg"),
    AVIF("avif"),
    HEIC("heic");

    companion object {
        /**
         * 문자열을 [ImageFormat]으로 파싱합니다.
         *
         * ## 동작/계약
         * - 앞뒤 공백을 제거한 뒤 enum 이름과 대소문자 무시 비교합니다.
         * - 매칭 실패 시 `null`을 반환합니다.
         *
         * ```kotlin
         * val png = ImageFormat.parse("PNG")
         * // png == ImageFormat.PNG
         * val jpg = ImageFormat.parse("jpg")
         * // jpg == ImageFormat.JPG
         * val tiff = ImageFormat.parse("TIFF")
         * // tiff == ImageFormat.TIFF
         * ```
         */
        @JvmStatic
        fun parse(formatName: String): ImageFormat? {
            val normalized = formatName.trim()
            if (normalized.isEmpty()) return null
            return entries.find { it.name.equals(normalized, ignoreCase = true) }
        }

        internal val NON_IMAGEIO_WRITABLE: Set<ImageFormat> = setOf(SVG, AVIF, HEIC)
    }
}

/**
 * 이 포맷이 Java ImageIO 기반 writer로 직접 쓸 수 있는지 여부를 반환합니다.
 *
 * ## 동작/계약
 * - `SVG`, `AVIF`, `HEIC`는 `false`를 반환합니다.
 * - `GIF`, `JPG`, `PNG`, `WEBP`, `TIFF`는 `true`를 반환합니다.
 *
 * ```kotlin
 * ImageFormat.TIFF.isWritableByImageIO() // true
 * ImageFormat.SVG.isWritableByImageIO()  // false
 * ImageFormat.AVIF.isWritableByImageIO() // false
 * ```
 */
fun ImageFormat.isWritableByImageIO(): Boolean = this !in ImageFormat.NON_IMAGEIO_WRITABLE

/**
 * 이 포맷이 Java ImageIO 기반 writer로 직접 쓸 수 없는 경우 예외를 발생시킵니다.
 *
 * ## 동작/계약
 * - [isWritableByImageIO]가 `false`인 포맷에서 호출 시 [IllegalArgumentException]을 발생시킵니다.
 *
 * ```kotlin
 * ImageFormat.PNG.requireWritable()  // 통과
 * ImageFormat.AVIF.requireWritable() // 예외 발생
 * ```
 *
 * @throws IllegalArgumentException ImageIO로 쓸 수 없는 포맷인 경우
 */
fun ImageFormat.requireWritable() {
    require(isWritableByImageIO()) {
        "ImageFormat.$name 은 Java ImageIO 기반 writer로 직접 쓸 수 없습니다. 전용 writer를 사용하세요."
    }
}
