package io.bluetape4k.images.filters

import com.sksamuel.scrimage.filter.Filter
import com.sksamuel.scrimage.filter.WatermarkCoverFilter
import com.sksamuel.scrimage.filter.WatermarkFilter
import com.sksamuel.scrimage.filter.WatermarkStampFilter
import io.bluetape4k.images.fonts.DEFAULT_FONT
import java.awt.Color
import java.awt.Font

/**
 * [type]에 따라 COVER/STAMP 용 Watermark 용 [Filter]를 생성합니다.
 *
 * @see WatermarkCoverFilter
 * @see WatermarkStampFilter
 *
 * @param text          워터마크 텍스트
 * @param font          워터마크 폰트 (기본값: [DEFAULT_FONT])
 * @param type          워터마크 종류 (기본값: [WatermarkFilterType.COVER])
 * @param antiAlias     안티앨리어싱 사용 여부 (기본값: true)
 * @param alpha         투명도 (기본값: 0.1)
 * @param color         색상 (기본값: [Color.WHITE])
 * @return [Filter] 인스턴스
 */
fun watermarkFilterOf(
    text: String,
    font: Font = DEFAULT_FONT,
    type: WatermarkFilterType = WatermarkFilterType.COVER,
    antiAlias: Boolean = true,
    alpha: Double = 0.1,
    color: Color = Color.WHITE,
): Filter = when (type) {
    WatermarkFilterType.COVER -> WatermarkCoverFilter(text, font, antiAlias, alpha, color)
    WatermarkFilterType.STAMP -> WatermarkStampFilter(text, font, antiAlias, alpha, color)
}

/**
 * (x,y) 위치에 워터마크를 그리는 [WatermarkFilter]를 생성합니다.
 *
 * @param text          워터마크 텍스트
 * @param x             x 좌표
 * @param y             y 좌표
 * @param font          워터마크 폰트 (기본값: [DEFAULT_FONT])
 * @param antiAlias     안티앨리어싱 사용 여부 (기본값: true)
 * @param alpha         투명도 (기본값: 0.1)
 * @param color         색상 (기본값: [Color.WHITE])
 * @return [WatermarkFilter] 인스턴스
 */
fun watermarkFilterOf(
    text: String,
    x: Int,
    y: Int,
    font: Font = DEFAULT_FONT,
    antiAlias: Boolean = true,
    alpha: Double = 0.1,
    color: Color = Color.WHITE,
): WatermarkFilter =
    WatermarkFilter(text, x, y, font, antiAlias, alpha, color)
