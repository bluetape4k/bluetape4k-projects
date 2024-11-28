package io.bluetape4k.units

import io.bluetape4k.support.unsafeLazy
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

fun storageOf(value: Number = 0.0, unit: StorageUnit = StorageUnit.BYTE) = Storage(value, unit)

fun <T: Number> T.storageBy(unit: StorageUnit): Storage = storageOf(this.toDouble(), unit)

fun <T: Number> T.bytes(): Storage = storageBy(StorageUnit.BYTE)
fun <T: Number> T.kbytes(): Storage = storageBy(StorageUnit.KBYTE)
fun <T: Number> T.mbytes(): Storage = storageBy(StorageUnit.MBYTE)
fun <T: Number> T.gbytes(): Storage = storageBy(StorageUnit.GBYTE)
fun <T: Number> T.tbytes(): Storage = storageBy(StorageUnit.TBYTE)
fun <T: Number> T.pbytes(): Storage = storageBy(StorageUnit.PBYTE)
fun <T: Number> T.xbytes(): Storage = storageBy(StorageUnit.XBYTE)
fun <T: Number> T.zbytes(): Storage = storageBy(StorageUnit.ZBYTE)
fun <T: Number> T.ybytes(): Storage = storageBy(StorageUnit.YBYTE)

operator fun <T: Number> T.times(storage: Storage): Storage = storage.times(this)

/**
 * 저장장치 크기 단위 (Bytes)
 *
 * @property unitName 단위 약어
 * @property factor 단위 factor
 */
enum class StorageUnit(override val unitName: String, override val factor: Double): MeasurableUnit {

    BYTE("B", 1.0),
    KBYTE("KB", Storage.KBYTES),
    MBYTE("MB", Storage.KBYTES.pow(2)),
    GBYTE("GB", Storage.KBYTES.pow(3)),
    TBYTE("TB", Storage.KBYTES.pow(4)),
    PBYTE("PB", Storage.KBYTES.pow(5)),
    XBYTE("XB", Storage.KBYTES.pow(6)),
    ZBYTE("ZB", Storage.KBYTES.pow(7)),
    YBYTE("YB", Storage.KBYTES.pow(8));

    companion object {
        @JvmStatic
        fun parse(unitStr: String): StorageUnit {
            assert(unitStr.isNotBlank()) { "unitStr must not be blank." }

            var upper = unitStr.uppercase()
            if (upper.endsWith("S")) {
                upper = upper.dropLast(1)
            }
            return entries.find { it.unitName == upper }
                ?: throw IllegalArgumentException("Unknown Storage unit. unitStr=$unitStr")
        }
    }
}

/**
 * 저장 장치의 크기를 나타내는 클래스입니다.
 *
 * ```
 * val storage = Storage(100.0, StorageUnit.BYTE)
 * val storage4 = 100.0.bytes()
 * val storage7 = 100.0 * Storage(100.0, StorageUnit.BYTE)
 * val storage10 = 100.0 * 100.0.bytes()
 * val storage13 = Storage.parse("100.0 B")
 * storage13.toHuman() // "100.0 B"
 * ```
 *
 * @property value 저장장치의 크기의 byte 단위의 값
 */
@JvmInline
value class Storage(override val value: Double = 0.0): Measurable<StorageUnit> {

    operator fun plus(that: Storage): Storage = Storage(value + that.value)
    operator fun plus(scalar: Number): Storage = Storage(value + scalar.toDouble())
    operator fun minus(that: Storage): Storage = Storage(value - that.value)
    operator fun minus(scalar: Number): Storage = Storage(value - scalar.toDouble())
    operator fun times(scalar: Number): Storage = Storage(value * scalar.toDouble())
    operator fun div(scalar: Number): Storage = Storage(value / scalar.toDouble())
    operator fun unaryMinus(): Storage = Storage(-value)

    fun inBytes() = valueBy(StorageUnit.BYTE)
    fun inKBytes() = valueBy(StorageUnit.KBYTE)
    fun inMBytes() = valueBy(StorageUnit.MBYTE)
    fun inGBytes() = valueBy(StorageUnit.GBYTE)
    fun inTBytes() = valueBy(StorageUnit.TBYTE)
    fun inPBytes() = valueBy(StorageUnit.PBYTE)
    fun inXBytes() = valueBy(StorageUnit.XBYTE)
    fun inZBytes() = valueBy(StorageUnit.ZBYTE)
    fun inYBytes() = valueBy(StorageUnit.YBYTE)

    override fun convertTo(newUnit: StorageUnit): Storage =
        Storage(valueBy(newUnit), newUnit)

    override fun toHuman(): String {
        var dispalay = value.absoluteValue
        var order = 0

        while (dispalay >= KBYTES) {
            order++
            dispalay /= KBYTES
        }

        return if (order == 0) formatUnit(value.toLong(), StorageUnit.BYTE.unitName)
        else formatUnit(dispalay * value.sign, StorageUnit.entries[order].unitName)
    }

    companion object {
        const val KBYTES: Double = 1024.0
        const val MBYTES: Double = KBYTES * KBYTES

        @JvmStatic
        val ZERO: Storage by unsafeLazy { Storage(0.0) }

        @JvmStatic
        val NaN: Storage by unsafeLazy { Storage(Double.NaN) }

        operator fun invoke(value: Number = 0.0, unit: StorageUnit = StorageUnit.BYTE): Storage =
            Storage(value.toDouble() * unit.factor)

        fun parse(expr: String?): Storage {
            if (expr.isNullOrBlank()) {
                return NaN
            }
            try {
                val (valueStr, unitStr) = expr.split(" ", limit = 2)
                return Storage(valueStr.toDouble(), StorageUnit.parse(unitStr))
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid Storage expression. expr=$expr")
            }
        }
    }
}
