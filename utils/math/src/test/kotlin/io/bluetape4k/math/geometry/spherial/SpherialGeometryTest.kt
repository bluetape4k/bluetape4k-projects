package io.bluetape4k.math.geometry.spherial

import io.bluetape4k.logging.KLogging
import io.bluetape4k.math.geometry.euclidean.vector3DOf
import io.bluetape4k.math.geometry.spherial.oned.arcOf
import io.bluetape4k.math.geometry.spherial.oned.arcsSetOf
import io.bluetape4k.math.geometry.spherial.oned.buildArcsSet
import io.bluetape4k.math.geometry.spherial.oned.toS1Point
import io.bluetape4k.math.geometry.spherial.twod.circleOf
import io.bluetape4k.math.geometry.spherial.twod.circlrOf
import io.bluetape4k.math.geometry.spherial.twod.copy
import io.bluetape4k.math.geometry.spherial.twod.s2PointOf
import io.bluetape4k.math.geometry.spherial.twod.toS2Point
import org.amshove.kluent.shouldBeNear
import org.amshove.kluent.shouldNotBeNull
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.junit.jupiter.api.Test
import kotlin.math.PI

class SpherialGeometryTest {

    companion object : KLogging()

    // --- Arc (S1D) ---

    @Test
    fun `arcOf로 구면 1D 호를 생성할 수 있다`() {
        val arc = arcOf(lower = 0.0, upper = PI, tolerance = 1e-10)
        arc.inf.shouldBeNear(0.0, 1e-10)
        arc.sup.shouldBeNear(PI, 1e-10)
    }

    // --- ArcsSet (S1D) ---

    @Test
    fun `arcsSetOf로 빈 ArcsSet을 생성할 수 있다`() {
        val arcsSet = arcsSetOf(tolerance = 1e-10)
        arcsSet.shouldNotBeNull()
    }

    @Test
    fun `arcsSetOf로 단일 호를 담은 ArcsSet을 생성할 수 있다`() {
        val arcsSet = arcsSetOf(lower = 0.0, upper = PI, tolerance = 1e-10)
        arcsSet.shouldNotBeNull()
        arcsSet.size.shouldBeNear(PI, 1e-10)
    }

    @Test
    fun `BSPTree로부터 ArcsSet을 생성할 수 있다`() {
        val source = arcsSetOf(lower = 0.0, upper = PI, tolerance = 1e-10)
        val tree = source.getTree(false)
        val arcsSet = tree.buildArcsSet(tolerance = 1e-10)
        arcsSet.shouldNotBeNull()
        arcsSet.size.shouldBeNear(PI, 1e-10)
    }

    // --- S1Point ---

    @Test
    fun `Double을 구면 1D 점으로 변환할 수 있다`() {
        val point = PI.toS1Point()
        point.alpha.shouldBeNear(PI, 1e-10)
    }

    @Test
    fun `Int를 구면 1D 점으로 변환할 수 있다`() {
        val point = 1.toS1Point()
        point.alpha.shouldBeNear(1.0, 1e-10)
    }

    // --- S2Point ---

    @Test
    fun `theta, phi로 구면 2D 점을 생성할 수 있다`() {
        val point = s2PointOf(theta = 0.0, phi = PI / 2)
        point.shouldNotBeNull()
        point.theta.shouldBeNear(0.0, 1e-10)
        point.phi.shouldBeNear(PI / 2, 1e-10)
    }

    @Test
    fun `3D 벡터를 구면 2D 점으로 변환할 수 있다`() {
        // (0, 0, 1) → north pole: phi = 0
        val v = Vector3D(0.0, 0.0, 1.0)
        val point = v.toS2Point()
        point.shouldNotBeNull()
        point.phi.shouldBeNear(0.0, 1e-10)
    }

    // --- Circle (S2D) ---

    @Test
    fun `극 벡터로 구면 2D 원을 생성할 수 있다`() {
        val pole = Vector3D(0.0, 0.0, 1.0)
        val circle = circleOf(pole = pole, tolerance = 1e-10)
        circle.shouldNotBeNull()
        circle.pole.z.shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `두 구면 점으로 대원을 생성할 수 있다`() {
        // phi != 0 인 두 다른 점 사용 (phi=0이면 north pole이라 두 점이 동일해짐)
        val p1 = s2PointOf(0.0, PI / 4)
        val p2 = s2PointOf(PI / 2, PI / 4)
        val circle = circlrOf(first = p1, second = p2, tolerance = 1e-10)
        circle.shouldNotBeNull()
    }

    @Test
    fun `Circle을 복사할 수 있다`() {
        val original = circleOf(pole = Vector3D(0.0, 0.0, 1.0), tolerance = 1e-10)
        val copy = original.copy()
        copy.shouldNotBeNull()
        copy.getPole().z.shouldBeNear(original.getPole().z, 1e-10)
    }
}
