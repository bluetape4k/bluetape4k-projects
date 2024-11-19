package io.bluetape4k.images

/**
 * 지원하는 Image Format
 */
enum class ImageFormat {
    GIF, JPG, PNG, WEBP;

    companion object {
        @JvmStatic
        fun parse(formatName: String): ImageFormat? =
            entries.find { it.name.equals(formatName, ignoreCase = true) }
    }
}
