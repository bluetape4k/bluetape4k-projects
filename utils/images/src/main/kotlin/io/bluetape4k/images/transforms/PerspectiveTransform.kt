package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.transforms.internal.MAX_OUTPUT_PIXELS
import io.bluetape4k.images.transforms.internal.alphaComponent
import io.bluetape4k.images.transforms.internal.argb
import io.bluetape4k.images.transforms.internal.blueComponent
import io.bluetape4k.images.transforms.internal.fillColor
import io.bluetape4k.images.transforms.internal.getArgbPixels
import io.bluetape4k.images.transforms.internal.greenComponent
import io.bluetape4k.images.transforms.internal.redComponent
import io.bluetape4k.images.transforms.internal.setArgbPixels
import io.bluetape4k.images.transforms.internal.toIntArgb
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private val log = KotlinLogging.logger {}

/**
 * 이미지 평면상의 한 점을 표현합니다.
 *
 * x는 열(column) 방향, y는 행(row) 방향입니다.
 *
 * @property x 열(column) 좌표.
 * @property y 행(row) 좌표.
 */
data class ImagePoint(val x: Double, val y: Double)

/**
 * 4개의 모서리를 매핑하여 원근(perspective) 변환을 적용한 새 [ImmutableImage] 를 반환합니다.
 *
 * `sourceCorners` 는 입력 이미지에서의 4개 점, `destinationCorners` 는 출력 이미지에서의 4개 점이며,
 * 보통 시계방향 좌상→우상→우하→좌하 순서로 지정합니다. 두 점 집합은 4×4 호모그래피(H, 3×3, h33=1)를
 * 정의하고, 출력 픽셀에서 역 호모그래피(H⁻¹) 로 원본 좌표를 역추적하여 4-tap bilinear sampling 으로
 * 색상을 결정합니다.
 *
 * 역추적된 좌표가 입력 이미지 범위를 벗어나면 [outsideColor] 로 채웁니다.
 *
 * ## 출력 한도
 * `outputWidth × outputHeight` 는 [MAX_OUTPUT_PIXELS] (64M pixels) 이하여야 합니다.
 *
 * ```kotlin
 * val src = listOf(
 *     ImagePoint(10.0, 10.0),
 *     ImagePoint(190.0, 12.0),
 *     ImagePoint(195.0, 195.0),
 *     ImagePoint(8.0, 200.0),
 * )
 * val dst = listOf(
 *     ImagePoint(0.0, 0.0),
 *     ImagePoint(255.0, 0.0),
 *     ImagePoint(255.0, 255.0),
 *     ImagePoint(0.0, 255.0),
 * )
 * val warped = image.perspectiveTransform(src, dst, 256, 256)
 * ```
 *
 * @param sourceCorners        입력 이미지의 4개 점 (시계방향 좌상→우상→우하→좌하 권장).
 * @param destinationCorners   출력 이미지의 4개 점 (대응 순서).
 * @param outputWidth          출력 이미지 너비 (양수).
 * @param outputHeight         출력 이미지 높이 (양수).
 * @param outsideColor         원본 범위를 벗어난 픽셀을 채울 색 (기본값: 투명).
 * @return 변환된 새 [ImmutableImage].
 * @throws IllegalArgumentException 점이 4개가 아니거나, 출력 크기가 한도를 초과하거나, 점이 거의 일직선이거나
 * 호모그래피가 퇴화한 경우.
 */
fun ImmutableImage.perspectiveTransform(
    sourceCorners: List<ImagePoint>,
    destinationCorners: List<ImagePoint>,
    outputWidth: Int,
    outputHeight: Int,
    outsideColor: Color = Color(0, 0, 0, 0),
): ImmutableImage {
    require(sourceCorners.size == 4) { "sourceCorners must have exactly 4 points" }
    require(destinationCorners.size == 4) { "destinationCorners must have exactly 4 points" }
    require(outputWidth > 0 && outputHeight > 0) { "output dimensions must be positive" }
    require(outputWidth.toLong() * outputHeight.toLong() <= MAX_OUTPUT_PIXELS) {
        "output size ${outputWidth}x${outputHeight} exceeds 64M pixels limit"
    }
    require(sourceCorners.all { it.x.isFinite() && it.y.isFinite() }) {
        "sourceCorners must have finite coordinates"
    }
    require(destinationCorners.all { it.x.isFinite() && it.y.isFinite() }) {
        "destinationCorners must have finite coordinates"
    }

    log.debug {
        "perspectiveTransform: src=${width}x${height}, out=${outputWidth}x${outputHeight}"
    }

    val homography = computeHomography(sourceCorners, destinationCorners)
    val inverse = invert3x3(homography)

    val srcBuf = this.toIntArgb()
    val srcW = srcBuf.width
    val srcH = srcBuf.height
    val srcPixels = srcBuf.getArgbPixels()

    val outBuf = BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB)
        .fillColor(outsideColor)
    val outPixels = outBuf.getArgbPixels()

    val maxX = (srcW - 1).toDouble()
    val maxY = (srcH - 1).toDouble()

    for (y in 0 until outputHeight) {
        val rowOffset = y * outputWidth
        val cy = y + 0.5
        for (x in 0 until outputWidth) {
            val cx = x + 0.5
            val wx = inverse[0] * cx + inverse[1] * cy + inverse[2]
            val wy = inverse[3] * cx + inverse[4] * cy + inverse[5]
            val ww = inverse[6] * cx + inverse[7] * cy + inverse[8]

            if (ww == 0.0 || !ww.isFinite()) continue

            val srcX = wx / ww
            val srcY = wy / ww

            if (!srcX.isFinite() || !srcY.isFinite()) continue
            if (srcX < 0.0 || srcY < 0.0 || srcX > maxX || srcY > maxY) continue

            outPixels[rowOffset + x] = bilinearSample(srcPixels, srcW, srcH, srcX, srcY)
        }
    }

    outBuf.setArgbPixels(outPixels)
    return ImmutableImage.wrapAwt(outBuf)
}

/**
 * Coroutines 환경에서 [perspectiveTransform] 을 실행합니다.
 *
 * CPU 바운드 작업이므로 [Dispatchers.Default] 에서 수행합니다.
 *
 * @see perspectiveTransform
 */
suspend fun ImmutableImage.suspendPerspectiveTransform(
    sourceCorners: List<ImagePoint>,
    destinationCorners: List<ImagePoint>,
    outputWidth: Int,
    outputHeight: Int,
    outsideColor: Color = Color(0, 0, 0, 0),
): ImmutableImage = withContext(Dispatchers.Default) {
    perspectiveTransform(sourceCorners, destinationCorners, outputWidth, outputHeight, outsideColor)
}

/**
 * 4쌍 점에 대한 호모그래피 행렬 H (3×3, h33=1) 를 계산합니다.
 *
 * 각 점쌍 (sx, sy) → (dx, dy) 에 대해 다음 두 행을 8×8 선형계에 추가합니다.
 * - `[sx, sy, 1, 0, 0, 0, -dx*sx, -dx*sy] · h = dx`
 * - `[0, 0, 0, sx, sy, 1, -dy*sx, -dy*sy] · h = dy`
 *
 * @return `[h11, h12, h13, h21, h22, h23, h31, h32, h33=1.0]` (row-major, size 9).
 */
private fun computeHomography(src: List<ImagePoint>, dst: List<ImagePoint>): DoubleArray {
    val a = DoubleArray(8 * 8)
    val b = DoubleArray(8)

    for (i in 0 until 4) {
        val sx = src[i].x
        val sy = src[i].y
        val dx = dst[i].x
        val dy = dst[i].y

        val r1 = (i * 2) * 8
        a[r1 + 0] = sx
        a[r1 + 1] = sy
        a[r1 + 2] = 1.0
        a[r1 + 3] = 0.0
        a[r1 + 4] = 0.0
        a[r1 + 5] = 0.0
        a[r1 + 6] = -dx * sx
        a[r1 + 7] = -dx * sy
        b[i * 2] = dx

        val r2 = (i * 2 + 1) * 8
        a[r2 + 0] = 0.0
        a[r2 + 1] = 0.0
        a[r2 + 2] = 0.0
        a[r2 + 3] = sx
        a[r2 + 4] = sy
        a[r2 + 5] = 1.0
        a[r2 + 6] = -dy * sx
        a[r2 + 7] = -dy * sy
        b[i * 2 + 1] = dy
    }

    val solution = solve8x8(a, b)
    return doubleArrayOf(
        solution[0], solution[1], solution[2],
        solution[3], solution[4], solution[5],
        solution[6], solution[7], 1.0,
    )
}

/**
 * 부분 피벗팅(partial pivoting) 을 사용하는 Gauss-Jordan 소거법으로 8×8 선형계를 풉니다.
 *
 * @param a 8×8 row-major 계수 행렬 (size 64). 호출 후 변경됩니다.
 * @param b 우변 벡터 (size 8). 호출 후 해(x) 로 덮어쓰입니다.
 * @return 해 벡터 (size 8). 입력 [b] 와 동일한 인스턴스입니다.
 * @throws IllegalArgumentException 피벗 절댓값이 1e-12 미만이면 (점이 거의 일직선) 발생합니다.
 */
private fun solve8x8(a: DoubleArray, b: DoubleArray): DoubleArray {
    val n = 8
    val pivotEpsilon = 1e-12

    for (col in 0 until n) {
        // Find pivot row
        var pivotRow = col
        var pivotAbs = abs(a[col * n + col])
        for (r in (col + 1) until n) {
            val v = abs(a[r * n + col])
            if (v > pivotAbs) {
                pivotAbs = v
                pivotRow = r
            }
        }
        if (pivotAbs < pivotEpsilon) {
            throw IllegalArgumentException("source or destination points are nearly collinear")
        }

        // Swap pivot row with current row
        if (pivotRow != col) {
            for (c in 0 until n) {
                val tmp = a[col * n + c]
                a[col * n + c] = a[pivotRow * n + c]
                a[pivotRow * n + c] = tmp
            }
            val tmpB = b[col]
            b[col] = b[pivotRow]
            b[pivotRow] = tmpB
        }

        // Normalize pivot row
        val pivot = a[col * n + col]
        for (c in 0 until n) {
            a[col * n + c] /= pivot
        }
        b[col] /= pivot

        // Eliminate other rows
        for (r in 0 until n) {
            if (r == col) continue
            val factor = a[r * n + col]
            if (factor == 0.0) continue
            for (c in 0 until n) {
                a[r * n + c] -= factor * a[col * n + c]
            }
            b[r] -= factor * b[col]
        }
    }
    return b
}

/**
 * 3×3 행렬의 역행렬을 cofactor/adjugate 방식의 닫힌 형태(closed form)로 계산합니다.
 *
 * @param h row-major 3×3 행렬 (size 9).
 * @return 역행렬 (size 9, row-major).
 * @throws IllegalArgumentException 행렬식의 절댓값이 1e-12 미만이면 (퇴화된 호모그래피) 발생합니다.
 */
private fun invert3x3(h: DoubleArray): DoubleArray {
    val det = h[0] * (h[4] * h[8] - h[5] * h[7]) -
        h[1] * (h[3] * h[8] - h[5] * h[6]) +
        h[2] * (h[3] * h[7] - h[4] * h[6])

    if (abs(det) < 1e-12) {
        throw IllegalArgumentException("homography matrix is degenerate")
    }

    val invDet = 1.0 / det
    val r = DoubleArray(9)
    r[0] = (h[4] * h[8] - h[5] * h[7]) * invDet
    r[1] = (h[2] * h[7] - h[1] * h[8]) * invDet
    r[2] = (h[1] * h[5] - h[2] * h[4]) * invDet
    r[3] = (h[5] * h[6] - h[3] * h[8]) * invDet
    r[4] = (h[0] * h[8] - h[2] * h[6]) * invDet
    r[5] = (h[2] * h[3] - h[0] * h[5]) * invDet
    r[6] = (h[3] * h[7] - h[4] * h[6]) * invDet
    r[7] = (h[1] * h[6] - h[0] * h[7]) * invDet
    r[8] = (h[0] * h[4] - h[1] * h[3]) * invDet
    return r
}

/**
 * 4-tap bilinear sampling 으로 ARGB 픽셀을 보간합니다.
 *
 * 좌표가 정수 경계 바깥(예: -0.5 ~ 0.0) 에 있을 수 있으므로 코너 인덱스는 [0, w-1], [0, h-1] 범위로 클램프됩니다.
 *
 * @param pixels row-major ARGB 픽셀 배열.
 * @param w      이미지 너비.
 * @param h      이미지 높이.
 * @param x      샘플링할 x 좌표 (실수).
 * @param y      샘플링할 y 좌표 (실수).
 * @return 보간된 ARGB packed `Int`.
 */
private fun bilinearSample(pixels: IntArray, w: Int, h: Int, x: Double, y: Double): Int {
    val x0i = floor(x).toInt()
    val y0i = floor(y).toInt()
    val fx = x - x0i
    val fy = y - y0i

    val x0 = x0i.coerceIn(0, w - 1)
    val y0 = y0i.coerceIn(0, h - 1)
    val x1 = (x0i + 1).coerceIn(0, w - 1)
    val y1 = (y0i + 1).coerceIn(0, h - 1)

    val c00 = pixels[y0 * w + x0]
    val c10 = pixels[y0 * w + x1]
    val c01 = pixels[y1 * w + x0]
    val c11 = pixels[y1 * w + x1]

    val w00 = (1.0 - fx) * (1.0 - fy)
    val w10 = fx * (1.0 - fy)
    val w01 = (1.0 - fx) * fy
    val w11 = fx * fy

    val a = (w00 * c00.alphaComponent() + w10 * c10.alphaComponent() +
        w01 * c01.alphaComponent() + w11 * c11.alphaComponent()).roundToInt().coerceIn(0, 255)
    val r = (w00 * c00.redComponent() + w10 * c10.redComponent() +
        w01 * c01.redComponent() + w11 * c11.redComponent()).roundToInt().coerceIn(0, 255)
    val g = (w00 * c00.greenComponent() + w10 * c10.greenComponent() +
        w01 * c01.greenComponent() + w11 * c11.greenComponent()).roundToInt().coerceIn(0, 255)
    val b = (w00 * c00.blueComponent() + w10 * c10.blueComponent() +
        w01 * c01.blueComponent() + w11 * c11.blueComponent()).roundToInt().coerceIn(0, 255)

    return argb(a, r, g, b)
}
