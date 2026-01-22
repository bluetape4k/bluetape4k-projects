package io.bluetape4k.captcha.utils

import io.bluetape4k.captcha.AbstractCaptchaTest
import io.bluetape4k.captcha.config.CaptchaConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class FontProviderTest: AbstractCaptchaTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `리소스에 있는 기본 폰트를 로드합니다`() {
        val paths = CaptchaConfig.DEFAULT_FONTS_IN_RESOURCE
        paths.forEach { path ->
            log.debug { "Load font from $path" }
            val font = FontProvider.loadFontFromResource(path)
            log.info { "Font loaded: $font" }
            font.shouldNotBeNull()
        }
    }

    @Test
    fun `리소스에 있는 모든 폰트를 로드합니다`() {
        val paths = CaptchaConfig.DEFAULT_FONTS_IN_RESOURCE
        val fonts = FontProvider.loadAllFontsFromResource(paths)
        fonts.forEach {
            log.debug { it }
        }
        fonts.shouldNotBeEmpty() shouldHaveSize paths.size
    }
}
