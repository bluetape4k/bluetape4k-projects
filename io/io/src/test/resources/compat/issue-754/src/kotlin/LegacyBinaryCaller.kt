package io.bluetape4k.io.serializer.compat.issue754.kotlin

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.deserialize
import java.nio.ByteBuffer

fun restoreLegacyBinary(serializer: BinarySerializer, payload: ByteArray): String? =
    serializer.deserialize(ByteBuffer.wrap(payload))
