package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.JwtConsts.HEADER_ALGORITHM
import io.bluetape4k.jwt.JwtConsts.HEADER_KEY_ID
import io.bluetape4k.jwt.codec.DeflateCodec
import io.bluetape4k.jwt.codec.GzipCodec
import io.bluetape4k.jwt.codec.JwtCodecs
import io.bluetape4k.jwt.codec.Lz4Codec
import io.bluetape4k.jwt.codec.SnappyCodec
import io.bluetape4k.jwt.codec.ZstdCodec
import io.bluetape4k.jwt.keychain.KeyChain
import io.jsonwebtoken.Claims
import io.jsonwebtoken.CompressionCodecResolver
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SigningKeyResolverAdapter
import java.security.Key


internal val jwtParserCache = mutableMapOf<JwtProvider, JwtParser>()

internal fun JwtProvider.currentJwtParser(): JwtParser =
    jwtParserCache.getOrPut(this) {
        Jwts.parserBuilder()
            .setSigningKeyResolver(getSigningKeyResolverAdapter { kid -> findKeyChain(kid) })
            .setCompressionCodecResolver(defaltCompressionCodecResolver)
            .build()
    }

internal fun getSigningKeyResolverAdapter(findKeyChain: (String) -> KeyChain?): SigningKeyResolverAdapter {
    return object: SigningKeyResolverAdapter() {
        override fun resolveSigningKey(header: JwsHeader<*>, claim: Claims): Key {
            val keyChain = header[HEADER_KEY_ID]?.let { findKeyChain(it.toString()) }
                ?: throw SecurityException("Not found kid in jwt header.")

            val algorithm = header[HEADER_ALGORITHM] as? String
            if (algorithm != keyChain.algorithm.name) {
                throw SecurityException("Algorithm mismatch. jwt: $algorithm, keyChain: ${keyChain.algorithm.name}")
            }
            return keyChain.keyPair.private
        }
    }
}

internal val defaltCompressionCodecResolver = CompressionCodecResolver { header ->
    when (header.compressionAlgorithm?.uppercase()) {
        Lz4Codec.ALGORITHM     -> JwtCodecs.Lz4
        SnappyCodec.ALGORITHM  -> JwtCodecs.Snappy
        ZstdCodec.ALGORITHM    -> JwtCodecs.Zstd
        GzipCodec.ALGORITHM    -> JwtCodecs.Gzip
        DeflateCodec.ALGORITHM -> JwtCodecs.Deflate
        else                   -> null
    }
}
