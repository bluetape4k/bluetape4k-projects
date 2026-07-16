package io.bluetape4k.json.compat.issue754.kotlin

import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize

fun restoreLegacyJson(serializer: JsonSerializer, payload: ByteArray): String? =
    serializer.deserialize(payload)
