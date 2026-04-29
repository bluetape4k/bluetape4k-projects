package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.filter.BlurFilter
import com.sksamuel.scrimage.filter.GaussianBlurFilter
import com.sksamuel.scrimage.filter.MotionBlurFilter
import com.sksamuel.scrimage.filter.NoiseReductionFilter
import com.sksamuel.scrimage.filter.SharpenFilter
import com.sksamuel.scrimage.filter.UnsharpFilter

/** 기본 블러를 적용합니다. */
fun ImageFilterChain.blur() {
    addNative(BlurFilter())
}

/**
 * 가우시안 블러를 적용합니다.
 *
 * @param radius 블러 반경 (픽셀). 0 이상이어야 합니다.
 */
fun ImageFilterChain.gaussianBlur(radius: Int = 2) {
    require(radius >= 0) { "gaussianBlur radius must be >= 0, but was $radius" }
    addNative(GaussianBlurFilter(radius))
}

/**
 * 모션 블러를 적용합니다.
 *
 * @param distance 블러 거리
 * @param angle 블러 각도 (라디안)
 */
fun ImageFilterChain.motionBlur(distance: Double, angle: Double) {
    addNative(MotionBlurFilter(distance, angle))
}

/** 선명도를 높입니다. */
fun ImageFilterChain.sharpen() {
    addNative(SharpenFilter())
}

/** 언샤프 마스크 선명화를 적용합니다. */
fun ImageFilterChain.unsharp() {
    addNative(UnsharpFilter())
}

/** 노이즈를 감소시킵니다. (scrimage [NoiseReductionFilter] — 매개변수 없음) */
fun ImageFilterChain.noiseReduction() {
    addNative(NoiseReductionFilter())
}
