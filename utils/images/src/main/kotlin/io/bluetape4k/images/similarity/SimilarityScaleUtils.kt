package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage

/**
 * 유사도 계산 전 이미지를 최대 [maxSide]px로 비율 유지 축소합니다.
 *
 * 이미 [maxSide] 이하이면 원본 반환(복사 없음).
 *
 * ## 크기별 권장 설정
 *
 * | 이미지 크기 | 알고리즘 | 권장 maxSide |
 * |---|---|---|
 * | ≤ 256px | `mssimTo` 직접 호출 | — |
 * | 256px ~ 800px | `mssimTo` | 800 |
 * | 800px ~ 4K | `histogramSimilarityTo` / `blockMeanSimilarityTo` | 512 |
 * | 4K+ | hash 계열 (`ahash`, `dhash`, `phash`) | — (내부 리사이즈) |
 *
 * ## 사용 예시
 *
 * ```kotlin
 * // MSSIM 전 다운스케일 (4K 이미지를 800px로 축소)
 * val score = img.prepareForSimilarity(800).mssimTo(other.prepareForSimilarity(800))
 *
 * // 히스토그램 — 크기 무관하지만 속도 향상 시
 * val sim = img.prepareForSimilarity(512)
 *     .histogramSimilarityTo(other.prepareForSimilarity(512))
 * ```
 *
 * @param maxSide 긴 변 최대 픽셀 수. 기본 512.
 * @return 다운스케일된 이미지 또는 원본
 */
fun ImmutableImage.prepareForSimilarity(maxSide: Int = 512): ImmutableImage =
    scaleToMaxSide(maxSide)
