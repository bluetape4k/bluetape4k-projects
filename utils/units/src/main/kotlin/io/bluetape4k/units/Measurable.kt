package io.bluetape4k.units

import java.io.Serializable

interface Measurable<T: MeasurableUnit>: Comparable<Measurable<T>>, Serializable {

    val value: Double

    fun convertTo(newUnit: T): Measurable<T>

    fun valueBy(unit: T): Double = value / unit.factor

    fun toHuman(): String

    fun toHuman(unit: T): String = formatUnit(valueBy(unit), unit.unitName)

    override fun compareTo(other: Measurable<T>): Int = value.compareTo(other.value)
}
