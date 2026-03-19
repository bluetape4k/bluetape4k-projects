package io.bluetape4k.captcha.config

import io.bluetape4k.captcha.AbstractCaptchaTest
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class CaptchaConfigTest: AbstractCaptchaTest() {

    @Test
    fun `기본 설정에 대한 검증`() {
        val config = CaptchaConfig.DEFAULT

        config.width shouldBeEqualTo CaptchaConfig.DEFAULT_WIDTH
        config.height shouldBeEqualTo CaptchaConfig.DEFAULT_HEIGHT
        config.length shouldBeEqualTo CaptchaConfig.DEFAULT_LENGTH
        config.noiseCount shouldBeEqualTo CaptchaConfig.DEFAULT_NOISE_COUNT

        config.lightPalette shouldBeEqualTo CaptchaConfig.DEFAULT_LIGHT_PALETTE
        config.darkPalette shouldBeEqualTo CaptchaConfig.DEFAULT_DARK_PALETTE

        config.fontStyles shouldBeEqualTo CaptchaConfig.DEFAULT_FONT_STYLES
        config.fontPaths.shouldBeEmpty()

        log.debug { "config=$config" }
    }

    @Test
    fun `테마 변경 시의 컬러 정보`() {
        val config = CaptchaConfig.DEFAULT

        val lightConfig = config.copy(theme = CaptchaTheme.LIGHT)
        lightConfig.isLightTheme.shouldBeTrue()
        lightConfig.themePalette shouldBeEqualTo CaptchaConfig.DEFAULT_LIGHT_PALETTE
        lightConfig.backgroundColor shouldBeEqualTo CaptchaConfig.DEFAULT_LIGHT_BG_COLOR

        val darkConfig = config.copy(theme = CaptchaTheme.DARK)
        darkConfig.isDarkTheme.shouldBeTrue()
        darkConfig.themePalette shouldBeEqualTo CaptchaConfig.DEFAULT_DARK_PALETTE
        darkConfig.backgroundColor shouldBeEqualTo CaptchaConfig.DEFAULT_DARK_BG_COLOR
    }
}
