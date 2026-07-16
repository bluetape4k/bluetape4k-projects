package io.bluetape4k.avro.compat.issue754.kotlin

import io.bluetape4k.avro.AvroReflectSerializer
import io.bluetape4k.avro.deserialize

fun restoreLegacyAvro(serializer: AvroReflectSerializer, payload: ByteArray): String? =
    serializer.deserialize(payload)
