package io.bluetape4k.images.analysis

import com.sksamuel.scrimage.ImmutableImage
/**
 * Median Cut quantization 알고리즘으로 이미지의 대표 색상을 추출한다.
 *
 * color-thief-java (MIT) 알고리즘을 Kotlin으로 자체 구현.
 * 5-bit/channel (32 levels) RGB 색공간을 사용하여 메모리를 절약한다.
 */
internal object MedianCutQuantizer {

    private const val SIGNAL_BITS = 5
    private const val RIGHT_SHIFT = 8 - SIGNAL_BITS  // = 3
    private const val MULT = 1 shl RIGHT_SHIFT        // = 8
    private const val HIST_SIZE = 1 shl (3 * SIGNAL_BITS) // 32^3 = 32768

    /**
     * 이미지에서 [count]개의 대표 색상을 추출한다.
     *
     * @param image 분석할 이미지
     * @param count 추출할 색상 수 (결과는 이 수보다 적을 수 있음 — 단색 이미지 등)
     * @param quality 픽셀 샘플링 간격 (1=모든 픽셀)
     * @param ignoreWhite true이면 흰색에 가까운 픽셀 제외
     * @return population 내림차순 정렬된 DominantColor 목록 (빈 이미지면 emptyList)
     */
    fun quantize(
        image: ImmutableImage,
        count: Int,
        quality: Int = 10,
        ignoreWhite: Boolean = false,
    ): List<DominantColor> {
        val pixels = collectPixels(image, quality, ignoreWhite)
        if (pixels.isEmpty()) return emptyList()

        val histogram = buildHistogram(pixels)
        val boxes = mutableListOf(buildInitialBox(histogram))

        // count개 박스가 될 때까지 분할
        repeat(count - 1) {
            val boxToSplit = boxes.maxByOrNull { it.count }
                ?.takeIf { it.count > 1 && it.volume > 0 }
                ?: return@repeat
            boxes.remove(boxToSplit)
            val (box1, box2) = splitBox(boxToSplit, histogram)
            boxes.add(box1)
            boxes.add(box2)
        }

        return boxes
            .map { averageColor(it, histogram) }
            .filter { it.population > 0 }
            .sortedByDescending { it.population }
    }

    /** 이미지에서 유효한 픽셀의 (r,g,b) 목록을 수집한다 (5-bit 양자화). */
    private fun collectPixels(
        image: ImmutableImage,
        quality: Int,
        ignoreWhite: Boolean,
    ): List<IntArray> {
        val pixelArray = image.pixels()
        val result = mutableListOf<IntArray>()
        var i = 0
        while (i < pixelArray.size) {
            val pixel = pixelArray[i]
            val alpha = pixel.alpha()
            val r = pixel.red()
            val g = pixel.green()
            val b = pixel.blue()

            if (alpha > 125) {
                if (!ignoreWhite || !(r > 250 && g > 250 && b > 250)) {
                    result.add(intArrayOf(r, g, b))
                }
            }
            i += quality
        }
        return result
    }

    /** 5-bit 색공간 히스토그램을 빌드한다. index → pixel count */
    private fun buildHistogram(pixels: List<IntArray>): IntArray {
        val hist = IntArray(HIST_SIZE)
        for ((r, g, b) in pixels) {
            val index = colorIndex(r, g, b)
            hist[index]++
        }
        return hist
    }

    private fun colorIndex(r: Int, g: Int, b: Int): Int =
        ((r shr RIGHT_SHIFT) shl (2 * SIGNAL_BITS)) or
            ((g shr RIGHT_SHIFT) shl SIGNAL_BITS) or
            (b shr RIGHT_SHIFT)

    /** 히스토그램 전체를 포괄하는 초기 박스를 생성한다. */
    private fun buildInitialBox(histogram: IntArray): ColorBox {
        var rMin = 32; var rMax = 0
        var gMin = 32; var gMax = 0
        var bMin = 32; var bMax = 0
        var count = 0

        for (i in 0 until HIST_SIZE) {
            if (histogram[i] == 0) continue
            val r = (i shr (2 * SIGNAL_BITS)) and 0x1F
            val g = (i shr SIGNAL_BITS) and 0x1F
            val b = i and 0x1F
            count += histogram[i]
            if (r < rMin) rMin = r; if (r > rMax) rMax = r
            if (g < gMin) gMin = g; if (g > gMax) gMax = g
            if (b < bMin) bMin = b; if (b > bMax) bMax = b
        }

        return ColorBox(rMin, rMax, gMin, gMax, bMin, bMax, count)
    }

    /** 박스를 가장 긴 축의 중앙에서 분할한다. */
    private fun splitBox(box: ColorBox, histogram: IntArray): Pair<ColorBox, ColorBox> {
        val rRange = box.rMax - box.rMin
        val gRange = box.gMax - box.gMin
        val bRange = box.bMax - box.bMin

        return when {
            rRange >= gRange && rRange >= bRange -> splitAlongAxis(box, histogram, Axis.R)
            gRange >= bRange -> splitAlongAxis(box, histogram, Axis.G)
            else -> splitAlongAxis(box, histogram, Axis.B)
        }
    }

    private enum class Axis { R, G, B }

    private fun splitAlongAxis(box: ColorBox, histogram: IntArray, axis: Axis): Pair<ColorBox, ColorBox> {
        val lookAheadSum = IntArray(32)
        var sum = 0

        val (start, end) = when (axis) {
            Axis.R -> box.rMin to box.rMax
            Axis.G -> box.gMin to box.gMax
            Axis.B -> box.bMin to box.bMax
        }

        for (val_ in start..end) {
            val dimSum = countInSlice(box, histogram, axis, val_)
            sum += dimSum
            lookAheadSum[val_] = sum
        }

        val total = sum
        val half = total / 2

        var splitAt = start
        var cumSum = 0
        for (val_ in start..end) {
            cumSum = lookAheadSum[val_]
            if (cumSum >= half) {
                splitAt = val_
                break
            }
        }

        // 분할점이 start와 같으면 최소 1 이동
        if (splitAt == start) splitAt = minOf(start + 1, end)

        val box1 = when (axis) {
            Axis.R -> box.copy(rMax = splitAt)
            Axis.G -> box.copy(gMax = splitAt)
            Axis.B -> box.copy(bMax = splitAt)
        }
        val box2 = when (axis) {
            Axis.R -> box.copy(rMin = splitAt + 1)
            Axis.G -> box.copy(gMin = splitAt + 1)
            Axis.B -> box.copy(bMin = splitAt + 1)
        }

        return Pair(
            box1.copy(count = countInBox(box1, histogram)),
            box2.copy(count = countInBox(box2, histogram)),
        )
    }

    private fun countInSlice(box: ColorBox, histogram: IntArray, axis: Axis, val_: Int): Int {
        var count = 0
        when (axis) {
            Axis.R -> for (g in box.gMin..box.gMax) for (b in box.bMin..box.bMax)
                count += histogram[colorIndex(val_ shl RIGHT_SHIFT, g shl RIGHT_SHIFT, b shl RIGHT_SHIFT)]
            Axis.G -> for (r in box.rMin..box.rMax) for (b in box.bMin..box.bMax)
                count += histogram[colorIndex(r shl RIGHT_SHIFT, val_ shl RIGHT_SHIFT, b shl RIGHT_SHIFT)]
            Axis.B -> for (r in box.rMin..box.rMax) for (g in box.gMin..box.gMax)
                count += histogram[colorIndex(r shl RIGHT_SHIFT, g shl RIGHT_SHIFT, val_ shl RIGHT_SHIFT)]
        }
        return count
    }

    private fun countInBox(box: ColorBox, histogram: IntArray): Int {
        var count = 0
        for (r in box.rMin..box.rMax)
            for (g in box.gMin..box.gMax)
                for (b in box.bMin..box.bMax)
                    count += histogram[colorIndex(r shl RIGHT_SHIFT, g shl RIGHT_SHIFT, b shl RIGHT_SHIFT)]
        return count
    }

    /** 박스 내 평균 색상을 계산한다. */
    private fun averageColor(box: ColorBox, histogram: IntArray): DominantColor {
        var rSum = 0L; var gSum = 0L; var bSum = 0L; var total = 0
        for (r in box.rMin..box.rMax) {
            for (g in box.gMin..box.gMax) {
                for (b in box.bMin..box.bMax) {
                    val hval = histogram[colorIndex(r shl RIGHT_SHIFT, g shl RIGHT_SHIFT, b shl RIGHT_SHIFT)]
                    if (hval > 0) {
                        rSum += hval.toLong() * (r * MULT + MULT / 2)
                        gSum += hval.toLong() * (g * MULT + MULT / 2)
                        bSum += hval.toLong() * (b * MULT + MULT / 2)
                        total += hval
                    }
        }}}
        return if (total == 0) {
            // 박스에 픽셀이 없는 경우 (단색 이미지 분할 시 rMin > rMax 가능) — coerceIn 으로 방어
            DominantColor(
                r = (box.rMin * MULT).coerceIn(0, 255),
                g = (box.gMin * MULT).coerceIn(0, 255),
                b = (box.bMin * MULT).coerceIn(0, 255),
                population = 0,
            )
        } else {
            DominantColor(
                r = (rSum / total).toInt().coerceIn(0, 255),
                g = (gSum / total).toInt().coerceIn(0, 255),
                b = (bSum / total).toInt().coerceIn(0, 255),
                population = total,
            )
        }
    }

    /** Median Cut 과정에서 사용하는 RGB 색 공간 내 박스 (5-bit 색 공간 기준). */
    private data class ColorBox(
        val rMin: Int, val rMax: Int,
        val gMin: Int, val gMax: Int,
        val bMin: Int, val bMax: Int,
        val count: Int,
    ) {
        val volume: Int get() = (rMax - rMin + 1) * (gMax - gMin + 1) * (bMax - bMin + 1)
    }
}
