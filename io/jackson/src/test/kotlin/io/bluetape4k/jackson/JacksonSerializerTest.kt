package io.bluetape4k.jackson

import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging

class JacksonSerializerTest: AbstractJsonSerializerTest() {

    companion object: KLogging()

    override val serializer: JsonSerializer = JacksonSerializer()

}
