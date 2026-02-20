package io.bluetape4k.images

/**
 * 지원하는 Image Format
 */
enum class ImageFormat {
    GIF, JPG, PNG, WEBP;

    companion object {
        @JvmStatic
        fun parse(formatName: String): ImageFormat? {
            val normalized = formatName.trim()
            if (normalized.isEmpty()) return null
            return entries.find { it.name.equals(normalized, ignoreCase = true) }
        }
    }
}
