package io.bluetape4k.fastjson2.model

import io.bluetape4k.fastjson2.AbstractJsonSerializerTest
import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.json.JsonSerializer

class FastjsonSerializerTest: AbstractJsonSerializerTest() {

    override val serializer: JsonSerializer = FastjsonSerializer()
}
