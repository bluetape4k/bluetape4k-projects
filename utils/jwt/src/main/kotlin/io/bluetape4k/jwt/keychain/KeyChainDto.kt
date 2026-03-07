package io.bluetape4k.jwt.keychain

import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.jwt.JwtConsts
import io.bluetape4k.jwt.keychain.KeyChainDto.Companion.serializer
import java.io.Serializable
import java.security.KeyPair
import java.time.Duration

/**
 * [KeyChain]의 직렬화용 DTO입니다.
 *
 * ## 동작/계약
 * - [algorithmName]은 서명 알고리즘의 ID 문자열입니다 (예: "RS256").
 * - [publicKey], [privateKey]는 JDK 직렬화로 변환된 바이트 배열입니다.
 */
data class KeyChainDto(
    val id: String,
    val algorithmName: String,
    val createdAt: Long,
    val expiredTtl: Long,
): Serializable {
    companion object {
        internal val serializer = BinarySerializers.LZ4Jdk
    }

    var publicKey: ByteArray? = null
    var privateKey: ByteArray? = null
}

/**
 * [KeyChain]을 [KeyChainDto]로 변환합니다.
 */
fun KeyChain.toDto(): KeyChainDto =
    KeyChainDto(
        id = id,
        algorithmName = algorithm.id,
        createdAt = createdAt,
        expiredTtl = expiredTtl,
    ).apply {
        publicKey = serializer.serialize(keyPair.public)
        privateKey = serializer.serialize(keyPair.private)
    }

/**
 * [KeyChainDto]를 [KeyChain]으로 변환합니다.
 */
fun KeyChainDto.toKeyChain(): KeyChain =
    KeyChain(
        algorithm = JwtConsts.signatureAlgorithmForId(algorithmName),
        keyPair = KeyPair(
            serializer.deserialize(publicKey),
            serializer.deserialize(privateKey),
        ),
        id = id,
        createdAt = createdAt,
        expiredTtl = Duration.ofMillis(expiredTtl)
    )
