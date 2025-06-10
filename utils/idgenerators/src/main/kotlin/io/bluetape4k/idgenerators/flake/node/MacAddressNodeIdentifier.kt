package io.bluetape4k.idgenerators.flake.node

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import java.net.NetworkInterface

/**
 * Mac 주소를 사용하여 노드 식별자를 생성하는 클래스
 */
class MacAddressNodeIdentifier private constructor(
    private val macAddress: ByteArray,
): NodeIdentifier {

    companion object: KLogging() {
        private const val MAC_ADDRESS_LENGTH = 6

        @JvmStatic
        operator fun invoke(): MacAddressNodeIdentifier {
            val macAddr = findViableMacAddress()
                ?: error("No viable MAC address found")
            return invoke(macAddr)
        }

        @JvmStatic
        operator fun invoke(interfaceName: String): MacAddressNodeIdentifier {
            interfaceName.requireNotBlank("interfaceName")
            val macAddr = getMacAddressFromInterface(interfaceName)
                ?: error("No network interface found with name $interfaceName")

            return invoke(macAddr)
        }

        @JvmStatic
        operator fun invoke(macAddress: ByteArray): MacAddressNodeIdentifier {
            require(macAddress.size == MAC_ADDRESS_LENGTH) { "MAC address must be $MAC_ADDRESS_LENGTH bytes" }
            return MacAddressNodeIdentifier(macAddress)
        }

        private fun findViableMacAddress(): ByteArray? {
            val nics = NetworkInterface.getNetworkInterfaces()
            while (nics.hasMoreElements()) {
                val currentNic = nics.nextElement()
                if (!currentNic.isLoopback && currentNic.isUp && (currentNic.hardwareAddress != null)) {
                    return currentNic.hardwareAddress
                }
            }
            return null
        }

        private fun getMacAddressFromInterface(interfaceName: String): ByteArray? {
            val nic = NetworkInterface.getByName(interfaceName)
            return nic?.hardwareAddress
        }
    }

    override fun get(): Long {
        var macAsLong = 0L
        repeat(MAC_ADDRESS_LENGTH) {
            val temp = (macAddress[it].toLong() and 0xFFL) shl ((5 - it) * 8)
            macAsLong = macAsLong or temp
        }
        return macAsLong
    }
}
