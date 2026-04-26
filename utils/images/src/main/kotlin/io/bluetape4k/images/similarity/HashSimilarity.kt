package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage

/**
 * 가변 비트폭 해시 계열 유사도 함수 모음.
 *
 * ## 제공 해시
 *
 * | 함수 | 알고리즘 | 특징 |
 * |---|---|---|
 * | [ahashOf] / [ahash] | Average Hash | 평균 휘도 기준 비트화. 단순/빠름 |
 * | [dhashOf] / [dhash] | Difference Hash | 인접 픽셀 그래디언트. 약한 밝기 변화에 견고 |
 * | [phashOf] | DCT pHash (가변 비트) | 저주파 DCT 계수 기반. 리사이즈/JPEG에 견고 |
 * | [whashOf] / [whash] | Haar Wavelet Hash | DWT LL subband 기반. pHash와 유사하나 더 빠름 |
 *
 * 모든 해시는 [LongArray] 형태로 반환되어 64bit / 256bit / 1024bit 비트폭을 지원합니다.
 * Hamming distance 비교는 [HashDistance.hamming] 사용.
 *
 * ```kotlin
 * val a = immutableImageOf(File("a.jpg"))
 * val b = immutableImageOf(File("b.jpg"))
 *
 * // 64bit 편의 단축형 (Long)
 * val da = HashDistance.hamming(a.ahash(), b.ahash())
 *
 * // 256bit 정밀 비교 (LongArray[4])
 * val dp = HashDistance.hamming(a.phashOf(PHashSize.BITS_256), b.phashOf(PHashSize.BITS_256))
 * ```
 */

/**
 * aHash / dHash의 비트폭 옵션.
 *
 * @property bits 해시 비트 수
 * @property gridSide N×N 그리드의 한 변 크기. 비트 수 = gridSide².
 */
enum class HashSize(val bits: Int, internal val gridSide: Int) {
    /** 64bit 해시 (8×8 그리드). */
    BITS_64(64, 8),

    /** 256bit 해시 (16×16 그리드). */
    BITS_256(256, 16),

    /** 1024bit 해시 (32×32 그리드). */
    BITS_1024(1024, 32);

    /** 비트를 담는 데 필요한 [Long] 슬롯 개수. */
    internal val longCount: Int get() = (bits + 63) / 64
}

/**
 * DCT pHash / Haar wHash 비트폭 옵션.
 *
 * @property bits 해시 비트 수
 * @property resize 변환 전 리사이즈 크기 (resize × resize)
 * @property lowSide 저주파 블록의 한 변 크기. 비트 수 = lowSide².
 */
enum class PHashSize(val bits: Int, internal val resize: Int, internal val lowSide: Int) {
    /** 64bit 해시 (resize 32×32 → low 8×8). 기존 [phash]()와 동일. */
    BITS_64(64, 32, 8),

    /** 256bit 해시 (resize 64×64 → low 16×16). */
    BITS_256(256, 64, 16),

    /** 1024bit 해시 (resize 128×128 → low 32×32). */
    BITS_1024(1024, 128, 32);

    init {
        // resize/lowSide 는 2의 거듭제곱이어야 DWT levels = log2(resize/lowSide) 가 정확히 계산됩니다.
        require(resize % lowSide == 0 && Integer.bitCount(resize / lowSide) == 1) {
            "resize/lowSide 는 2의 거듭제곱이어야 합니다: $resize/$lowSide"
        }
    }

    /** 비트를 담는 데 필요한 [Long] 슬롯 개수. */
    internal val longCount: Int get() = (bits + 63) / 64
}

/**
 * 평균 해시(aHash)를 계산합니다.
 *
 * 알고리즘: [size].gridSide × [size].gridSide 리사이즈 → 그레이스케일 →
 * 전체 평균값 기준으로 각 픽셀을 0/1 비트화 → [LongArray].
 *
 * JPEG 압축·밝기 변화에 견고합니다. dHash/pHash 대비 단순하지만 가장 빠릅니다.
 *
 * 비트 순서: row-major, LSB = first bit.
 *
 * @param size 비트폭. 기본 [HashSize.BITS_64] (64bit, 8×8 그리드).
 * @return 비트 배열. BITS_64 → LongArray(1), BITS_256 → LongArray(4), BITS_1024 → LongArray(16).
 */
fun ImmutableImage.ahashOf(size: HashSize = HashSize.BITS_64): LongArray {
    val side = size.gridSide
    val scaled = scaleTo(side, side, HASH_SCALE_METHOD)
    val lum = Array(side) { y -> DoubleArray(side) { x -> luminance(scaled.pixel(x, y)) } }
    val avg = lum.sumOf { row -> row.sum() } / (side * side)
    return bitsToLongArray(
        bits = size.bits,
        longCount = size.longCount,
        isSet = { idx -> lum[idx / side][idx % side] >= avg }
    )
}

/**
 * 64bit aHash 편의 단축형.
 *
 * `ahashOf(HashSize.BITS_64)[0]`과 동일.
 */
fun ImmutableImage.ahash(): Long = ahashOf(HashSize.BITS_64)[0]

/**
 * 차이 해시(dHash)를 계산합니다.
 *
 * 알고리즘: ([size].gridSide + 1) × [size].gridSide 리사이즈 → 그레이스케일 →
 * 같은 행 내 인접 픽셀(좌→우) 비교: 왼쪽 < 오른쪽이면 비트 1 → [LongArray].
 *
 * 그래디언트 기반으로 JPEG 압축·약한 밝기 변화에 aHash보다 견고합니다.
 *
 * 비트 순서: row-major, LSB = first bit.
 *
 * @param size 비트폭. 기본 [HashSize.BITS_64].
 * @return 비트 배열.
 */
fun ImmutableImage.dhashOf(size: HashSize = HashSize.BITS_64): LongArray {
    val cols = size.gridSide
    val rows = size.gridSide
    val scaled = scaleTo(cols + 1, rows, HASH_SCALE_METHOD)
    val lum = Array(rows) { y -> DoubleArray(cols + 1) { x -> luminance(scaled.pixel(x, y)) } }
    return bitsToLongArray(
        bits = size.bits,
        longCount = size.longCount,
        isSet = { idx ->
            val row = idx / cols
            val col = idx % cols
            lum[row][col] < lum[row][col + 1]
        }
    )
}

/**
 * 64bit dHash 편의 단축형.
 *
 * `dhashOf(HashSize.BITS_64)[0]`과 동일.
 */
fun ImmutableImage.dhash(): Long = dhashOf(HashSize.BITS_64)[0]

/**
 * Haar 웨이블릿 해시(wHash)를 계산합니다.
 *
 * 알고리즘: [size].resize × [size].resize 리사이즈 → 그레이스케일 →
 * `log2(resize/lowSide)`-level Haar DWT → [size].lowSide × [size].lowSide LL subband 추출 →
 * 평균 기준 비트화 → [LongArray].
 *
 * DCT pHash와 유사하나 Haar 변환을 사용해 더 빠릅니다.
 * BITS_64: scaleTo(32, 32) → DWT 2-level → 8×8 LL subband = 64bit.
 *
 * 비트 순서: row-major, LSB = first bit.
 *
 * @param size 비트폭. 기본 [PHashSize.BITS_64].
 * @return 비트 배열.
 */
fun ImmutableImage.whashOf(size: PHashSize = PHashSize.BITS_64): LongArray {
    val scaled = scaleTo(size.resize, size.resize, HASH_SCALE_METHOD)
    val matrix = Array(size.resize) { y ->
        DoubleArray(size.resize) { x -> luminance(scaled.pixel(x, y)) / PIXEL_MAX }
    }
    // levels = log2(resize/lowSide): BITS_64 → log2(4)=2, BITS_256 → 2, BITS_1024 → 2
    val levels = Integer.numberOfTrailingZeros(size.resize / size.lowSide)
    haarTransform2d(matrix, levels = levels)

    val low = size.lowSide
    val coeffs = DoubleArray(low * low) { idx -> matrix[idx / low][idx % low] }
    val avg = coeffs.average()

    return bitsToLongArray(
        bits = size.bits,
        longCount = size.longCount,
        isSet = { idx -> coeffs[idx] >= avg }
    )
}

/**
 * 64bit wHash 편의 단축형.
 *
 * `whashOf(PHashSize.BITS_64)[0]`과 동일.
 */
fun ImmutableImage.whash(): Long = whashOf(PHashSize.BITS_64)[0]

/**
 * DCT 지각 해시(pHash) 가변 비트폭 버전.
 *
 * 알고리즘: [size].resize × [size].resize 리사이즈 → 그레이스케일 →
 * 2D DCT-II → [size].lowSide × [size].lowSide 저주파 블록 추출 →
 * DC 성분(`coeffs[0]`) 제외 평균 기준 비트화 → [LongArray].
 *
 * 비트 순서: row-major, LSB = first bit.
 * 불변식: `phashOf(PHashSize.BITS_64)[0] == phash()` (기존 64bit pHash와 동일 결과).
 *
 * @param size 비트폭. [PHashSize.BITS_64] 시 기존 [phash]()와 결과 동일.
 * @return 비트 배열.
 */
fun ImmutableImage.phashOf(size: PHashSize = PHashSize.BITS_64): LongArray {
    val scaled = scaleTo(size.resize, size.resize, HASH_SCALE_METHOD)
    val gray = Array(size.resize) { y -> DoubleArray(size.resize) { x -> luminance(scaled.pixel(x, y)) } }
    val dct = dct2d(gray, size.resize)

    val low = size.lowSide
    val totalBits = low * low
    val coeffs = DoubleArray(totalBits) { idx -> dct[idx / low][idx % low] }

    // DC 성분(coeffs[0]) 제외 평균 — 기존 phash()와 동일 규칙
    var avg = 0.0
    for (i in 1 until totalBits) avg += coeffs[i]
    avg /= (totalBits - 1)

    return bitsToLongArray(
        bits = size.bits,
        longCount = size.longCount,
        isSet = { idx -> coeffs[idx] > avg }
    )
}

/**
 * 두 이미지의 [phashOf] 결과 사이의 Hamming distance를 반환합니다.
 *
 * @param other 비교 대상 이미지
 * @param size 비트폭. 기본 [PHashSize.BITS_64].
 * @return Hamming distance (0 ~ [PHashSize.bits])
 */
fun ImmutableImage.phashOfDistanceTo(
    other: ImmutableImage,
    size: PHashSize = PHashSize.BITS_64,
): Int = HashDistance.hamming(phashOf(size), other.phashOf(size))

/**
 * 해시 배열 간 Hamming distance 계산 유틸리티.
 *
 * 64bit 단일 해시는 [Long] 오버로드, 가변 비트폭 해시는 [LongArray] 오버로드를 사용합니다.
 */
object HashDistance {

    /**
     * 두 64bit 해시의 Hamming distance.
     *
     * @param a 첫 번째 해시
     * @param b 두 번째 해시
     * @return 서로 다른 비트 수 (0 ~ 64)
     */
    fun hamming(a: Long, b: Long): Int = (a xor b).countOneBits()

    /**
     * 두 [LongArray] 해시의 Hamming distance.
     *
     * @param a 첫 번째 해시
     * @param b 두 번째 해시
     * @return 서로 다른 비트 수 (0 ~ a.size × 64)
     * @throws IllegalArgumentException 두 배열의 길이가 다를 때
     */
    fun hamming(a: LongArray, b: LongArray): Int {
        require(a.size == b.size) { "해시 배열 길이가 동일해야 합니다: ${a.size} vs ${b.size}" }
        var sum = 0
        for (i in a.indices) {
            sum += (a[i] xor b[i]).countOneBits()
        }
        return sum
    }
}

/**
 * 비트 배열을 [LongArray]로 인코딩합니다. row-major, LSB = first bit.
 *
 * @param bits 전체 비트 수
 * @param longCount 결과 [LongArray] 크기 (= ceil(bits / 64))
 * @param isSet 인덱스 i (0 ≤ i < bits) 가 1인지 여부를 반환하는 함수
 */
private inline fun bitsToLongArray(
    bits: Int,
    longCount: Int,
    isSet: (Int) -> Boolean,
): LongArray {
    val result = LongArray(longCount)
    for (i in 0 until bits) {
        if (isSet(i)) {
            result[i / 64] = result[i / 64] or (1L shl (i % 64))
        }
    }
    return result
}
