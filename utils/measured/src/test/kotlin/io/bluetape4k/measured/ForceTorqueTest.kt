package io.bluetape4k.measured

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

@RandomizedTest
class ForceTorqueTest {

    companion object: KLogging()

    @Test
    fun `질량과 가속도로 힘을 계산한다`() {
        val force = 1.kilograms() * 1.metersPerSecondSquared()

        (force `in` Force.newtons).shouldBeNear(1.0, 1e-10)
        val mixedScale = 1000.grams() * 1.metersPerSecondSquared()
        (mixedScale `in` Force.newtons).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `힘과 가속도로 질량을 역산한다`() {
        val mass = 1.newtons() / 1.metersPerSecondSquared()

        (mass `in` Mass.kilograms).shouldBeNear(1.0, 1e-10)
        (1000.newtons() / 10.metersPerSecondSquared() `in` Mass.kilograms).shouldBeNear(100.0, 1e-8)
    }

    @Test
    fun `힘의 SI 접두어와 혼합 스케일을 변환한다`() {
        (1.kiloNewtons() `in` Force.newtons).shouldBeNear(1000.0, 1e-10)
        (1000.milliNewtons() `in` Force.newtons).shouldBeNear(1.0, 1e-10)
        (1.megaNewtons() `in` Force.kiloNewtons).shouldBeNear(1000.0, 1e-8)
        1500.newtons().toHuman() shouldBeEqualTo "1.5 kN"
    }

    @Test
    fun `수직 모멘트암을 명시해 토크를 계산한다`() {
        val torque: Measure<Torque> = 1.kiloNewtons().torqueAt(2.meters())

        (torque `in` Torque.newtonMeters).shouldBeNear(2_000.0, 1e-8)
        (torque `in` Torque.kiloNewtonMeters).shouldBeNear(2.0, 1e-10)
        torque.toHuman() shouldBeEqualTo "2.0 kN·m"
    }

    @Test
    fun `토크와 에너지는 정적 의미 타입과 표시를 분리한다`() {
        val torque: Measure<Torque> = 2.newtonMeters()
        val energy: Measure<Energy> = 2.joules()

        torque.toHuman() shouldBeEqualTo "2.0 N·m"
        energy.toHuman() shouldBeEqualTo "2.0 J"
        (torque `in` Torque.newtonMeters).shouldBeNear(2.0, 1e-10)
        (energy `in` Energy.joules).shouldBeNear(2.0, 1e-10)
    }

    @Test
    fun `일반 힘과 길이 곱은 토크 의미 타입으로 자동 축약되지 않는다`() {
        val product = 2.newtons() * 3.meters()

        product.units shouldBeEqualTo Force.newtons * Length.meters
        (product `in` (Force.newtons * Length.meters)).shouldBeNear(6.0, 1e-10)
    }

    @Test
    fun `토크 단위의 사칙연산과 음수 표시가 동작한다`() {
        val torque = 1.kiloNewtonMeters()

        ((torque + torque) `in` Torque.kiloNewtonMeters).shouldBeNear(2.0, 1e-10)
        ((torque - 500.newtonMeters()) `in` Torque.newtonMeters).shouldBeNear(500.0, 1e-8)
        (-torque).toHuman() shouldBeEqualTo "-1.0 kN·m"
    }
}
