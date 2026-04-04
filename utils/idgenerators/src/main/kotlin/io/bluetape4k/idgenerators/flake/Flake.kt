package io.bluetape4k.idgenerators.flake

import io.bluetape4k.codec.Url62
import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.idgenerators.utils.node.MacAddressNodeIdentifier
import io.bluetape4k.idgenerators.utils.node.NodeIdentifier
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import java.math.BigInteger
import java.nio.ByteBuffer
import java.time.Clock
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [Boundary](https://github.com/boundary/flake) 의  Flake ID 생성 알고리즘의 Kotlin 구현체
 *
 * ```
 * val flake = Flake()
 * val id: ByteArray = flake.nextId()  // 128 bit ID (16 bytes)
 * val idString = flake.nextIdString() // 128 bit ID as base62 string (예: "AmknfdhdjSsgw6fqBk")
 * ```
 *
 * 3개의 Flake ID를 생성하면, 다음과 같이 출력됩니다:
 * ```
 * Hex=0000019265902e57beab72881e400000
 * Hex=0000019265902e57beab72881e400001
 * Hex=0000019265902e57beab72881e400002
 * Base62=AmknwjEj6DWnSOpkRM
 * Base62=AmknwjEj6DWnSOpkRN
 * Base62=AmknwjEj6DWnSOpkRO
 * ```
 *
 */
class Flake private constructor(
    private val nodeId: ByteArray,
    private val clock: Clock,
): IdGenerator<ByteArray> {

    companion object: KLogging() {
        private const val MAX_SEQ = 0xFFFF      // Short.MAX_VALUE
        private const val ID_SIZE_BYTES = 16
        private const val NODE_ID_BYTES = 6
        private const val HEX_VALUES = "0123456789abcdef"

        /**
         * [nodeIdentifier]와 [clock]을 사용해 Flake 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val flake = Flake()
         * val id: ByteArray = flake.nextId()  // 16 bytes
         * ```
         *
         * @param nodeIdentifier 노드 식별자. 기본값은 MAC 주소 기반
         * @param clock 타임스탬프 공급원. 기본값은 UTC 시스템 시각
         */
        @JvmStatic
        operator fun invoke(
            nodeIdentifier: NodeIdentifier = MacAddressNodeIdentifier(),
            clock: Clock = Clock.systemUTC(),
        ): Flake {
            val tmpNodeId = nodeIdentifier.get()
            val nodeId = ByteArray(NODE_ID_BYTES)
            for (i in 0 until NODE_ID_BYTES) {
                nodeId[i] = ((tmpNodeId shr (NODE_ID_BYTES - 1 - i) * 8) and 0xFF).toByte()
            }
            return Flake(nodeId, clock)
        }

        /**
         * Flake ID 바이트 배열을 소문자 Hex 문자열로 변환합니다.
         *
         * ```kotlin
         * val flake = Flake()
         * val id: ByteArray = flake.nextId()
         * val hex: String = Flake.asHexString(id)
         * // hex.length == 32
         * ```
         *
         * @param flakeId 변환할 16바이트 Flake ID
         * @return 32자 소문자 Hex 문자열
         */
        fun asHexString(flakeId: ByteArray): String = buildString {
            flakeId.forEach { byte ->
                val high = (byte.toInt() and 0xF0) ushr 4
                val low = byte.toInt() and 0x0F
                append(HEX_VALUES[high])
                append(HEX_VALUES[low])
            }
        }

        /**
         * Flake ID 바이트 배열을 Base62 문자열로 변환합니다.
         *
         * ```kotlin
         * val flake = Flake()
         * val id: ByteArray = flake.nextId()
         * val base62: String = Flake.asBase62String(id)
         * // base62.isNotBlank() == true
         * ```
         *
         * @param flakeId 변환할 16바이트 Flake ID
         * @return Base62 인코딩 문자열
         */
        fun asBase62String(flakeId: ByteArray): String {
            require(flakeId.size == 16) { "ByteArray size must be 16" }
            val buffer = ByteBuffer.wrap(flakeId)
            val msb = buffer.getLong()
            val lsb = buffer.getLong()
            return Url62.encode(UUID(msb, lsb))
        }

        /**
         * Flake ID 바이트 배열을 `타임스탬프-노드ID-시퀀스` 형식의 컴포넌트 문자열로 변환합니다.
         *
         * ```kotlin
         * val flake = Flake()
         * val id: ByteArray = flake.nextId()
         * val comp: String = Flake.asComponentString(id)
         * // comp 예: "1689000000000-48112611404-0"
         * ```
         *
         * @param flakeId 변환할 16바이트 Flake ID
         * @return `{timestamp}-{nodeId}-{sequence}` 형식의 문자열
         */
        fun asComponentString(flakeId: ByteArray): String {
            val buffer = ByteBuffer.wrap(flakeId)
            val node = ByteArray(NODE_ID_BYTES)
            buffer.get(node)
            return "${buffer.long}-${BigInteger(node).toLong()}-${buffer.short}"
        }
    }

    private val lock: Lock = ReentrantLock()

    @Volatile
    private var currentTime: Long = clock.millis()

    @Volatile
    private var lastTime: Long = clock.millis()

    private val sequencer = atomic(0)
    private var sequence: Int by sequencer

    /**
     * 다음 Flake ID를 16바이트 배열로 생성합니다.
     *
     * ```kotlin
     * val flake = Flake()
     * val id: ByteArray = flake.nextId()
     * // id.size == 16
     * ```
     *
     * @return 128비트(16바이트) Flake ID
     */
    override fun nextId(): ByteArray {
        lock.withLock {
            updateState()
            val idBuffer: ByteBuffer = ByteBuffer.allocate(ID_SIZE_BYTES)
            return idBuffer
                .putLong(currentTime)
                .put(nodeId)
                .putShort(sequence.toShort())
                .array()
        }
    }

    /**
     * 다음 Flake ID를 Base62 문자열로 생성합니다.
     *
     * ```kotlin
     * val flake = Flake()
     * val id: String = flake.nextIdAsString()
     * // id.isNotBlank() == true
     * ```
     *
     * @return Base62 인코딩된 Flake ID 문자열
     */
    override fun nextIdAsString(): String {
        return asBase62String(nextId())
    }

    private fun updateState() {
        currentTime = clock.millis()

        if (currentTime != lastTime) {
            sequence = 0
            lastTime = currentTime
        } else if (sequence == MAX_SEQ) {
            while (currentTime <= lastTime) {
                currentTime = clock.millis()
            }
            sequence = 0
            lastTime = currentTime
        } else {
            sequencer.incrementAndGet()
        }
    }
}
