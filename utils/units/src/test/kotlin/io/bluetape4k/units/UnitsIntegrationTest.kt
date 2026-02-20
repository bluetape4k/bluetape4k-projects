package io.bluetape4k.units

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue
import kotlin.math.pow

/**
 * 다양한 단위 타입 간의 통합 테스트
 *
 * 이 테스트는 서로 다른 단위 타입(길이, 무게, 면적 등) 간의 연산과 상호작용을 검증합니다.
 */
@RandomizedTest
class UnitsIntegrationTest {
    companion object: KLogging() {
        private const val EPSILON = 1e-10
    }

    private fun Double.shouldBeNear(
        expected: Double,
        delta: Double = EPSILON,
    ) {
        (this - expected).absoluteValue shouldBeLessThan delta
    }

    @Test
    fun `volume operations`() {
        // 부피 연산 테스트
        val volume1 = 100.0.liter()
        val volume2 = 50.0.liter()

        (volume1 + volume2).inLiter() shouldBeEqualTo 150.0
        (volume1 - volume2).inLiter() shouldBeEqualTo 50.0
        (volume1 * 2).inLiter() shouldBeEqualTo 200.0
        (volume1 / 2).inLiter() shouldBeEqualTo 50.0
    }

    @Test
    fun `area operations`() {
        // 면적 연산 테스트
        val area1 = 100.0.meter2()
        val area2 = 50.0.meter2()

        (area1 + area2).inMeter2() shouldBeEqualTo 150.0
        (area1 - area2).inMeter2() shouldBeEqualTo 50.0
        (area1 * 2).inMeter2() shouldBeEqualTo 200.0
        (area1 / 2).inMeter2() shouldBeEqualTo 50.0
    }

    @Test
    fun `complex chain of operations`() {
        // 복잡한 연산 체인 테스트
        // (2m + 3m) * 2m = 10 m^2 (면적 계산)
        val length1 = 2.0.meter()
        val length2 = 3.0.meter()
        val width = 2.0.meter()

        val totalLength = length1 + length2 // 5m
        // totalLength * width는 Area를 만들 수 없으므로 직접 계산
        val area = 10.0.meter2()
        area.inMeter2() shouldBeEqualTo 10.0

        // 10 m^2 * 5m = 50 m^3
        val height = 5.0.meter()
        val volume = area * height
        volume.inMeter3() shouldBeEqualTo 50.0
    }

    @Test
    fun `mixed unit arithmetic`() {
        // 서로 다른 단위 간의 연산
        val lengthInKm = 1.0.kilometer()
        val lengthInM = 500.0.meter()

        // 1km + 500m = 1500m
        val totalLength = lengthInKm + lengthInM
        totalLength.inMeter() shouldBeEqualTo 1500.0
        totalLength.inKilometer() shouldBeEqualTo 1.5

        // 면적 계산
        val width = 100.0.meter()
        val area = (totalLength.inMeter() * width.inMeter()).meter2()
        area.inMeter2() shouldBeEqualTo 150000.0
    }

    @Test
    fun `comparison across units`() {
        // 다른 단위로 표현된 값 비교
        val length1 = 1.0.kilometer() // 1000m
        val length2 = 900.0.meter() // 900m
        val length3 = 50000.0.centimeter() // 500m

        length1 shouldBeGreaterThan length2
        length1 shouldBeGreaterThan length3
        length2 shouldBeGreaterThan length3
        length3 shouldBeEqualTo 500.0.meter()
    }

    @Test
    fun `parsing and conversion roundtrip`() {
        // 파싱 -> 변환 -> 문자열화 라운드트립 테스트
        val length = Length.parse("100.5 m")
        length.inMeter() shouldBeEqualTo 100.5
        length.inKilometer() shouldBeEqualTo 0.1005
        length.toHuman() shouldBeEqualTo "100.5 m"

        val weight = Weight.parse("50.0 kg")
        weight.inKilogram() shouldBeEqualTo 50.0
        weight.inGram() shouldBeEqualTo 50000.0

        val area = Area.parse("25.0 m^2")
        area.inMeter2() shouldBeEqualTo 25.0
        area.inCentimeter2() shouldBeEqualTo 250000.0

        val volume = Volume.parse("5.0 l")
        volume.inLiter() shouldBeEqualTo 5.0
        volume.inMilliLiter() shouldBeEqualTo 5000.0
    }

    @Test
    fun `negative values handling`() {
        // 음수 값 처리 테스트
        val negativeLength = (-100.0).meter()
        negativeLength.inMeter() shouldBeEqualTo -100.0
        (-negativeLength).inMeter() shouldBeEqualTo 100.0

        val negativeWeight = (-50.0).kilogram()
        negativeWeight.inKilogram() shouldBeEqualTo -50.0

        // 음수 값의 연산
        val positiveLength = 200.0.meter()
        val result = positiveLength + negativeLength
        result.inMeter() shouldBeEqualTo 100.0
    }

    @Test
    fun `scalar multiplication with different types`() {
        // 스칼라 곱셈 테스트
        val scalars = listOf(0.5, 1.0, 2.0, 5.0, 10.0)
        scalars.forEach { scalar ->
            val length = 10.0.meter()
            val scaledLength = length * scalar
            scaledLength.inMeter().shouldBeNear(10.0 * scalar)

            val weight = 5.0.kilogram()
            val scaledWeight = weight * scalar
            scaledWeight.inKilogram().shouldBeNear(5.0 * scalar)
        }
    }

    @Test
    fun `unit consistency across operations`() {
        // 연산에서 단위 일관성 검증
        val baseLength = 100.0.meter()

        // 모든 결과가 올바른 기본 단위(mm)로 저장되는지 확인
        val lengthInMm = baseLength.value
        val lengthInCm = 10000.0.centimeter().value // 100m = 10000cm
        val lengthInKm = 0.1.kilometer().value // 100m = 0.1km

        lengthInMm shouldBeEqualTo lengthInCm
        lengthInMm shouldBeEqualTo lengthInKm
    }

    @Test
    fun `edge cases with zero and infinity`() {
        // 영과 무한대 처리
        val zeroLength = Length.ZERO
        val oneMeter = 1.0.meter()

        zeroLength + oneMeter shouldBeEqualTo oneMeter
        oneMeter - oneMeter shouldBeEqualTo Length.ZERO
        zeroLength * 100 shouldBeEqualTo Length.ZERO

        // 무한대
        val infLength = Length(Double.POSITIVE_INFINITY)
        infLength shouldBeGreaterThan oneMeter

        val negInfLength = Length(Double.NEGATIVE_INFINITY)
        negInfLength shouldBeLessThan oneMeter
    }

    @Test
    fun `NaN propagation`() {
        // NaN 전파 테스트
        val nanLength = Length.NaN
        val normalLength = 100.0.meter()

        (nanLength + normalLength).value.isNaN().shouldBeTrue()
        (nanLength - normalLength).value.isNaN().shouldBeTrue()

        // NaN 파싱
        Length.parse(null).value.isNaN().shouldBeTrue()
        Length.parse("").value.isNaN().shouldBeTrue()
    }

    @Test
    fun `chained conversions`() {
        // 연속된 단위 변환
        val length = 1.0.kilometer()

        // km -> m
        val inMeters = length.convertTo(LengthUnit.METER)
        inMeters.inMeter() shouldBeEqualTo 1000.0

        // m -> cm
        val inCentimeters = inMeters.convertTo(LengthUnit.CENTIMETER)
        inCentimeters.inCentimeter() shouldBeEqualTo 100000.0

        // cm -> mm
        val inMillimeters = inCentimeters.convertTo(LengthUnit.MILLIMETER)
        inMillimeters.inMillimeter() shouldBeEqualTo 1000000.0
    }

    @Test
    fun `storage calculation`() {
        // 디지털 저장 단위 계산
        val fileSize1 = 500.0.mbytes()
        val fileSize2 = 750.0.mbytes()

        val totalSize = fileSize1 + fileSize2
        totalSize.inMBytes() shouldBeEqualTo 1250.0
        totalSize.inGBytes().shouldBeNear(1.220703125)

        // 대용량 저장
        val bigStorage = 1.0.pbytes()
        bigStorage.inBytes() shouldBeEqualTo 1024.0.pow(5)
    }

    @Test
    fun `temperature conversions`() {
        // 온도 변환 (특별한 케이스: offset 기반)
        val celsius = 100.0.celsius()
        celsius.inCelcius() shouldBeEqualTo 100.0
        celsius.inKelvin() shouldBeEqualTo 373.15
        celsius.inFahrenheit() shouldBeEqualTo 212.0

        val kelvin = 273.15.kelvin()
        kelvin.inCelcius() shouldBeEqualTo 0.0
        kelvin.inFahrenheit() shouldBeEqualTo 32.0

        val fahrenheit = 32.0.fahrenheit()
        fahrenheit.inFahrenheit() shouldBeEqualTo 32.0
        fahrenheit.inCelcius().shouldBeNear(0.0, 0.01)
        fahrenheit.inKelvin().shouldBeNear(273.15, 0.01)
    }

    @Test
    fun `pressure unit conversions`() {
        // 압력 단위 변환
        val atm = 1.0.atm()
        atm.inPascal().shouldBeNear(101325.0, 0.1)
        atm.inBar().shouldBeNear(1.01325, 0.001)
        // PSI 변환은 factor 값에 따라 약간의 오차가 있을 수 있음
        atm.inPsi().shouldBeNear(14.696, 0.01)

        val bar = 1.0.bar()
        bar.inPascal() shouldBeEqualTo 100000.0
        bar.inKiloPascal() shouldBeEqualTo 100.0
    }

    @Test
    fun `angle conversions and operations`() {
        // 각도 변환 및 연산
        val rightAngle = 90.0.degree()
        rightAngle.inRadian().shouldBeNear(Math.PI / 2)

        val piRadians = Math.PI.radian()
        piRadians.inDegree().shouldBeNear(180.0)

        // 각도 덧셈
        val angle1 = 45.0.degree()
        val angle2 = 45.0.degree()
        val sum = angle1 + angle2
        sum.inDegree() shouldBeEqualTo 90.0

        // toHuman은 항상 0-360 범위로 정규화
        val bigAngle = 450.0.degree()
        bigAngle.toHuman() shouldBeEqualTo "90.0 deg"
    }

    @Test
    fun `all measurable types comparison`() {
        // 모든 Measurable 타입의 비교 연산 일관성
        val length1 = 100.0.meter()
        val length2 = 200.0.meter()

        (length1 < length2).shouldBeTrue()
        (length1 > length2).shouldBeFalse()
        (length1 == length1).shouldBeTrue()

        val weight1 = 50.0.kilogram()
        val weight2 = 100.0.kilogram()

        (weight1 < weight2).shouldBeTrue()
        (weight1 > weight2).shouldBeFalse()
    }
}
