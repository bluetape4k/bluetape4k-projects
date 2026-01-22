package io.bluetape4k.idgenerators

import io.bluetape4k.idgenerators.snowflake.MAX_MACHINE_ID
import io.bluetape4k.idgenerators.utils.node.MacAddressNodeIdentifier
import kotlin.math.absoluteValue

/**
 * 36진수: (0-9, a-z) 로 구성된 문자열을 가지는 진법
 */
const val ALPHA_NUMERIC_BASE: Int = Character.MAX_RADIX

private val macAddressNodeIdentifier = MacAddressNodeIdentifier()

fun getMachineId(maxNumber: Int = MAX_MACHINE_ID): Int =
    macAddressNodeIdentifier.get().toInt().absoluteValue % maxNumber

fun String.parseAsInt(radix: Int = ALPHA_NUMERIC_BASE): Int =
    Integer.parseInt(this.lowercase(), radix)

fun String.parseAsUInt(radix: Int = ALPHA_NUMERIC_BASE): Int =
    Integer.parseUnsignedInt(this.lowercase(), radix)

fun String.parseAsLong(radix: Int = ALPHA_NUMERIC_BASE): Long =
    java.lang.Long.parseLong(this.lowercase(), radix)

fun String.parseAsULong(radix: Int = ALPHA_NUMERIC_BASE): Long =
    java.lang.Long.parseUnsignedLong(this.lowercase(), radix)
