package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.transforms.internal.alphaComponent
import io.bluetape4k.images.transforms.internal.argb
import io.bluetape4k.images.transforms.internal.blueComponent
import io.bluetape4k.images.transforms.internal.copyArgb
import io.bluetape4k.images.transforms.internal.getArgbPixels
import io.bluetape4k.images.transforms.internal.greenComponent
import io.bluetape4k.images.transforms.internal.redComponent
import io.bluetape4k.images.transforms.internal.setArgbPixels
import io.bluetape4k.images.transforms.internal.toIntArgb
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val log = KotlinLogging.logger {}

/**
 * CLAHE (Contrast Limited Adaptive Histogram Equalization) 를 적용한 새 [ImmutableImage] 를 반환합니다.
 *
 * 이 구현은 다음 단계로 진행됩니다.
 *
 * 1. RGB 채널을 YCbCr (BT.601) 색공간으로 변환하여 휘도(Y) 평면에만 평활화를 적용합니다.
 *    크로마(Cb, Cr) 채널은 보존되어 색상 왜곡을 최소화합니다.
 * 2. 영상을 [tileSize] x [tileSize] 크기의 타일로 분할하고, 각 타일별 히스토그램을 계산합니다.
 *    - 영상 크기가 [tileSize] 보다 작으면 단일 타일로 fallback 합니다.
 * 3. 히스토그램의 한 빈이 `clipLimit * tilePixelCount / 256` 을 초과하면 잘라내고(clip),
 *    잘려나간 양을 모든 빈에 균등 재분배하여 노이즈 증폭을 제어합니다.
 * 4. 각 타일의 누적 분포 함수(CDF) 로 256-entry LUT 를 구성합니다.
 * 5. 각 픽셀에 대해 인접 4개 타일의 LUT 결과를 거리 기반 **bilinear 보간** 하여
 *    타일 경계에서 발생하는 블록 아티팩트를 제거합니다.
 * 6. 변환된 Y 와 원본 Cb, Cr 을 RGB 로 역변환하여 결과 이미지를 생성합니다.
 *
 * @param tileSize 타일 한 변의 픽셀 크기. 작을수록 적응성이 높아지고 클수록 전역에 가까워집니다. 기본값 8.
 * @param clipLimit 히스토그램 빈 별 상한 배수 (1.0 이면 균등 분포 수준). 클수록 대비 향상이 강해지고 노이즈가 증가합니다. 기본값 2.0.
 * @return CLAHE 가 적용된 새 [ImmutableImage].
 * @throws IllegalArgumentException [tileSize] 가 1 미만이거나 [clipLimit] 가 0 이하일 때.
 */
fun ImmutableImage.clahe(tileSize: Int = 8, clipLimit: Double = 2.0): ImmutableImage {
    require(tileSize >= 1) { "tileSize must be >= 1" }
    require(clipLimit > 0.0) { "clipLimit must be > 0" }

    val srcBuf = this.toIntArgb()
    val pixels = srcBuf.getArgbPixels()
    val w = srcBuf.width
    val h = srcBuf.height
    val total = w * h

    // Step 2: RGB -> YCbCr (BT.601)
    val yPlane = IntArray(total)
    val cbPlane = IntArray(total)
    val crPlane = IntArray(total)
    for (i in 0 until total) {
        val argb = pixels[i]
        val r = argb.redComponent()
        val g = argb.greenComponent()
        val b = argb.blueComponent()
        yPlane[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
        cbPlane[i] = (-0.169 * r - 0.331 * g + 0.5 * b + 128).toInt().coerceIn(0, 255)
        crPlane[i] = (0.5 * r - 0.419 * g - 0.081 * b + 128).toInt().coerceIn(0, 255)
    }

    // Step 3: Tile fallback — clamp to maxOf(w,h) so globalEqualize (tileSize=max) yields a 1×1 grid
    val effectiveTileSize = if (tileSize >= maxOf(w, h)) {
        log.debug { "clahe tile fallback: tile=$tileSize >= max(w=$w, h=$h), collapsing to single tile" }
        maxOf(w, h)
    } else {
        tileSize
    }

    // Step 4: Compute tile grid
    val nTilesX = (w + effectiveTileSize - 1) / effectiveTileSize
    val nTilesY = (h + effectiveTileSize - 1) / effectiveTileSize

    // Step 5: For each tile, compute histogram -> clip -> CDF -> LUT
    val tileLuts = Array(nTilesY) { ty ->
        Array(nTilesX) { tx ->
            val x0 = tx * effectiveTileSize
            val y0 = ty * effectiveTileSize
            val x1 = minOf(x0 + effectiveTileSize, w)
            val y1 = minOf(y0 + effectiveTileSize, h)
            val tilePixelCount = (x1 - x0) * (y1 - y0)

            // Build histogram
            val hist = IntArray(256)
            for (py in y0 until y1) {
                for (px in x0 until x1) {
                    hist[yPlane[py * w + px]]++
                }
            }

            // Clip histogram
            val cap = (clipLimit * tilePixelCount / 256).toInt().coerceAtLeast(1)
            var excess = 0
            for (i in 0 until 256) {
                if (hist[i] > cap) {
                    excess += hist[i] - cap
                    hist[i] = cap
                }
            }
            // Redistribute excess evenly
            val addPerBin = excess / 256
            val remainder = excess % 256
            for (i in 0 until 256) {
                hist[i] += addPerBin
            }
            for (i in 0 until remainder) {
                hist[i]++
            }

            // Build CDF LUT
            val lut = IntArray(256)
            var cdf = 0
            for (i in 0 until 256) {
                cdf += hist[i]
                lut[i] = (cdf.toLong() * 255 / tilePixelCount).toInt().coerceIn(0, 255)
            }
            lut
        }
    }

    // Step 6: Apply bilinear tile interpolation
    for (py in 0 until h) {
        for (px in 0 until w) {
            val txF = (px - effectiveTileSize / 2.0) / effectiveTileSize
            val tyF = (py - effectiveTileSize / 2.0) / effectiveTileSize
            val tx0 = txF.toInt().coerceIn(0, nTilesX - 1)
            val tx1 = (tx0 + 1).coerceIn(0, nTilesX - 1)
            val ty0 = tyF.toInt().coerceIn(0, nTilesY - 1)
            val ty1 = (ty0 + 1).coerceIn(0, nTilesY - 1)
            val wx = (txF - tx0).coerceIn(0.0, 1.0)
            val wy = (tyF - ty0).coerceIn(0.0, 1.0)
            val yVal = yPlane[py * w + px]
            val v00 = tileLuts[ty0][tx0][yVal]
            val v10 = tileLuts[ty0][tx1][yVal]
            val v01 = tileLuts[ty1][tx0][yVal]
            val v11 = tileLuts[ty1][tx1][yVal]
            val yNew = ((1 - wx) * (1 - wy) * v00
                + wx * (1 - wy) * v10
                + (1 - wx) * wy * v01
                + wx * wy * v11).toInt().coerceIn(0, 255)
            yPlane[py * w + px] = yNew
        }
    }

    // Step 7: YCbCr -> RGB
    for (i in 0 until total) {
        val y = yPlane[i]
        val cb = cbPlane[i] - 128
        val cr = crPlane[i] - 128
        val r = (y + 1.402 * cr).toInt().coerceIn(0, 255)
        val g = (y - 0.344 * cb - 0.714 * cr).toInt().coerceIn(0, 255)
        val b = (y + 1.772 * cb).toInt().coerceIn(0, 255)
        val alpha = pixels[i].alphaComponent()
        pixels[i] = argb(alpha, r, g, b)
    }

    // Step 8: Return result
    val outBuf = srcBuf.copyArgb()
    outBuf.setArgbPixels(pixels)
    return ImmutableImage.wrapAwt(outBuf)
}

/**
 * 영상 전체를 단일 타일로 처리하는 전역 히스토그램 평활화를 적용합니다.
 *
 * 내부적으로 [clahe] 를 `tileSize = max(width, height)` 로 호출하여
 * 단일 타일 LUT 를 생성한 뒤 모든 픽셀에 동일하게 적용합니다.
 * `clipLimit = 2.0` 으로 약한 클리핑이 들어가므로 순수 글로벌 평활화 보다 약간의 노이즈 억제 효과가 있습니다.
 *
 * @return 전역 평활화가 적용된 새 [ImmutableImage].
 */
fun ImmutableImage.globalEqualize(): ImmutableImage =
    clahe(tileSize = maxOf(width, height), clipLimit = 2.0)

/**
 * [clahe] 의 코루틴 비동기 버전.
 *
 * 픽셀 연산이 CPU 바운드이므로 [Dispatchers.Default] 컨텍스트에서 수행합니다.
 *
 * @param tileSize 타일 한 변의 픽셀 크기. 기본값 8.
 * @param clipLimit 히스토그램 빈 별 상한 배수. 기본값 2.0.
 * @return CLAHE 가 적용된 새 [ImmutableImage].
 */
suspend fun ImmutableImage.suspendClahe(
    tileSize: Int = 8,
    clipLimit: Double = 2.0,
): ImmutableImage = withContext(Dispatchers.Default) {
    clahe(tileSize, clipLimit)
}
