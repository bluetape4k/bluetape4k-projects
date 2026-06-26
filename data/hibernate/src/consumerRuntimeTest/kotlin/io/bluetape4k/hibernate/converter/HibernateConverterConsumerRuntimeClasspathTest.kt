package io.bluetape4k.hibernate.converter

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.converters.AESStringConverter
import io.bluetape4k.hibernate.converters.BZip2StringConverter
import io.bluetape4k.hibernate.converters.EncryptedStringConverterKeysets
import io.bluetape4k.hibernate.converters.ForyObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.KryoObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.LZ4KryoObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.LZ4StringConverter
import io.bluetape4k.hibernate.converters.SnappyKryoObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.SnappyStringConverter
import io.bluetape4k.hibernate.converters.ZstdForyObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.ZstdStringConverter
import io.bluetape4k.tink.aeadKeysetHandle
import io.bluetape4k.tink.keyset.toJsonKeyset
import org.junit.jupiter.api.Test

class HibernateConverterConsumerRuntimeClasspathTest {

    @Test
    fun `documented Tink encryption converter works from consumer runtime classpath`() {
        EncryptedStringConverterKeysets.configureAesKeyset(aeadKeysetHandle().toJsonKeyset())
        val converter = AESStringConverter()
        val original = "consumer-runtime-secret"

        val encrypted = converter.convertToDatabaseColumn(original)
        encrypted.shouldNotBeNull()
        encrypted shouldNotBeEqualTo original

        converter.convertToEntityAttribute(encrypted) shouldBeEqualTo original
    }

    @Test
    fun `documented Kryo and Fory byte-array converters work from consumer runtime classpath`() {
        val original = "consumer-runtime-payload"
        val converters = listOf(
            KryoObjectAsByteArrayConverter(),
            LZ4KryoObjectAsByteArrayConverter(),
            SnappyKryoObjectAsByteArrayConverter(),
            ForyObjectAsByteArrayConverter(),
            ZstdForyObjectAsByteArrayConverter(),
        )

        converters.forEach { converter ->
            val bytes = converter.convertToDatabaseColumn(original)
            bytes.shouldNotBeNull()

            converter.convertToEntityAttribute(bytes) shouldBeEqualTo original
        }
    }

    @Test
    fun `documented compression converters work from consumer runtime classpath`() {
        val original = "consumer-runtime-compression".repeat(8)
        val converters = listOf(
            BZip2StringConverter(),
            LZ4StringConverter(),
            SnappyStringConverter(),
            ZstdStringConverter(),
        )

        converters.forEach { converter ->
            val compressed = converter.convertToDatabaseColumn(original)
            compressed.shouldNotBeNull()
            compressed shouldNotBeEqualTo original

            converter.convertToEntityAttribute(compressed) shouldBeEqualTo original
        }
    }
}
