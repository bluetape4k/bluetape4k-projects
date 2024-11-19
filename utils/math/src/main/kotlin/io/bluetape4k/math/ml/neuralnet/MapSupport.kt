package io.bluetape4k.math.ml.neuralnet

import org.apache.commons.math3.ml.neuralnet.MapUtils
import org.apache.commons.math3.ml.neuralnet.Network
import org.apache.commons.math3.ml.neuralnet.Neuron
import org.apache.commons.math3.ml.neuralnet.twod.NeuronSquareMesh2D

/**
 * Finds the neuron that best matches the given features.
 */
fun Iterable<Neuron>.findBest(features: DoubleArray, distance: (DoubleArray, DoubleArray) -> Double): Neuron {
    return MapUtils.findBest(features, this, distance)
}

/**
 * Finds the two neurons that best match the given features.
 *
 * @param features Data.
 * @param distance Distance function. The neuron's features are passed as the first argument to
 * [DistanceMeasure.compute(double[],double[])].
 * @receiver List of neurons to scan. If the list is empty [null] will be returned.
 * @return the two neurons whose features are closest to the given data.
 */
fun Iterable<Neuron>.findBestAndSecondBest(
    features: DoubleArray,
    distance: (DoubleArray, DoubleArray) -> Double,
): Pair<Neuron, Neuron> {
    val pair = MapUtils.findBestAndSecondBest(features, this, distance)
    return pair.first to pair.second
}

fun Iterable<Neuron>.sort(
    features: DoubleArray,
    distance: (DoubleArray, DoubleArray) -> Double,
): Array<Neuron> {
    return MapUtils.sort(features, this, distance)
}

fun Iterable<Neuron>.computeQuantizationError(
    data: Iterable<DoubleArray>,
    distance: (DoubleArray, DoubleArray) -> Double,
): Double {
    return MapUtils.computeQuantizationError(data, this, distance)
}

fun NeuronSquareMesh2D.computeU(distance: (DoubleArray, DoubleArray) -> Double): Array<DoubleArray> {
    return MapUtils.computeU(this, distance)
}

fun NeuronSquareMesh2D.computeHitHistogram(
    data: Iterable<DoubleArray>,
    distance: (DoubleArray, DoubleArray) -> Double,
): Array<IntArray> {
    return MapUtils.computeHitHistogram(data, this, distance)
}

fun Network.computeTopographicError(
    data: Iterable<DoubleArray>,
    distance: (DoubleArray, DoubleArray) -> Double,
): Double {
    return MapUtils.computeTopographicError(data, this, distance)
}
