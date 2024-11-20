package io.bluetape4k.jwt.codec

import io.bluetape4k.support.unsafeLazy

object JwtCodecs {

    val Deflate by unsafeLazy { DeflateCodec() }
    val Gzip by unsafeLazy { GzipCodec() }
    val Lz4 by unsafeLazy { Lz4Codec() }
    val Snappy by unsafeLazy { SnappyCodec() }
    val Zstd by unsafeLazy { ZstdCodec() }
}
