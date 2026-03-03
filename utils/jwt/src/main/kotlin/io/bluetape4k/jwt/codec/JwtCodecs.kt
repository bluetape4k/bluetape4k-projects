package io.bluetape4k.jwt.codec


object JwtCodecs {

    val Deflate by lazy { DeflateCodec() }
    val Gzip by lazy { GzipCodec() }
    val Lz4 by lazy { Lz4Codec() }
    val Snappy by lazy { SnappyCodec() }
    val Zstd by lazy { ZstdCodec() }
}
