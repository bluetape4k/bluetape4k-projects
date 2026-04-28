package io.bluetape4k.jwt.keychain

import io.bluetape4k.jwt.JwtConsts
import java.io.Serializable
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Duration

/**
 * [KeyChain]의 직렬화용 DTO입니다.
 *
 * ## 동작/계약
 * - [algorithmName]은 서명 알고리즘의 ID 문자열입니다 (예: "RS256").
 * - [publicKey]는 X.509 인코딩된 공개키 바이트 배열입니다 (`PublicKey.getEncoded()`).
 * - [privateKey]는 PKCS#8 인코딩된 개인키 바이트 배열입니다 (`PrivateKey.getEncoded()`).
 *
 * ## 보안 주의
 * - JDK 직렬화(ObjectInputStream)를 사용하지 않으므로 RCE 위험이 없습니다.
 * - Redis 등 외부 저장소에 저장 시 개인키가 포함되므로 저장소 접근 제어를 반드시 설정하세요.
 *
 * ## 마이그레이션
 * - v1.6 이전 버전의 JDK 직렬화 형식으로 저장된 Redis 키체인 항목은 호환되지 않습니다.
 * - 업그레이드 시 키체인 저장소(Redis queue 등)를 비워야 합니다.
 */
data class KeyChainDto(
    val id: String,
    val algorithmName: String,
    val createdAt: Long,
    val expiredTtl: Long,
): Serializable {
    var publicKey: ByteArray? = null
    var privateKey: ByteArray? = null
}

/**
 * [KeyChain]을 [KeyChainDto]로 변환합니다.
 *
 * 공개키는 X.509, 개인키는 PKCS#8 인코딩으로 저장합니다.
 *
 * ```kotlin
 * val keyChain = KeyChain()
 * val dto = keyChain.toDto()
 * // dto.algorithmName == "RS256"
 * // dto.id == keyChain.id
 * ```
 */
fun KeyChain.toDto(): KeyChainDto =
    KeyChainDto(
        id = id,
        algorithmName = algorithm.id,
        createdAt = createdAt,
        expiredTtl = expiredTtl,
    ).apply {
        publicKey = keyPair.public.encoded   // X.509 format
        privateKey = keyPair.private.encoded // PKCS#8 format
    }

/**
 * [KeyChainDto]를 [KeyChain]으로 변환합니다.
 *
 * X.509 / PKCS#8 인코딩된 바이트 배열로부터 키를 복원합니다.
 *
 * ```kotlin
 * val keyChain = KeyChain()
 * val dto = keyChain.toDto()
 * val restored = dto.toKeyChain()
 * // restored == keyChain
 * // restored.algorithm.id == "RS256"
 * ```
 */
fun KeyChainDto.toKeyChain(): KeyChain {
    // RSA-PSS 계열(PS256/PS384/PS512)은 "RSASSA-PSS" KeyFactory, RS 계열은 "RSA" KeyFactory 사용
    val keyFactoryAlgorithm = if (algorithmName.startsWith("PS")) "RSASSA-PSS" else "RSA"
    val keyFactory = KeyFactory.getInstance(keyFactoryAlgorithm)
    return KeyChain(
        algorithm = JwtConsts.signatureAlgorithmForId(algorithmName),
        keyPair = KeyPair(
            keyFactory.generatePublic(X509EncodedKeySpec(requireNotNull(publicKey) { "publicKey must not be null" })),
            keyFactory.generatePrivate(PKCS8EncodedKeySpec(requireNotNull(privateKey) { "privateKey must not be null" })),
        ),
        id = id,
        createdAt = createdAt,
        expiredTtl = Duration.ofMillis(expiredTtl)
    )
}
