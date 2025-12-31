package io.bluetape4k.avro

import org.apache.avro.file.CodecFactory

/**
 * Avro의 [CodecFactory]의 기본 인스턴스입니다.
 *
 * @see [CodecFactory]
 * @see [CodecFactory.zstandardCodec]
 * @see [CodecFactory.snappyCodec]
 * @see [CodecFactory.deflateCodec]
 */
val DEFAULT_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.zstandardCodec(3, true, true)
}
