package io.bluetape4k.math.geometry.spherial.oned

import org.apache.commons.math3.geometry.partitioning.BSPTree
import org.apache.commons.math3.geometry.spherical.oned.ArcsSet
import org.apache.commons.math3.geometry.spherical.oned.Sphere1D

/**
 * 허용 오차로 빈 ArcsSet을 생성합니다.
 *
 * ```kotlin
 * val arcsSet = arcsSetOf(tolerance = 1e-10)
 * ```
 */
fun arcsSetOf(tolerance: Double): ArcsSet = ArcsSet(tolerance)

/**
 * 하한과 상한, 허용 오차로 단일 호를 담은 ArcsSet을 생성합니다.
 *
 * ```kotlin
 * val arcsSet = arcsSetOf(lower = 0.0, upper = Math.PI, tolerance = 1e-10)
 * ```
 */
fun arcsSetOf(lower: Double, upper: Double, tolerance: Double): ArcsSet = ArcsSet(lower, upper, tolerance)

/**
 * BSPTree로부터 ArcsSet을 생성합니다.
 *
 * ```kotlin
 * val tree: BSPTree<Sphere1D> = ...
 * val arcsSet = tree.buildArcsSet(tolerance = 1e-10)
 * ```
 */
fun BSPTree<Sphere1D>.buildArcsSet(tolerance: Double): ArcsSet = ArcsSet(this, tolerance)
