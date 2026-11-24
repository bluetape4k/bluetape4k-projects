package io.bluetape4k.io.digest

import java.security.MessageDigest
import java.util.HexFormat

/**
 * 입력 조각을 순서대로 전달해 하나의 digest를 만드는 변경 가능한 세션입니다.
 *
 * 세션은 스레드 안전하지 않으므로 하나의 계산에서만 사용해야 합니다. `digest()`와 `digestHex()`는
 * 현재 계산을 종료한 뒤 JDK `MessageDigest`처럼 초기 상태로 되돌립니다. reset이나 결과 반환은
 * provider 메모리의 안전한 삭제를 보장하지 않으며, 반환된 배열은 호출자가 관리합니다.
 */
interface DigestSession {

    /** 입력 바이트 배열을 처리하고 현재 세션을 반환합니다. */
    fun update(data: ByteArray): DigestSession

    /** 입력 배열의 지정 범위를 처리하고 현재 세션을 반환합니다. */
    fun update(data: ByteArray, offset: Int, length: Int): DigestSession

    /** 단일 바이트를 처리하고 현재 세션을 반환합니다. */
    fun update(value: Byte): DigestSession

    /** 현재까지 입력한 바이트의 digest를 호출자 소유 배열로 반환하고 세션을 초기화합니다. */
    fun digest(): ByteArray

    /** 현재까지 입력한 바이트의 lowercase hexadecimal digest를 반환하고 세션을 초기화합니다. */
    fun digestHex(): String
}

internal class JdkDigestSession(
    private val messageDigest: MessageDigest,
): DigestSession {

    override fun update(data: ByteArray): DigestSession = apply { messageDigest.update(data) }

    override fun update(data: ByteArray, offset: Int, length: Int): DigestSession =
        apply { messageDigest.update(data, offset, length) }

    override fun update(value: Byte): DigestSession = apply { messageDigest.update(value) }

    override fun digest(): ByteArray = messageDigest.digest()

    override fun digestHex(): String = HexFormat.of().formatHex(messageDigest.digest())
}
