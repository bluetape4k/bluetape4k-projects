package io.bluetape4k.math.ml

import io.bluetape4k.logging.KLogging
import io.bluetape4k.math.ml.distance.DistanceMeasureMethod
import io.bluetape4k.math.ml.neuralnet.computeHitHistogram
import io.bluetape4k.math.ml.neuralnet.computeQuantizationError
import io.bluetape4k.math.ml.neuralnet.computeU
import io.bluetape4k.math.ml.neuralnet.findBest
import io.bluetape4k.math.ml.neuralnet.findBestAndSecondBest
import io.bluetape4k.math.ml.neuralnet.sort
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeNull
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
        sorted.size shouldBeGreaterOrEqualTo 1
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
        error shouldBeGreaterOrEqualTo 0.0
    }

    @Test
    fun `computeU는 U-Matrix를 반환한다`() {
        val uMatrix = mesh.computeU(distance)
        uMatrix.shouldNotBeNull()
        uMatrix.size shouldBeGreaterOrEqualTo 1
    }

    @Test
    fun `computeHitHistogram은 히트 히스토그램을 반환한다`() {
        val data = listOf(
            doubleArrayOf(0.1, 0.1),
            doubleArrayOf(0.9, 0.9)
        )
        val histogram = mesh.computeHitHistogram(data, distance)
        histogram.shouldNotBeNull()
        histogram.size shouldBeGreaterOrEqualTo 1
    }
}
