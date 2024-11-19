@file:JvmName("Descriptives")

package io.bluetape4k.math

import org.apache.commons.math3.exception.MathIllegalArgumentException
import org.apache.commons.math3.exception.MathIllegalStateException
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

/**
 * Maintains a dataset of values of a single variable and computes descriptive
 * statistics based on stored data. The {@link #getWindowSize() windowSize}
 * property sets a limit on the number of values that can be stored in the
 * dataset.  The default value, INFINITE_WINDOW, puts no limit on the size of
 * the dataset.  This value should be used with caution, as the backing store
 * will grow without bound in this case.  For very large datasets,
 * {@link SummaryStatistics}, which does not store the dataset, should be used
 * instead of this class. If <code>windowSize</code> is not INFINITE_WINDOW and
 * more values are added than can be stored in the dataset, new values are
 * added in a "rolling" manner, with new values replacing the "oldest" values
 * in the dataset.
 */
interface Descriptives {
    /**
     * Returns the maximum number of values that can be stored in the
     * dataset, or INFINITE_WINDOW (-1) if there is no limit.
     *
     * @return The current window size or -1 if its Infinite.
     */
    val windowSize: Int

    /**
     * Returns the <a href="http://www.xycoon.com/arithmetic_mean.htm">
     * arithmetic mean </a> of the available values
     * @return The mean or Double.NaN if no values have been added.
     */
    val mean: Double

    /**
     * Returns the <a href="http://www.xycoon.com/geometric_mean.htm">
     * geometric mean </a> of the available values.
     * <p>
     * See {@link GeometricMean} for details on the computing algorithm.</p>
     *
     * @return The geometricMean, Double.NaN if no values have been added,
     * or if any negative values have been added.
     */
    val geometricMean: Double

    /**
     * Returns the (sample) variance of the available values.
     *
     * <p>This method returns the bias-corrected sample variance (using {@code n - 1} in
     * the denominator).  Use {@link #getPopulationVariance()} for the non-bias-corrected
     * population variance.</p>
     *
     * @return The variance, Double.NaN if no values have been added
     * or 0.0 for a single value set.
     */
    val variance: Double

    /**
     * Returns the standard deviation of the available values.
     * @return The standard deviation, Double.NaN if no values have been added
     * or 0.0 for a single value set.
     */
    val standardDeviation: Double

    /**
     * Returns the skewness of the available values. Skewness is a
     * measure of the asymmetry of a given distribution.
     *
     * @return The skewness, Double.NaN if less than 3 values have been added.
     */
    val skewness: Double

    /**
     * Returns the Kurtosis of the available values. Kurtosis is a
     * measure of the "peakedness" of a distribution.
     *
     * @return The kurtosis, Double.NaN if less than 4 values have been added.
     */
    val kurtosis: Double

    /**
     * Returns the maximum of the available values
     * @return The max or Double.NaN if no values have been added.
     */
    val max: Double

    /**
     * Returns the minimum of the available values
     * @return The min or Double.NaN if no values have been added.
     */
    val min: Double

    /**
     * Returns the number of available values
     * @return The number of available values
     */
    val size: Long

    /**
     * Returns the sum of the values that have been added to Univariate.
     * @return The sum or Double.NaN if no values have been added
     */
    val sum: Double

    /**
     * Returns the sum of the squares of the available values.
     * @return The sum of the squares or Double.NaN if no values have been added.
     */
    val sumSquared: Double

    /**
     * Returns the current set of values in an array of double primitives.
     * The order of addition is preserved.  The returned array is a fresh
     * copy of the underlying data -- i.e., it is not a reference to the
     * stored data.
     *
     * @return returns the current set of numbers in the order in which they
     *         were added to this set
     */
    val values: DoubleArray

    /**
     * Returns an estimate for the pth percentile of the stored values.
     * <p>
     * The implementation provided here follows the first estimation procedure presented
     * <a href="http://www.itl.nist.gov/div898/handbook/prc/section2/prc252.htm">here.</a>
     * </p><p>
     * <strong>Preconditions</strong>:<ul>
     * <li><code>0 &lt; p &le; 100</code> (otherwise an
     * <code>MathIllegalArgumentException</code> is thrown)</li>
     * <li>at least one value must be stored (returns <code>Double.NaN
     *     </code> otherwise)</li>
     * </ul></p>
     *
     * @param percentile the requested percentile (scaled from 0 - 100)
     * @return An estimate for the pth percentile of the stored data
     *
     * @throws MathIllegalStateException if percentile implementation has been overridden
     *         and the supplied implementation does not support setQuantile
     * @throws MathIllegalArgumentException if p is not a valid quantile
     */
    fun percentile(percentile: Double): Double

    /**
     * Returns the element at the specified index
     * @param index The Index of the element
     * @return return the element at the specified index
     */
    operator fun get(index: Int): Double
}

/**
 * Apache math 의 `DescriptiveStatistics`를 Wrapping하여,
 * 필요한 속성만 지연 방식으로 값을 계산하게 합니다.
 */
internal class ApacheDescriptives(private val ds: DescriptiveStatistics): Descriptives {

    override val windowSize by lazy { ds.windowSize }
    override val mean by lazy { ds.mean }
    override val geometricMean by lazy { ds.geometricMean }
    override val variance by lazy { ds.variance }
    override val standardDeviation by lazy { ds.standardDeviation }
    override val skewness by lazy { ds.skewness }
    override val kurtosis by lazy { ds.kurtosis }
    override val max by lazy { ds.max }
    override val min by lazy { ds.min }
    override val size by lazy { ds.n }
    override val sum by lazy { ds.sum }
    override val sumSquared by lazy { ds.sumsq }
    override val values: DoubleArray by lazy { ds.values }
    override fun percentile(percentile: Double) = ds.getPercentile(percentile)
    override operator fun get(index: Int) = ds.getElement(index)

}
