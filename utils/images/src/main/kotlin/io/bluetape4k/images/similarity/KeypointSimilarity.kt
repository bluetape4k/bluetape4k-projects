package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage
import kotlin.math.sqrt

private const val BLOCK_MEAN_DEFAULT_GRID = 8

/**
 * 이미지를 [gridRows]×[gridCols] 그리드로 분할하여 각 셀의 평균 휘도를 descriptor로 반환합니다.
 *
 * 이동·JPEG 압축에 견고하나 회전·스케일 변형에는 제한적입니다.
 * 회전 불변 유사도는 [bestRotationSimilarityTo]를 사용하세요.
 *
 * SIFT/ORB 수준의 robust 매칭이 필요하면 별도 이슈(BoofCV 통합)를 참조하세요.
 *
 * @param gridRows 행 그리드 수. 기본 8.
 * @param gridCols 열 그리드 수. 기본 8.
 * @return 길이 `gridRows*gridCols`의 정규화된 평균 휘도 배열 (0.0 ~ 1.0)
 */
fun ImmutableImage.blockMeanDescriptor(
    gridRows: Int = BLOCK_MEAN_DEFAULT_GRID,
    gridCols: Int = BLOCK_MEAN_DEFAULT_GRID,
): DoubleArray {
    require(gridRows >= 1) { "gridRows는 1 이상: $gridRows" }
    require(gridCols >= 1) { "gridCols는 1 이상: $gridCols" }
    val cellH = height.toDouble() / gridRows
    val cellW = width.toDouble() / gridCols
    val descriptor = DoubleArray(gridRows * gridCols)
    for (r in 0 until gridRows) {
        val yStart = (r * cellH).toInt()
        val yEnd = ((r + 1) * cellH).toInt().coerceAtMost(height)
        for (c in 0 until gridCols) {
            val xStart = (c * cellW).toInt()
            val xEnd = ((c + 1) * cellW).toInt().coerceAtMost(width)
            var sum = 0.0
            var cnt = 0
            for (y in yStart until yEnd) {
                for (x in xStart until xEnd) {
                    sum += luminance(pixel(x, y))
                    cnt++
                }
            }
            descriptor[r * gridCols + c] = if (cnt > 0) (sum / cnt) / PIXEL_MAX else 0.0
        }
    }
    return descriptor
}

/**
 * Block-Mean descriptor의 L2 거리를 [0,1] 유사도로 반환합니다.
 * `similarity = 1 / (1 + L2)`. 완전 동일 = 1.0.
 *
 * @param other 비교 대상 이미지
 * @param gridRows 행 그리드 수. 기본 8.
 * @param gridCols 열 그리드 수. 기본 8.
 * @return 유사도 (0.0 ~ 1.0). 완전 동일 = 1.0.
 */
fun ImmutableImage.blockMeanSimilarityTo(
    other: ImmutableImage,
    gridRows: Int = BLOCK_MEAN_DEFAULT_GRID,
    gridCols: Int = BLOCK_MEAN_DEFAULT_GRID,
): Double {
    val da = blockMeanDescriptor(gridRows, gridCols)
    val db = other.blockMeanDescriptor(gridRows, gridCols)
    val l2 = sqrt(da.indices.sumOf { val d = da[it] - db[it]; d * d })
    return 1.0 / (1.0 + l2)
}

/**
 * 0°/90°/180°/270° 회전 4가지 중 최대 [blockMeanSimilarityTo]를 반환합니다.
 *
 * `other` 이미지를 4방향으로 회전하여 가장 높은 유사도를 찾습니다.
 * 비정사각 이미지도 지원하나, 종횡비가 바뀌므로 90°/270° 회전 시 크기가 달라집니다.
 * 내부적으로 `other`를 64×64로 축소 후 회전하여 성능을 최적화합니다.
 * `this`는 원본 해상도를 유지합니다 — block-mean descriptor는 크기에 정규화되므로 비대칭 스케일도 비교 가능합니다.
 *
 * @param other 비교 대상 이미지
 * @param gridRows 행 그리드 수. 기본 8.
 * @param gridCols 열 그리드 수. 기본 8.
 * @return 4방향 회전 중 최대 유사도 (0.0 ~ 1.0)
 */
fun ImmutableImage.bestRotationSimilarityTo(
    other: ImmutableImage,
    gridRows: Int = BLOCK_MEAN_DEFAULT_GRID,
    gridCols: Int = BLOCK_MEAN_DEFAULT_GRID,
): Double {
    // 성능 최적화: other를 64×64로 축소 후 회전
    val target = other.scaleToMaxSide(64)
    val rot0 = target
    val rot90 = target.rotateLeft()    // 90° CCW
    val rot180 = rot90.rotateLeft()    // 180°
    val rot270 = rot180.rotateLeft()   // 270° CCW

    return listOf(rot0, rot90, rot180, rot270)
        .maxOf { blockMeanSimilarityTo(it, gridRows, gridCols) }
}
