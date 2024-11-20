package io.bluetape4k.jwt.keychain

import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.jwt.keychain.KeyChainDto.Companion.serializer
import io.bluetape4k.support.unsafeLazy
import io.jsonwebtoken.SignatureAlgorithm
import java.io.Serializable
import java.security.KeyPair
import java.time.Duration

data class KeyChainDto(
    val id: String,
    val algorithmName: String,
    val createdAt: Long,
    val expiredTtl: Long,
): Serializable {
    companion object {
        internal val serializer by unsafeLazy { BinarySerializers.LZ4Jdk }
    }

    var publicKey: ByteArray? = null
    var privateKey: ByteArray? = null
}

fun KeyChain.toDto(): KeyChainDto =
    KeyChainDto(
        id = id,
        algorithmName = algorithm.name,
        createdAt = createdAt,
        expiredTtl = expiredTtl,
    ).apply {
        publicKey = serializer.serialize(keyPair.public)
        privateKey = serializer.serialize(keyPair.private)
    }

fun KeyChainDto.toKeyChain(): KeyChain =
    KeyChain(
        algorithm = SignatureAlgorithm.forName(algorithmName),
        keyPair = KeyPair(
            serializer.deserialize(publicKey),
            serializer.deserialize(privateKey),
        ),
        id = id,
        createdAt = createdAt,
        expiredTtl = Duration.ofMillis(expiredTtl)
    )
