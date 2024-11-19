package io.bluetape4k.images.filters

import com.sksamuel.scrimage.Position
import com.sksamuel.scrimage.filter.CaptionFilter
import com.sksamuel.scrimage.filter.Padding
import io.bluetape4k.images.fonts.DEFAULT_FONT
import java.awt.Color
import java.awt.Font

/**
 * [CaptionFilter] 생성자
 *
 * ```
 * val filter = CaptionFilter(
 *    text = "Power by bluetape4k",
 *    position = Position.BottomLeft,
 * )
 * ```
 *
 * @param text        캡션 텍스트
 * @param position    캡션 위치 (기본값: [Position.BottomLeft])
 * @param font        캡션에 사용할 폰트 정보 (기본값: [DEFAULT_FONT])
 * @param textAlpha   텍스트 투명도 (기본값: 0.5)
 * @param antiAlias   안티앨리어싱 사용 여부 (기본값: false)
 * @param fullWidth   전체 너비 사용 여부 (기본값: false)
 * @param color       캡션 색상 (기본값: [Color.WHITE])
 * @param captionAlpha 캡션 투명도 (기본값: 0.1)
 * @param padding       캡션 패딩 (기본값: [Padding(20)])
 * @return [CaptionFilter] 인스턴스
 */
fun captionFilterOf(
    text: String,
    position: Position = Position.BottomLeft,
    font: Font = DEFAULT_FONT,
    textAlpha: Double = 0.5,
    antiAlias: Boolean = false,
    fullWidth: Boolean = false,
    color: Color = Color.WHITE,
    captionAlpha: Double = 0.1,
    padding: Padding = Padding(20),
): CaptionFilter {
    return CaptionFilter(
        text,
        position,
        font,
        color,
        textAlpha,
        antiAlias,
        fullWidth,
        color,
        captionAlpha,
        padding
    )
}
