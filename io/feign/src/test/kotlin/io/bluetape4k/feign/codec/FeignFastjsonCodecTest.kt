package io.bluetape4k.feign.codec

import feign.codec.Decoder
import feign.codec.Encoder
import io.bluetape4k.logging.KLogging

class FeignFastjsonCodecTest: AbstractFeignCodecTest() {

    companion object: KLogging()

    override val encoder: Encoder = FeignFastjsonEncoder.INSTANCE
    override val decoder: Decoder = FeignFastjsonDecoder.INSTANCE

}
