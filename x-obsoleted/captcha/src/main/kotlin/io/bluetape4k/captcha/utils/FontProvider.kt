package io.bluetape4k.captcha.utils

import io.bluetape4k.captcha.exceptions.FontLoadException
import io.bluetape4k.captcha.utils.FontProvider.loadAllFontsFromResource
import io.bluetape4k.captcha.utils.FontProvider.loadFontFromResource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.utils.Resourcex
import java.awt.Font

/**
 * CAPTCHA 렌더링용 폰트를 리소스에서 로드하는 유틸리티입니다.
 *
 * ## 동작/계약
 * - 리소스 경로 단위로 TTF 폰트 로딩을 시도합니다.
 * - 개별 폰트 로드 실패는 경고 로그 후 건너뜁니다([loadAllFontsFromResource]).
 *
 * ```kotlin
 * val fonts = FontProvider.loadAllFontsFromResource(listOf("/fonts/Roboto-Bold.ttf"))
 * // fonts.isNotEmpty() == true
 * ```
 */
object FontProvider: KLogging() {

    /**
     * 리소스 경로 목록의 폰트를 모두 로드합니다.
     *
     * ## 동작/계약
     * - 각 경로마다 [loadFontFromResource]를 호출합니다.
     * - 실패한 항목은 제외하고 성공 항목만 반환합니다.
     */
    fun loadAllFontsFromResource(resourcePaths: List<String>): List<Font> {
        return resourcePaths.mapNotNull {
            runCatching {
                loadFontFromResource(it)
            }.onFailure { ex ->
                log.warn(ex) { "Failed to load font from resource: $it" }
            }.getOrNull()
        }
    }

    /**
     * 단일 리소스 경로에서 TTF 폰트를 로드합니다.
     *
     * ## 동작/계약
     * - 리소스를 찾지 못하면 [FontLoadException]을 던집니다.
     * - 성공 시 [Font.createFont] 결과를 반환합니다.
     */
    fun loadFontFromResource(resourcePath: String): Font? {
        val fontStream = Resourcex.getInputStream(resourcePath)
            ?: throw FontLoadException("Font file not found: $resourcePath")

        return Font.createFont(Font.TRUETYPE_FONT, fontStream)
    }
}
