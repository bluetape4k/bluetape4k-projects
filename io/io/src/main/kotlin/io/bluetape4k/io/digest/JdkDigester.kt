package io.bluetape4k.io.digest

import java.security.MessageDigest

/**
 * JDK `MessageDigest` provider를 사용하는 digest 구현입니다.
 *
 * [algorithm]은 신뢰된 설정에서 지정해야 합니다. `MD5`와 `SHA-1`은 레거시 비보안 checksum
 * 호환에만 사용하고, 일반적인 선택에는 [Digesters.SHA256]을 권장합니다. 키 기반 인증과 공개
 * 검증이 필요한 경우 keyed MAC 또는 전자 서명을 사용하십시오.
 *
 * @param algorithm JDK provider가 지원하는 알고리즘 이름
 */
class JdkDigester(
    override val algorithm: String,
): Digester {

    /** 매 호출에 독립적인 JDK digest 상태를 가진 세션을 생성합니다. */
    override fun newSession(): DigestSession = JdkDigestSession(MessageDigest.getInstance(algorithm))
}
