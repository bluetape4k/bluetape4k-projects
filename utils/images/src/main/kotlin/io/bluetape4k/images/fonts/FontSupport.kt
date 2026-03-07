package io.bluetape4k.images.fonts

import com.sksamuel.scrimage.FontUtils
import io.bluetape4k.utils.Resourcex
import java.awt.Font
import java.io.InputStream

/**
 * 기본 폰트 크기 (12pt)
 */
const val DEFAULT_FONT_SIZE = 12

/**
 * 기본 [Font] 인스턴스 (`SansSerif`, [Font.PLAIN], 12pt)
 */
val DEFAULT_FONT = fontOf()

/**
 * [Font]를 생성합니다.
 *
 * ## 동작/계약
 * - `SansSerif` 폰트 패밀리를 사용합니다.
 *
 * ```kotlin
 * val font = fontOf(style = Font.BOLD, size = 24)
 * ```
 *
 * @param style 폰트 스타일 (기본값: [Font.PLAIN])
 * @param size 폰트 크기 (기본값: [DEFAULT_FONT_SIZE])
 * @return [Font] 인스턴스
 */
fun fontOf(
    style: Int = Font.PLAIN,
    size: Int = DEFAULT_FONT_SIZE,
): Font =
    Font(Font.SANS_SERIF, style, size)

/**
 * 리소스에서 TrueType [Font]를 로드하여 생성합니다.
 *
 * ## 동작/계약
 * - `/fonts/{fontName}` 경로의 리소스에서 폰트 파일을 읽어 생성합니다.
 * - 리소스가 없으면 `null` 스트림으로 인해 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val font = createTrueTypeFont("Roboto-Regular.ttf", size = 18)
 * ```
 *
 * @param fontName 폰트 파일명 (기본값: "Roboto-Regular.ttf")
 * @param size 폰트 크기 (기본값: [DEFAULT_FONT_SIZE])
 * @return [Font] 인스턴스
 */
fun createTrueTypeFont(
    fontName: String = "Roboto-Regular.ttf",
    size: Int = DEFAULT_FONT_SIZE,
): Font {
    val fontStream: InputStream? = Resourcex.getInputStream("/fonts/$fontName")
    return FontUtils.createTrueType(fontStream, size)
}
