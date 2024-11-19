package io.bluetape4k.tokenizer.model

import java.io.Serializable
import java.util.*

/**
 * 금칙어 처리를 위한 옵션 정보
 *
 * @property mask      금칙어를 mask 처리할 때 사용할 문자열 (기본값: *)
 * @property locale    [Locale] 정보 (기본값: [Locale.KOREAN])
 * @property severity  금칙어에 해당하는 수준 (심각도) (기본값: [Severity.DEFAULT] = [Severity.LOW])
 */
data class BlockwordOptions(
    var mask: String = "*",
    var locale: Locale = Locale.KOREAN,
    var severity: Severity = Severity.DEFAULT,
): Serializable {
    companion object {
        val DEFAULT = BlockwordOptions()
    }
}

/**
 * 금칙어 처리 옵션 정보를 생성한다.
 *
 * @param mask    금칙어를 mask 처리할 때 사용할 문자열 (기본값: `*`)
 * @param locale  [Locale] 정보 (기본값: [Locale.KOREAN])
 */
fun blockwordOptionsOf(
    mask: String = "*",
    locale: Locale = Locale.KOREAN,
): BlockwordOptions {
    return BlockwordOptions(mask, locale)
}
