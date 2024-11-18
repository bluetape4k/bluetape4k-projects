package io.bluetape4k.idgenerators

import io.bluetape4k.idgenerators.snowflake.MAX_MACHINE_ID
import java.net.NetworkInterface
import java.security.SecureRandom
import kotlin.math.absoluteValue

private val random: SecureRandom by lazy { SecureRandom.getInstanceStrong() }

/**
 * 36진수: (0-9, a-z) 로 구성된 문자열을 가지는 진법
 */
const val ALPHA_NUMERIC_BASE = Character.MAX_RADIX

fun getMachineId(maxNumber: Int = MAX_MACHINE_ID): Int {
    val machineId = try {
        buildString {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .forEach { network ->
                    network.hardwareAddress?.run {
                        forEach { elem -> append("%02X".format(elem)) }
                    }
                }
        }.hashCode()
    } catch (e: Throwable) {
        random.nextInt()
    }
    return machineId.absoluteValue % maxNumber
}

fun String.parseAsInt(radix: Int = ALPHA_NUMERIC_BASE): Int =
    java.lang.Integer.parseInt(this.lowercase(), radix)

fun String.parseAsUInt(radix: Int = ALPHA_NUMERIC_BASE): Int =
    java.lang.Integer.parseUnsignedInt(this.lowercase(), radix)

fun String.parseAsLong(radix: Int = ALPHA_NUMERIC_BASE): Long =
    java.lang.Long.parseLong(this.lowercase(), radix)

fun String.parseAsULong(radix: Int = ALPHA_NUMERIC_BASE): Long =
    java.lang.Long.parseUnsignedLong(this.lowercase(), radix)
