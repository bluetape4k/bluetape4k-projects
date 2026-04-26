package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.Position
import com.sksamuel.scrimage.filter.Padding
import io.bluetape4k.images.filters.WatermarkFilterType
import io.bluetape4k.images.filters.captionFilterOf
import io.bluetape4k.images.filters.watermarkFilterOf
import io.bluetape4k.images.fonts.DEFAULT_FONT
import java.awt.Color
import java.awt.Font

/**
 * 워터마크를 이미지에 오버레이합니다 (COVER 또는 STAMP 방식).
 *
 * @param text 워터마크 텍스트
 * @param font 폰트 (기본값: [DEFAULT_FONT])
 * @param type 워터마크 방식 (기본값: [WatermarkFilterType.COVER])
 * @param antiAlias 안티앨리어싱 여부 (기본값: true)
 * @param alpha 투명도 (기본값: 0.1)
 * @param color 텍스트 색상 (기본값: 흰색)
 */
fun ImageFilterChain.watermark(
    text: String,
    font: Font = DEFAULT_FONT,
    type: WatermarkFilterType = WatermarkFilterType.COVER,
    antiAlias: Boolean = true,
    alpha: Double = 0.1,
    color: Color = Color.WHITE,
) {
    addNative(watermarkFilterOf(text, font, type, antiAlias, alpha, color))
}

/**
 * 지정된 (x, y) 좌표에 워터마크를 추가합니다.
 *
 * @param text 워터마크 텍스트
 * @param x X 좌표
 * @param y Y 좌표
 * @param font 폰트 (기본값: [DEFAULT_FONT])
 * @param antiAlias 안티앨리어싱 여부 (기본값: true)
 * @param alpha 투명도 (기본값: 0.1)
 * @param color 텍스트 색상 (기본값: 흰색)
 */
fun ImageFilterChain.watermarkAt(
    text: String,
    x: Int,
    y: Int,
    font: Font = DEFAULT_FONT,
    antiAlias: Boolean = true,
    alpha: Double = 0.1,
    color: Color = Color.WHITE,
) {
    addNative(watermarkFilterOf(text, x, y, font, antiAlias, alpha, color))
}

/**
 * 이미지에 캡션 텍스트를 추가합니다.
 *
 * @param text 캡션 텍스트
 * @param position 캡션 위치 (기본값: [Position.BottomLeft])
 * @param font 폰트 (기본값: [DEFAULT_FONT])
 * @param color 텍스트 색상 (기본값: 흰색)
 * @param textAlpha 텍스트 투명도 (기본값: 0.5)
 * @param captionAlpha 캡션 배경 투명도 (기본값: 0.1)
 * @param padding 캡션 패딩 (기본값: Padding(20))
 * @param antiAlias 안티앨리어싱 여부 (기본값: true)
 * @param fullWidth 전체 너비 사용 여부 (기본값: false)
 */
fun ImageFilterChain.caption(
    text: String,
    position: Position = Position.BottomLeft,
    font: Font = DEFAULT_FONT,
    color: Color = Color.WHITE,
    textAlpha: Double = 0.5,
    captionAlpha: Double = 0.1,
    padding: Padding = Padding(20),
    antiAlias: Boolean = true,
    fullWidth: Boolean = false,
) {
    addNative(captionFilterOf(text, position, font, textAlpha, antiAlias, fullWidth, color, captionAlpha, padding))
}
