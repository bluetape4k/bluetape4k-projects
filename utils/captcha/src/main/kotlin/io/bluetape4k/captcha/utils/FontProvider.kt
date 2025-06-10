package io.bluetape4k.captcha.utils

import io.bluetape4k.captcha.exceptions.FontLoadException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.utils.Resourcex
import java.awt.Font

object FontProvider: KLogging() {

    fun loadAllFontsFromResource(resourcePaths: List<String>): List<Font> {
        return resourcePaths.mapNotNull {
            runCatching {
                loadFontFromResource(it)
            }.onFailure { ex ->
                log.warn(ex) { "Failed to load font from resource: $it" }
            }.getOrNull()
        }
    }

    fun loadFontFromResource(resourcePath: String): Font? {
        val fontStream = Resourcex.getInputStream(resourcePath)
            ?: throw FontLoadException("Font file not found: $resourcePath")

        return Font.createFont(Font.TRUETYPE_FONT, fontStream)
    }
}
