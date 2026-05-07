package io.bluetape4k.exposed.core.measured

import io.bluetape4k.measured.Area
import io.bluetape4k.measured.Length
import io.bluetape4k.measured.Measure
import io.bluetape4k.measured.celsius
import io.bluetape4k.measured.celsiusDelta
import io.bluetape4k.measured.centimeters
import io.bluetape4k.measured.kilometers2
import io.bluetape4k.measured.meters
import io.bluetape4k.assertions.invoking
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * [MeasureColumnType], [TemperatureColumnType], [TemperatureDeltaColumnType] 단위 테스트.
 *
 * DB 연결 없이 컬럼 타입의 직렬화/역직렬화 계약을 검증합니다.
 */
class ColumnTypeUnitTest {

    // ──────────────────────────────────────────────────────────────
    // MeasureColumnType
    // ──────────────────────────────────────────────────────────────

    // sqlType() 은 Exposed DoubleColumnType 에 위임되며 트랜잭션 컨텍스트(getCurrentDialect) 를 필요로 하므로
    // DB 통합 테스트(MeasuredColumnTypesTest.Jdbc)에서 검증합니다.

    @Test
    fun `MeasureColumnType notNullValueToDB 는 base unit 기준 Double 로 변환한다`() {
        // 150 cm → 1.5 m
        val type = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }
        val result = type.notNullValueToDB(1.5.meters()) as Double
        result.shouldBeNear(1.5, 1e-10)
    }

    @Test
    fun `MeasureColumnType notNullValueToDB 는 단위 변환 후 base unit 값을 저장한다`() {
        // 150 cm → 1.5 m (base unit = meters)
        val type = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }
        val result = type.notNullValueToDB(150.centimeters()) as Double
        result.shouldBeNear(1.5, 1e-10)
    }

    @Test
    fun `MeasureColumnType nonNullValueToString 은 base unit Double 문자열을 반환한다`() {
        val type = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }
        val str = type.nonNullValueToString(2.0.meters())
        str.shouldBeEqualTo("2.0")
    }

    @Test
    fun `MeasureColumnType valueFromDB 는 Number 를 Measure 로 역직렬화한다`() {
        val type = MeasureColumnType(Area.meters2) { Measure(it, Area.meters2) }
        val decoded = type.valueFromDB(25.0)!!
        (decoded `in` Area.meters2).shouldBeNear(25.0, 1e-10)
    }

    @Test
    fun `MeasureColumnType valueFromDB 는 Measure 인스턴스를 그대로 반환한다`() {
        val type = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }
        val original = 3.0.meters()
        val result = type.valueFromDB(original)!!
        (result `in` Length.meters).shouldBeNear(3.0, 1e-10)
    }

    @Test
    fun `MeasureColumnType valueFromDB 는 Int Number 도 처리한다`() {
        val type = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }
        val result = type.valueFromDB(5)!!
        (result `in` Length.meters).shouldBeNear(5.0, 1e-10)
    }

    @Test
    fun `MeasureColumnType valueFromDB 는 지원하지 않는 타입에서 예외를 던진다`() {
        val type = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }
        invoking { type.valueFromDB("invalid") } shouldThrow IllegalStateException::class
    }

    @Test
    fun `MeasureColumnType valueFromDB 예외 메시지는 baseUnit 정보를 포함한다`() {
        val type = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }
        val exception = runCatching { type.valueFromDB("bad") }.exceptionOrNull()
        (exception?.message?.contains("MeasureColumnType") == true).shouldBeTrue()
    }

    // ──────────────────────────────────────────────────────────────
    // TemperatureColumnType
    // ──────────────────────────────────────────────────────────────

    // sqlType() 은 DoubleColumnType 에 위임 → DB 통합 테스트에서 검증

    @Test
    fun `TemperatureColumnType notNullValueToDB 는 Kelvin Double 로 변환한다`() {
        val type = TemperatureColumnType()
        // 25°C = 298.15 K
        val result = type.notNullValueToDB(25.celsius()) as Double
        result.shouldBeNear(298.15, 1e-10)
    }

    @Test
    fun `TemperatureColumnType nonNullValueToString 은 Kelvin Double 문자열을 반환한다`() {
        val type = TemperatureColumnType()
        val str = type.nonNullValueToString(25.celsius())
        str.shouldBeEqualTo("298.15")
    }

    @Test
    fun `TemperatureColumnType valueFromDB 는 Number 를 Temperature 로 역직렬화한다`() {
        val type = TemperatureColumnType()
        val temp = type.valueFromDB(298.15)!!
        temp.inCelsius().shouldBeNear(25.0, 1e-10)
    }

    @Test
    fun `TemperatureColumnType valueFromDB 는 Temperature 인스턴스를 그대로 반환한다`() {
        val type = TemperatureColumnType()
        val original = 100.celsius()
        val result = type.valueFromDB(original)!!
        result.inCelsius().shouldBeNear(100.0, 1e-10)
    }

    @Test
    fun `TemperatureColumnType valueFromDB 는 지원하지 않는 타입에서 예외를 던진다`() {
        val type = TemperatureColumnType()
        invoking { type.valueFromDB("invalid") } shouldThrow IllegalStateException::class
    }

    @Test
    fun `TemperatureColumnType valueFromDB 예외 메시지는 타입 정보를 포함한다`() {
        val type = TemperatureColumnType()
        val exception = runCatching { type.valueFromDB(true) }.exceptionOrNull()
        (exception?.message?.contains("TemperatureColumnType") == true).shouldBeTrue()
    }

    // ──────────────────────────────────────────────────────────────
    // TemperatureDeltaColumnType
    // ──────────────────────────────────────────────────────────────

    // sqlType() 은 DoubleColumnType 에 위임 → DB 통합 테스트에서 검증

    @Test
    fun `TemperatureDeltaColumnType notNullValueToDB 는 Kelvin delta Double 로 변환한다`() {
        val type = TemperatureDeltaColumnType()
        // 10°C delta = 10 K delta
        val result = type.notNullValueToDB(10.celsiusDelta()) as Double
        result.shouldBeNear(10.0, 1e-10)
    }

    @Test
    fun `TemperatureDeltaColumnType nonNullValueToString 은 Kelvin delta Double 문자열을 반환한다`() {
        val type = TemperatureDeltaColumnType()
        val str = type.nonNullValueToString(10.celsiusDelta())
        str.shouldBeEqualTo("10.0")
    }

    @Test
    fun `TemperatureDeltaColumnType valueFromDB 는 Number 를 TemperatureDelta 로 역직렬화한다`() {
        val type = TemperatureDeltaColumnType()
        val delta = type.valueFromDB(10.0)!!
        delta.inCelsius().shouldBeNear(10.0, 1e-10)
    }

    @Test
    fun `TemperatureDeltaColumnType valueFromDB 는 TemperatureDelta 인스턴스를 그대로 반환한다`() {
        val type = TemperatureDeltaColumnType()
        val original = 5.0.celsiusDelta()
        val result = type.valueFromDB(original)!!
        result.inCelsius().shouldBeNear(5.0, 1e-10)
    }

    @Test
    fun `TemperatureDeltaColumnType valueFromDB 는 지원하지 않는 타입에서 예외를 던진다`() {
        val type = TemperatureDeltaColumnType()
        invoking { type.valueFromDB(listOf(1, 2)) } shouldThrow IllegalStateException::class
    }

    @Test
    fun `TemperatureDeltaColumnType valueFromDB 예외 메시지는 타입 정보를 포함한다`() {
        val type = TemperatureDeltaColumnType()
        val exception = runCatching { type.valueFromDB("bad") }.exceptionOrNull()
        (exception?.message?.contains("TemperatureDeltaColumnType") == true).shouldBeTrue()
    }

    // ──────────────────────────────────────────────────────────────
    // 단위 변환 round-trip 정확도 검증
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `MeasureColumnType area 는 km2 를 m2 기준으로 저장하고 복원한다`() {
        val type = MeasureColumnType(Area.meters2) { Measure(it, Area.meters2) }
        val encoded = type.notNullValueToDB(2.5.kilometers2()) as Double
        // 2.5 km2 = 2_500_000 m2
        encoded.shouldBeNear(2_500_000.0, 1e-4)

        val decoded = type.valueFromDB(encoded)!!
        (decoded `in` Area.meters2).shouldBeNear(2_500_000.0, 1e-4)
    }

    @Test
    fun `MeasureColumnType Float Number 도 역직렬화된다`() {
        val type = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }
        val result = type.valueFromDB(1.5f)!!
        (result `in` Length.meters).shouldBeNear(1.5, 1e-6)
    }
}
