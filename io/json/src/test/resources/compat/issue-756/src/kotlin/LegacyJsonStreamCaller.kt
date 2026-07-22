package io.bluetape4k.json.compat.issue756.kotlin

import io.bluetape4k.json.JsonSerializer
import java.io.ByteArrayOutputStream
import java.io.OutputStream

private class LegacyJsonKotlinImplementation: JsonSerializer {
    override fun serialize(graph: Any?): ByteArray = graph?.toString()?.encodeToByteArray() ?: byteArrayOf()

    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? =
        bytes?.takeIf { it.isNotEmpty() }?.decodeToString()?.let(clazz::cast)
}

fun main() {
    val serializer: JsonSerializer = LegacyJsonKotlinImplementation()
    check(serializer.serialize("json").decodeToString() == "json") { "legacy JSON call changed" }

    val method = JsonSerializer::class.java.getMethod(
        "serializeJsonToStream",
        Any::class.java,
        OutputStream::class.java,
    )
    check(method.isDefault) { "JSON stream method is not a default" }

    val target = ByteArrayOutputStream()
    val written = method.invoke(serializer, "json", target)
    check(written == 4) { "unexpected JSON stream write count" }
    check(target.toString(Charsets.UTF_8) == "json") { "unexpected JSON stream payload" }
    println("legacy-json-kotlin-stream-caller=PASS")
}
