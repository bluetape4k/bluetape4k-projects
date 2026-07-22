package io.bluetape4k.io.serializer.compat.issue756.kotlin

import io.bluetape4k.io.serializer.BinarySerializer
import java.io.ByteArrayOutputStream
import java.io.OutputStream

private class LegacyBinaryKotlinImplementation: BinarySerializer {
    override fun serialize(graph: Any?): ByteArray = graph?.toString()?.encodeToByteArray() ?: byteArrayOf()

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> deserialize(bytes: ByteArray?): T? =
        bytes?.takeIf { it.isNotEmpty() }?.decodeToString() as T?
}

fun main() {
    val serializer: BinarySerializer = LegacyBinaryKotlinImplementation()
    check(serializer.serialize("binary").decodeToString() == "binary") { "legacy binary call changed" }

    val method = BinarySerializer::class.java.getMethod(
        "serializeBinaryToStream",
        Any::class.java,
        OutputStream::class.java,
    )
    check(method.isDefault) { "binary stream method is not a default" }

    val target = ByteArrayOutputStream()
    val written = method.invoke(serializer, "binary", target)
    check(written == 6) { "unexpected binary stream write count" }
    check(target.toString(Charsets.UTF_8) == "binary") { "unexpected binary stream payload" }
    println("legacy-binary-kotlin-stream-caller=PASS")
}
