package io.bluetape4k.images.filters

import com.sksamuel.scrimage.filter.Padding

/**
 * 상하좌우 동일한 값의 [Padding]을 생성합니다.
 *
 * ```kotlin
 * val padding = paddingOf(20)
 * // 상/하/좌/우 모두 20px 패딩
 * ```
 *
 * @param constant 상하좌우 동일 패딩 값 (px)
 * @return [Padding] 인스턴스
 */
fun paddingOf(constant: Int): Padding = Padding(constant)

/**
 * 상/우/하/좌 개별 값으로 [Padding]을 생성합니다.
 *
 * ```kotlin
 * val padding = paddingOf(top = 10, right = 20, bottom = 10, left = 20)
 * ```
 *
 * @param top    상단 패딩 (px)
 * @param right  우측 패딩 (px)
 * @param bottom 하단 패딩 (px)
 * @param left   좌측 패딩 (px)
 * @return [Padding] 인스턴스
 */
fun paddingOf(
    top: Int,
    right: Int,
    bottom: Int,
    left: Int,
): Padding =
    Padding(top, right, bottom, left)
