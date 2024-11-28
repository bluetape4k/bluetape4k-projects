package io.bluetape4k.units

import io.bluetape4k.support.unsafeLazy
import kotlin.math.absoluteValue

fun pressureOf(value: Number = 0.0, unit: PressureUnit = PressureUnit.PASCAL): Pressure =
    Pressure(value, unit)

fun <T: Number> T.pressureBy(unit: PressureUnit): Pressure = pressureOf(this.toDouble(), unit)

fun <T: Number> T.atm(): Pressure = pressureOf(this, PressureUnit.ATM)
fun <T: Number> T.pascal(): Pressure = pressureOf(this, PressureUnit.PASCAL)
fun <T: Number> T.hectoPascal(): Pressure = pressureOf(this, PressureUnit.HECTO_PASCAL)
fun <T: Number> T.kiloPascal(): Pressure = pressureOf(this, PressureUnit.KILO_PASCAL)
fun <T: Number> T.megaPascal(): Pressure = pressureOf(this, PressureUnit.MEGA_PASCAL)

fun <T: Number> T.bar(): Pressure = pressureOf(this, PressureUnit.BAR)
fun <T: Number> T.deciBar(): Pressure = pressureOf(this, PressureUnit.DECI_BAR)
fun <T: Number> T.milliBar(): Pressure = pressureOf(this, PressureUnit.MILLI_BAR)

fun <T: Number> T.psi(): Pressure = pressureOf(this, PressureUnit.PSI)
fun <T: Number> T.torr(): Pressure = pressureOf(this, PressureUnit.TORR)
fun <T: Number> T.mmHg(): Pressure = pressureOf(this, PressureUnit.MMHG)

operator fun <T: Number> T.times(pressure: Pressure): Pressure = pressure.times(this)

/**
 * ## 압력 단위
 */
enum class PressureUnit(
    override val unitName: String,
    override val factor: Double,
): MeasurableUnit {

    /**
     * 기압 :  지구 해수면 근처에서 잰 대기압을 기준으로 한다. 1기압은 101325 Pa, 760 mmHg이다.
     */
    ATM("atm", 101325.0),

    /**
     * Pascal 단위 ( N/m2 ) : https://ko.wikipedia.org/wiki/%ED%8C%8C%EC%8A%A4%EC%B9%BC_(%EB%8B%A8%EC%9C%84)
     */
    PASCAL("Pa", 1.0),

    /**
     * HectoPascal (1 hPa = 100 pa)
     */
    HECTO_PASCAL("hPa", 100.0),

    /**
     * KiloPascal (1kPa = 1000 Pa)
     */
    KILO_PASCAL("kPa", 1000.0),

    /**
     * MegaPascal (1 MPa = 1000,000 Pa)
     */
    MEGA_PASCAL("mPa", 1.0e6),

    /**
     * GigaPascal (1 MPa = 1000,000 Pa)
     */
    GIGA_PASCAL("gPa", 1.0e9),

    /**
     * Bar : 1 bar = 100,000 Pa
     *
     *
     * 대기압은 통상적으로 밀리바를 사용하여, "표준" 해수면 압력을 1.01325 바와 같은 1013.25 밀리바(hPa, 헥토파스칼)로 정의한다.
     * 밀리바는 SI 단위계가 아닌데도 불구하고, 아직 기상학 분야에서 대기압을 기술하는 압력 단위로 사용되곤 한다.
     */
    BAR("bar", 1.0e5),

    /**
     * Deci Bar : 1 dbar = 0.1 bar = 10,000 Pa
     *
     *
     * 대기압은 통상적으로 밀리바를 사용하여, "표준" 해수면 압력을 1.01325 바와 같은 1013.25 밀리바(hPa, 헥토파스칼)로 정의한다.
     * 밀리바는 SI 단위계가 아닌데도 불구하고, 아직 기상학 분야에서 대기압을 기술하는 압력 단위로 사용되곤 한다.
     */
    DECI_BAR("decibar", 1.0e4),

    /**
     * Milli Bar : 1 mbar = 0.001 bar = 100 Pa
     *
     *
     * 대기압은 통상적으로 밀리바를 사용하여, "표준" 해수면 압력을 1.01325 바와 같은 1013.25 밀리바(hPa, 헥토파스칼)로 정의한다.
     * 밀리바는 SI 단위계가 아닌데도 불구하고, 아직 기상학 분야에서 대기압을 기술하는 압력 단위로 사용되곤 한다.
     */
    MILLI_BAR("mmbar", 100.0),

    /**
     * 제곱 인치당 파운드(영어: pound per square inch, 정확하게는 영어: pound-force per square inch,
     * 기호는 psi, lbf/in2, lbf/in2, lbf/sq in, lbf/sq in)는 애버더포와 단위로 나타낸 압력이나 응력 단위다.
     * 이 단위는 1 제곱 인치 넓이에 1 파운드힘이 누르는 압력이다. 1 제곱 인치당 파운드 는 약 6894.757 Pa이다.
     */
    PSI("psi", 6895.757),

    /**
     * 토르(torr, 기호 Torr)는 압력의 단위로 1mm의 수은주 압력에 해당한다.
     * 1 Torr = 1 mmHg. 1기압은 760 mmHg 이므로 1 Torr는 1 기압의 1/760이다. 이탈리아의 과학자 에반젤리스타 토리첼리의 이름을 따서 만들어졌다.
     */
    TORR("torr", 1.0 / 122.322),

    /**
     * 수은주 밀리미터(mmHg)는 압력의 단위로 1mmHg는 수은주의 높이가 1mm일 때의 압력이다. 1기압은 약 760mmHg에 해당한다.
     */
    MMHG("mmHg", 1.0 / 122.322);

    companion object {
        @JvmStatic
        fun parse(unitStr: String): PressureUnit {
            val lower = unitStr.lowercase().dropLastWhile { it == 's' }
            return entries.find { it.unitName.lowercase() == lower }
                ?: throw IllegalArgumentException("Unknown Pressure unit. unitStr=$unitStr")
        }
    }
}

/**
 * ## 주요 압력 단위
 *
 * 1. 파스칼 (Pa)
 *  - 정의: 국제 단위계(SI)에서 채택한 기본 압력 단위로, 1 Pa는 1㎡ 면적에 1 N의 힘이 가해질 때의 압력을 의미합니다.
 * 2. 바 (bar)
 *  - 정의: 1 bar는  Pa에 해당하며, 주로 기압을 측정할 때 사용됩니다.
 * 3. 킬로그램힘 퍼 제곱센티미터 (kgf/cm²)
 *  - 정의: 1 kgf/cm²는 1 cm² 면적에 1 kg의 힘이 작용할 때의 압력을 의미합니다. 이는 약 98,066.5 Pa에 해당합니다.
 * 4. 대기압 (atm)
 *  - 정의: 해수면에서의 평균 대기압을 기준으로 하며, 1 atm은 약 101,325 Pa에 해당합니다.
 *
 * 이 외에도 다양한 상황과 지역에서 psi(파운드 퍼 스퀘어 인치)와 같은 다른 단위들이 사용되기도 합니다.
 * 이러한 압력 단위들은 서로 변환이 가능하며, 각 분야에서 적절한 단위를 선택하여 사용합니다.
 */
@JvmInline
value class Pressure(override val value: Double = 0.0): Measurable<PressureUnit> {

    operator fun plus(other: Pressure): Pressure = Pressure(value + other.value)
    operator fun minus(other: Pressure): Pressure = Pressure(value - other.value)
    operator fun times(scalar: Number): Pressure = Pressure(value * scalar.toDouble())
    operator fun div(scalar: Number): Pressure = Pressure(value / scalar.toDouble())

    operator fun unaryMinus(): Pressure = Pressure(-value)

    fun inAtm(): Double = valueBy(PressureUnit.ATM)

    fun inPascal(): Double = valueBy(PressureUnit.PASCAL)
    fun inHectoPascal(): Double = valueBy(PressureUnit.HECTO_PASCAL)
    fun inKiloPascal(): Double = valueBy(PressureUnit.KILO_PASCAL)
    fun inMegaPascal(): Double = valueBy(PressureUnit.MEGA_PASCAL)
    fun inGigaPascal(): Double = valueBy(PressureUnit.GIGA_PASCAL)

    fun inBar(): Double = valueBy(PressureUnit.BAR)
    fun inDeciBar(): Double = valueBy(PressureUnit.DECI_BAR)
    fun inMilliBar(): Double = valueBy(PressureUnit.MILLI_BAR)

    fun inPsi(): Double = valueBy(PressureUnit.PSI)
    fun inTorr(): Double = valueBy(PressureUnit.TORR)
    fun inMmHg(): Double = valueBy(PressureUnit.MMHG)

    override fun convertTo(newUnit: PressureUnit): Pressure =
        Pressure(valueBy(newUnit), newUnit)

    override fun toHuman(): String {
        val dislay = value.absoluteValue
        val displayUnit = PressureUnit.entries.lastOrNull { dislay / it.factor > 1.0 } ?: PressureUnit.PASCAL
        return formatUnit(valueBy(displayUnit), displayUnit.unitName)
    }

    companion object {
        @JvmStatic
        val ZERO by unsafeLazy { Pressure(0.0) }

        @JvmStatic
        val NaN by unsafeLazy { Pressure(Double.NaN) }

        operator fun invoke(value: Number = 0.0, unit: PressureUnit = PressureUnit.PASCAL): Pressure =
            Pressure(value.toDouble() * unit.factor)

        fun parse(expr: String?): Pressure {
            if (expr.isNullOrBlank()) {
                return NaN
            }
            try {
                val (valueStr, unitStr) = expr.split(" ", limit = 2)
                println("valueStr=$valueStr, unitStr=$unitStr")
                return invoke(valueStr.toDouble(), PressureUnit.parse(unitStr))
            } catch (e: Throwable) {
                throw IllegalArgumentException("Invalid Pressure expression. expr=$expr")
            }
        }
    }
}
