package io.bluetape4k.feign.codec

import feign.codec.Decoder
import feign.codec.Encoder
import io.bluetape4k.logging.KLogging

class JacksonCodec2Test: AbstractFeignCodecTest() {

    companion object: KLogging()

    override val encoder: Encoder = JacksonEncoder2.INSTANCE

    override val decoder: Decoder = JacksonDecoder2.INSTANCE

}
