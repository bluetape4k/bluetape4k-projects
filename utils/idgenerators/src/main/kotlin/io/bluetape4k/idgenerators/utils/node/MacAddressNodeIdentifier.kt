package io.bluetape4k.idgenerators.utils.node

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import java.net.NetworkInterface

/**
 * Mac 주소를 사용하여 노드 식별자를 생성하는 클래스
 *
 * ```kotlin
 * val nodeId = MacAddressNodeIdentifier()
 * val id: Long = nodeId.get()
 * // id != 0L
 * ```
 */
class MacAddressNodeIdentifier private constructor(
    private val macAddress: ByteArray,
): NodeIdentifier {

    companion object: KLogging() {
        private const val MAC_ADDRESS_LENGTH = 6

        /**
         * 로컬 네트워크 인터페이스의 MAC 주소를 자동 탐지하여 [MacAddressNodeIdentifier]를 생성합니다.
         *
         * ```kotlin
         * val nodeId = MacAddressNodeIdentifier()
         * val id: Long = nodeId.get()
         * // id != 0L
         * ```
         */
        @JvmStatic
        operator fun invoke(): MacAddressNodeIdentifier {
            val macAddr = findViableMacAddress()
                ?: error("No viable MAC address found")
            return invoke(macAddr)
        }

        /**
         * 지정한 네트워크 인터페이스 이름의 MAC 주소를 사용하여 [MacAddressNodeIdentifier]를 생성합니다.
         *
         * ```kotlin
         * val nodeId = MacAddressNodeIdentifier("en0")
         * val id: Long = nodeId.get()
         * // id != 0L
         * ```
         *
         * @param interfaceName 네트워크 인터페이스 이름 (예: "en0", "eth0")
         */
        @JvmStatic
        operator fun invoke(interfaceName: String): MacAddressNodeIdentifier {
            interfaceName.requireNotBlank("interfaceName")
            val macAddr = getMacAddressFromInterface(interfaceName)
                ?: error("No network interface found with name $interfaceName")

            return invoke(macAddr)
        }

        /**
         * 직접 제공한 MAC 주소 바이트 배열로 [MacAddressNodeIdentifier]를 생성합니다.
         *
         * ```kotlin
         * val mac = byteArrayOf(0x00, 0x1A, 0x2B, 0x3C, 0x4D, 0x5E)
         * val nodeId = MacAddressNodeIdentifier(mac)
         * val id: Long = nodeId.get()
         * // id != 0L
         * ```
         *
         * @param macAddress 6바이트 MAC 주소 배열
         */
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

    /**
     * MAC 주소를 Long 값으로 변환하여 반환합니다.
     *
     * ```kotlin
     * val nodeId = MacAddressNodeIdentifier()
     * val id: Long = nodeId.get()
     * // id != 0L
     * ```
     *
     * @return MAC 주소를 6바이트 빅엔디안으로 인코딩한 Long 값
     */
    override fun get(): Long {
        var macAsLong = 0L
        repeat(MAC_ADDRESS_LENGTH) {
            val temp = (macAddress[it].toLong() and 0xFFL) shl ((5 - it) * 8)
            macAsLong = macAsLong or temp
        }
        return macAsLong
    }
}
