package io.bluetape4k.images

import io.bluetape4k.images.ImageFormat.Companion.parse


/**
 * 지원하는 이미지 포맷 열거형입니다.
 *
 * ## 동작/계약
 * - 현재 `GIF`, `JPG`, `PNG`, `WEBP` 포맷을 지원합니다.
 * - [parse]는 대소문자를 무시하고 매칭하며, 공백/미지원 문자열은 `null`을 반환합니다.
 *
 * ```kotlin
 * val png = ImageFormat.parse("png")
 * val unknown = ImageFormat.parse("tiff")
 * // png == ImageFormat.PNG
 * // unknown == null
 * ```
 */
enum class ImageFormat {
    GIF, JPG, PNG, WEBP;

    companion object {
        /**
         * 문자열을 [ImageFormat]으로 파싱합니다.
         *
         * ## 동작/계약
         * - 앞뒤 공백을 제거한 뒤 enum 이름과 대소문자 무시 비교합니다.
         * - 매칭 실패 시 `null`을 반환합니다.
         */
        @JvmStatic
        fun parse(formatName: String): ImageFormat? {
            val normalized = formatName.trim()
            if (normalized.isEmpty()) return null
            return entries.find { it.name.equals(normalized, ignoreCase = true) }
        }
    }
}
