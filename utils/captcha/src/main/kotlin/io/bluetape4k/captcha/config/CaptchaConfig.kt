package io.bluetape4k.captcha.config

import io.bluetape4k.logging.KLogging
import java.awt.Color
import java.awt.Font
import java.io.Serializable

/**
 * Captcha 생성 설정 정보
 *
 * ## 동작/계약
 * - 값 객체이며 설정 변경 시 새 인스턴스를 생성해 사용하는 것을 권장합니다.
 * - [theme]에 따라 [themePalette], [backgroundColor] 계산 결과가 달라집니다.
 * - 기본 폰트/팔레트 리스트는 mutable이므로 공유 시 외부 변경에 주의해야 합니다.
 *
 * ```kotlin
 * val cfg = CaptchaConfig(length = 6, theme = CaptchaTheme.DARK)
 * // cfg.backgroundColor == cfg.darkBackgroundColor
 * ```
 *
 * @property width Captcha 이미지의 너비
 * @property height Captcha 이미지의 높이
 * @property length Captcha 코드의 길이
 * @property noiseCount Captcha 이미지에 추가될 노이즈의 개수
 * @property theme Captcha 테마 (eg. LIGHT, DARK)
 * @property lightPalette LIGHT 테마에서 사용될 색상 팔레트
 * @property darkPalette DARK 테마에서 사용될 색상 팔레트
 * @property lightBackgroundColor LIGHT 테마에서 사용될 배경색
 * @property darkBackgroundColor DARK 테마에서 사용될 배경색
 * @property fontStyles 사용될 폰트 스타일
 * @property fontPaths 사용될 폰트 파일 경로
 * @property fontSize 사용될 폰트 크기
 */
data class CaptchaConfig(
    val width: Int = DEFAULT_WIDTH,
    val height: Int = DEFAULT_HEIGHT,
    val length: Int = DEFAULT_LENGTH,
    val noiseCount: Int = DEFAULT_NOISE_COUNT,
    val theme: CaptchaTheme = DEFAULT_THEME,
    val lightPalette: MutableList<Color> = DEFAULT_LIGHT_PALETTE,
    val darkPalette: MutableList<Color> = DEFAULT_DARK_PALETTE,
    val lightBackgroundColor: Color = DEFAULT_LIGHT_BG_COLOR,
    val darkBackgroundColor: Color = DEFAULT_DARK_BG_COLOR,
    val fontStyles: MutableList<Int> = DEFAULT_FONT_STYLES,
    val fontPaths: MutableList<String> = DEFAULT_FONTS,
    val fontSize: Int = (height * 0.9).toInt(),
): Serializable {

    companion object: KLogging() {
        const val DEFAULT_WIDTH = 200
        const val DEFAULT_HEIGHT = 80
        const val DEFAULT_LENGTH = 6
        const val DEFAULT_NOISE_COUNT = 4

        val DEFAULT_THEME = CaptchaTheme.LIGHT

        val DEFAULT_LIGHT_PALETTE = mutableListOf(
            Color.BLACK,
            Color.BLUE,
            Color.RED,
            Color.DARK_GRAY,
        )
        val DEFAULT_DARK_PALETTE = mutableListOf(
            Color.WHITE,
            Color.LIGHT_GRAY,
            Color.CYAN,
            Color.ORANGE,
            Color.YELLOW,
            Color.MAGENTA,
            Color.PINK
        )

        val DEFAULT_LIGHT_BG_COLOR: Color = Color.WHITE
        val DEFAULT_DARK_BG_COLOR = Color(30, 30, 30)

        val DEFAULT_FONT_STYLES = mutableListOf(Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD or Font.ITALIC)
        val DEFAULT_FONTS = mutableListOf<String>()
        val DEFAULT_FONTS_IN_RESOURCE = mutableListOf(
            "/fonts/ComicMono-Bold.ttf",
            "/fonts/JetBrainsMonoNL-Bold.ttf",
            "/fonts/Monaco.ttf",
            "/fonts/Roboto-Bold.ttf",
            "/fonts/Ubuntu-Bold.ttf",
        )

        @JvmStatic
        val DEFAULT = CaptchaConfig()
    }

    val isDarkTheme: Boolean get() = theme == CaptchaTheme.DARK
    val isLightTheme: Boolean get() = theme == CaptchaTheme.LIGHT

    /** 현재 테마에서 사용할 문자 색상 팔레트입니다. */
    val themePalette: List<Color>
        get() = if (isDarkTheme) darkPalette else lightPalette

    /** 현재 테마의 배경색입니다. */
    val backgroundColor: Color
        get() = if (isDarkTheme) darkBackgroundColor else lightBackgroundColor
}
