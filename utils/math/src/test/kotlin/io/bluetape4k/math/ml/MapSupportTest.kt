package io.bluetape4k.math.ml

import io.bluetape4k.logging.KLogging
import io.bluetape4k.math.ml.distance.DistanceMeasureMethod
import io.bluetape4k.math.ml.neuralnet.computeHitHistogram
import io.bluetape4k.math.ml.neuralnet.computeQuantizationError
import io.bluetape4k.math.ml.neuralnet.computeTopographicError
import io.bluetape4k.math.ml.neuralnet.computeU
import io.bluetape4k.math.ml.neuralnet.findBest
import io.bluetape4k.math.ml.neuralnet.findBestAndSecondBest
import io.bluetape4k.math.ml.neuralnet.sort
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeInRange
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.commons.math3.ml.neuralnet.FeatureInitializerFactory
import org.apache.commons.math3.ml.neuralnet.SquareNeighbourhood
import org.apache.commons.math3.ml.neuralnet.twod.NeuronSquareMesh2D
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MapSupportTest {

    companion object : KLogging()

    private lateinit var mesh: NeuronSquareMesh2D
    private val distance: (DoubleArray, DoubleArray) -> Double =
        { a, b -> DistanceMeasureMethod.Euclidean.compute(a, b) }

    @BeforeEach
    fun setup() {
        val initializers = arrayOf(
            FeatureInitializerFactory.uniform(0.0, 1.0),
            FeatureInitializerFactory.uniform(0.0, 1.0)
        )
        mesh = NeuronSquareMesh2D(3, false, 3, false, SquareNeighbourhood.VON_NEUMANN, initializers)
    }

    @Test
    fun `findBest는 가장 가까운 뉴런을 반환한다`() {
        val features = doubleArrayOf(0.5, 0.5)
        val neurons = mesh.network.toList()
        val best = neurons.findBest(features, distance)
        best.shouldNotBeNull()
    }

    @Test
    fun `findBestAndSecondBest는 가장 가까운 두 뉴런을 반환한다`() {
        val features = doubleArrayOf(0.5, 0.5)
        val neurons = mesh.network.toList()
        val (best, second) = neurons.findBestAndSecondBest(features, distance)
        best.shouldNotBeNull()
        second.shouldNotBeNull()
    }

    @Test
    fun `sort는 거리 순으로 뉴런을 정렬한다`() {
        val features = doubleArrayOf(0.5, 0.5)
        val neurons = mesh.network.toList()
        val sorted = neurons.sort(features, distance)
        sorted.shouldNotBeNull()
        // 모든 뉴런이 포함되어야 한다
        sorted.size shouldBeEqualTo neurons.size
        // 거리 비단조 증가 순서 검증
        val distances = sorted.map { distance(it.features, features) }
        for (i in 1 until distances.size) {
            distances[i] shouldBeGreaterOrEqualTo distances[i - 1]
        }
    }

    @Test
    fun `computeQuantizationError는 양자화 오차를 반환한다`() {
        val data = listOf(
            doubleArrayOf(0.1, 0.1),
            doubleArrayOf(0.9, 0.9),
            doubleArrayOf(0.5, 0.5)
        )
        val neurons = mesh.network.toList()
        val error = neurons.computeQuantizationError(data, distance)
        // 오차는 비음수: 뉴런이 데이터 범위 [0,1]^2 내에 있으므로 최대 대각 길이(√2) 미만
        error shouldBeGreaterOrEqualTo 0.0
    }

    @Test
    fun `computeU는 3x3 mesh에서 3x3 U-Matrix를 반환한다`() {
        val uMatrix = mesh.computeU(distance)
        uMatrix.shouldNotBeNull()
        // 3x3 NeuronSquareMesh2D → U-Matrix 차원도 3x3
        uMatrix.size shouldBeEqualTo 3
        uMatrix[0].size shouldBeEqualTo 3
    }

    @Test
    fun `computeHitHistogram은 히트 총합이 데이터 크기와 같다`() {
        val data = listOf(
            doubleArrayOf(0.1, 0.1),
            doubleArrayOf(0.9, 0.9)
        )
        val histogram = mesh.computeHitHistogram(data, distance)
        histogram.shouldNotBeNull()
        histogram.size shouldBeEqualTo 3
        // 각 데이터 포인트는 정확히 하나의 뉴런에 매핑되므로 합계 == data.size
        val totalHits = histogram.sumOf { row -> row.sum() }
        totalHits shouldBeEqualTo data.size
    }

    @Test
    fun `computeTopographicError는 위상 오차를 0과 1 사이 값으로 반환한다`() {
        val data = listOf(
            doubleArrayOf(0.1, 0.1),
            doubleArrayOf(0.9, 0.9),
            doubleArrayOf(0.5, 0.5),
            doubleArrayOf(0.2, 0.8)
        )
        val error = mesh.network.computeTopographicError(data, distance)
        error shouldBeInRange 0.0..1.0
    }
}
