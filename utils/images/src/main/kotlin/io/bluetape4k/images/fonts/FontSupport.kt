package io.bluetape4k.images.fonts

import com.sksamuel.scrimage.FontUtils
import io.bluetape4k.utils.Resourcex
import java.awt.Font

/**
 * 기본 폰트 크기 (12)
 */
const val DEFAULT_FONT_SIZE = 12

/**
 * 기본 [Font] 인스턴스
 */
val DEFAULT_FONT = fontOf()

/**
 * [Font]를 생성합니다.
 *
 * @param style 폰트 스타일 (기본값: [Font.PLAIN])
 * @param size 폰트 크기 (기본값: [DEFAULT_FONT_SIZE])
 * @return [Font] 인스턴스
 */
fun fontOf(
    style: Int = Font.PLAIN,
    size: Int = DEFAULT_FONT_SIZE,
): Font {
    return Font(Font.SANS_SERIF, style, size)
}

/**
 * True type [Font]를 생성합니다.
 *
 * @param fontName 폰트 이름 (기본값: "Roboto-Regular.ttf")
 * @param size 폰트 크기 (기본값: [DEFAULT_FONT_SIZE])
 * @return [Font] 인스턴스
 */
fun createTrueTypeFont(
    fontName: String = "Roboto-Regular.ttf",
    size: Int = DEFAULT_FONT_SIZE,
): Font {
    val fontStream = Resourcex.getInputStream("/fonts/$fontName")
    return FontUtils.createTrueType(fontStream, size)
}
