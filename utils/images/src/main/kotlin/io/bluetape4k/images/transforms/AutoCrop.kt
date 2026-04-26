package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.transforms.internal.getArgbPixels
import io.bluetape4k.images.transforms.internal.toIntArgb
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color

private val log = KotlinLogging.logger {}

/**
 * 이미지의 배경색을 감지하여 불필요한 여백을 자동으로 제거합니다.
 *
 * ## 동작/계약
 * - `tolerance` 범위 내의 픽셀을 배경으로 간주하고, 실제 콘텐츠 경계를 찾아 잘라냅니다.
 * - `backgroundColor` 가 `null` 이면 이미지의 4개 모서리 픽셀을 평균하여 배경색을 추정합니다.
 * - 콘텐츠가 전혀 없는 경우 (모든 픽셀이 배경) 원본 이미지를 그대로 반환하며, debug 로그를 남깁니다.
 * - `padding` 을 지정하면 잘라낸 영역의 외곽에 여백을 추가합니다.
 *
 * ```kotlin
 * val cropped = image.autoCrop()
 * // 배경색 명시
 * val cropped2 = image.autoCrop(tolerance = 20, padding = 5, backgroundColor = Color.WHITE)
 * ```
 *
 * @param tolerance 배경으로 판단하는 RGB 채널별 최대 편차 (0..255, 기본값 10).
 * @param padding 자른 후 각 방향에 추가할 여백 픽셀 수 (0 이상, 기본값 0).
 * @param backgroundColor 기준 배경색. `null` 이면 4개 모서리 평균으로 자동 감지.
 * @return 여백이 제거된 [ImmutableImage]. 콘텐츠가 없으면 원본 반환.
 */
fun ImmutableImage.autoCrop(
    tolerance: Int = 10,
    padding: Int = 0,
    backgroundColor: Color? = null,
): ImmutableImage {
    require(tolerance in 0..255) { "tolerance must be in 0..255, but was $tolerance" }
    require(padding >= 0) { "padding must be >= 0, but was $padding" }

    val w = width
    val h = height
    val argbImage = toIntArgb()
    val pixels = argbImage.getArgbPixels()

    // 배경색 결정: 명시 지정 또는 4 모서리 평균
    val (bgR, bgG, bgB) = if (backgroundColor != null) {
        Triple(backgroundColor.red, backgroundColor.green, backgroundColor.blue)
    } else {
        val topLeft = pixels[0]
        val topRight = pixels[w - 1]
        val bottomLeft = pixels[(h - 1) * w]
        val bottomRight = pixels[h * w - 1]

        val avgR = ((topLeft ushr 16 and 0xFF) + (topRight ushr 16 and 0xFF) +
            (bottomLeft ushr 16 and 0xFF) + (bottomRight ushr 16 and 0xFF)) / 4
        val avgG = ((topLeft ushr 8 and 0xFF) + (topRight ushr 8 and 0xFF) +
            (bottomLeft ushr 8 and 0xFF) + (bottomRight ushr 8 and 0xFF)) / 4
        val avgB = ((topLeft and 0xFF) + (topRight and 0xFF) +
            (bottomLeft and 0xFF) + (bottomRight and 0xFF)) / 4

        Triple(avgR, avgG, avgB)
    }

    // 픽셀이 배경인지 검사: 모든 RGB 채널 편차가 tolerance 이하이면 배경
    fun isBackground(pixel: Int): Boolean {
        val r = pixel ushr 16 and 0xFF
        val g = pixel ushr 8 and 0xFF
        val b = pixel and 0xFF
        return kotlin.math.abs(r - bgR) <= tolerance &&
            kotlin.math.abs(g - bgG) <= tolerance &&
            kotlin.math.abs(b - bgB) <= tolerance
    }

    // 위쪽 경계: 배경이 아닌 픽셀이 하나라도 있는 첫 번째 행
    var top = h
    outer@ for (y in 0 until h) {
        for (x in 0 until w) {
            if (!isBackground(pixels[y * w + x])) {
                top = y
                break@outer
            }
        }
    }

    // 아래쪽 경계: 배경이 아닌 픽셀이 하나라도 있는 마지막 행 (exclusive)
    var bottom = 0
    outer@ for (y in h - 1 downTo 0) {
        for (x in 0 until w) {
            if (!isBackground(pixels[y * w + x])) {
                bottom = y + 1
                break@outer
            }
        }
    }

    // 왼쪽 경계: 배경이 아닌 픽셀이 하나라도 있는 첫 번째 열
    var left = w
    outer@ for (x in 0 until w) {
        for (y in 0 until h) {
            if (!isBackground(pixels[y * w + x])) {
                left = x
                break@outer
            }
        }
    }

    // 오른쪽 경계: 배경이 아닌 픽셀이 하나라도 있는 마지막 열 (exclusive)
    var right = 0
    outer@ for (x in w - 1 downTo 0) {
        for (y in 0 until h) {
            if (!isBackground(pixels[y * w + x])) {
                right = x + 1
                break@outer
            }
        }
    }

    // 패딩 적용
    left = (left - padding).coerceAtLeast(0)
    top = (top - padding).coerceAtLeast(0)
    right = (right + padding).coerceAtMost(w)
    bottom = (bottom + padding).coerceAtMost(h)

    // 콘텐츠 없음 → 원본 반환 (silent fallback)
    if (right - left < 1 || bottom - top < 1) {
        log.debug {
            "autoCrop silent fallback: no content found (w=$w, h=$h, bg=($bgR,$bgG,$bgB))"
        }
        return this
    }

    return subimage(left, top, right - left, bottom - top)
}

/**
 * 코루틴 환경에서 이미지의 배경색을 감지하여 불필요한 여백을 자동으로 제거합니다.
 *
 * ## 동작/계약
 * - [autoCrop] 을 `Dispatchers.Default` 에서 실행합니다.
 * - 콘텐츠가 전혀 없는 경우 원본 이미지를 그대로 반환합니다.
 *
 * ```kotlin
 * val cropped = image.suspendAutoCrop()
 * // 배경색 명시
 * val cropped2 = image.suspendAutoCrop(tolerance = 20, padding = 5, backgroundColor = Color.WHITE)
 * ```
 *
 * @param tolerance 배경으로 판단하는 RGB 채널별 최대 편차 (0..255, 기본값 10).
 * @param padding 자른 후 각 방향에 추가할 여백 픽셀 수 (0 이상, 기본값 0).
 * @param backgroundColor 기준 배경색. `null` 이면 4개 모서리 평균으로 자동 감지.
 * @return 여백이 제거된 [ImmutableImage]. 콘텐츠가 없으면 원본 반환.
 */
suspend fun ImmutableImage.suspendAutoCrop(
    tolerance: Int = 10,
    padding: Int = 0,
    backgroundColor: Color? = null,
): ImmutableImage = withContext(Dispatchers.Default) { autoCrop(tolerance, padding, backgroundColor) }
