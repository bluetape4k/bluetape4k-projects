package io.bluetape4k.units

internal const val DOUBLE_UNIT_FORMAT = "%.1f %s"
internal const val DECIMAL_UNIT_FORMAT = "%d %s"

internal fun formatUnit(value: Double, unitName: String): String =
    DOUBLE_UNIT_FORMAT.format(value, unitName)

internal fun formatUnit(value: Double, unitName: String, precision: Int): String {
    val formatStr = "%.${precision}f %s"
    return formatStr.format(value, unitName)
}

internal fun formatUnit(value: Long, unitName: String): String =
    DECIMAL_UNIT_FORMAT.format(value, unitName)
