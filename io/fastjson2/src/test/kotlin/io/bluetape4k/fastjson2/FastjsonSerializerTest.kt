package io.bluetape4k.fastjson2

import io.bluetape4k.json.JsonSerializer

class FastjsonSerializerTest: AbstractJsonSerializerTest() {

    override val serializer: JsonSerializer = FastjsonSerializer()
}
