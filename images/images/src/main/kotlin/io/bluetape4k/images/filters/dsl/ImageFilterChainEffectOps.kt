package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.filter.BorderFilter
import com.sksamuel.scrimage.filter.CrystallizeFilter
import com.sksamuel.scrimage.filter.GlowFilter
import com.sksamuel.scrimage.filter.LensFlareFilter
import com.sksamuel.scrimage.filter.OilFilter
import com.sksamuel.scrimage.filter.PixelateFilter
import com.sksamuel.scrimage.filter.VignetteFilter
import io.bluetape4k.images.filters.MedianBlurFilter
import io.bluetape4k.images.filters.MedianBoundaryMode
import io.bluetape4k.images.filters.RoundedCornerFilter
import java.awt.Color

/**
 * 오일 페인팅 효과를 적용합니다.
 *
 * @param range 효과 범위 (기본값 3)
 * @param levels 색상 단계 수 (기본값 256)
 */
fun ImageFilterChain.oil(range: Int = 3, levels: Int = 256) {
    addNative(OilFilter(range, levels))
}

/** 크리스탈 분산 효과를 적용합니다. */
fun ImageFilterChain.crystallize() {
    addNative(CrystallizeFilter())
}

/**
 * 픽셀화 효과를 적용합니다.
 *
 * @param blockSize 픽셀 블록 크기. 1 이상이어야 합니다.
 */
fun ImageFilterChain.pixelate(blockSize: Int = 8) {
    require(blockSize >= 1) { "pixelate blockSize must be >= 1, but was $blockSize" }
    addNative(PixelateFilter(blockSize))
}

/**
 * 미디언 블러로 노이즈를 제거합니다.
 *
 * @param radius 윈도우 반경. 0 이상이어야 합니다.
 * @param boundary 경계 픽셀 처리 방식 (기본값 [MedianBoundaryMode.REPLICATE])
 */
fun ImageFilterChain.medianBlur(
    radius: Int = 1,
    boundary: MedianBoundaryMode = MedianBoundaryMode.REPLICATE,
) {
    addNative(MedianBlurFilter(radius, boundary))
}

/**
 * 이미지 테두리를 추가합니다.
 *
 * @param thickness 테두리 두께 (픽셀). 0 이상이어야 합니다.
 * @param color 테두리 색상 (기본값: 검정)
 */
fun ImageFilterChain.border(thickness: Int = 1, color: Color = Color.BLACK) {
    require(thickness >= 0) { "border thickness must be >= 0, but was $thickness" }
    addNative(BorderFilter(thickness, color))
}

/**
 * 비네트 효과를 적용합니다 (가장자리를 어둡게).
 *
 * @param start 효과 시작 지점 비율 (기본값 0.85)
 * @param end 효과 끝 지점 비율 (기본값 0.95)
 * @param blur 블러 강도 (기본값 0.3)
 * @param color 비네트 색상 (기본값: 검정)
 */
fun ImageFilterChain.vignette(
    start: Float = 0.85f,
    end: Float = 0.95f,
    blur: Float = 0.3f,
    color: Color = Color.BLACK,
) {
    addNative(VignetteFilter(start, end, blur, color))
}

/**
 * 글로우(빛 번짐) 효과를 적용합니다.
 *
 * @param amount 글로우 강도 (기본값 0.5)
 */
fun ImageFilterChain.glow(amount: Float = 0.5f) {
    addNative(GlowFilter(amount))
}

/** 렌즈 플레어 효과를 적용합니다. */
fun ImageFilterChain.lensFlare() {
    addNative(LensFlareFilter())
}

/**
 * 사각형 모서리를 둥글게 깎습니다.
 *
 * @param radius 모서리 반경 (픽셀). 0 이상이어야 합니다.
 */
fun ImageFilterChain.roundedCorners(radius: Int) {
    require(radius >= 0) { "roundedCorners radius must be >= 0, but was $radius" }
    addNative(RoundedCornerFilter(radius))
}
