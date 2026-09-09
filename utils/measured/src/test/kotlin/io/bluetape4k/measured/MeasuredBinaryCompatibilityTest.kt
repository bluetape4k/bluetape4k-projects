package io.bluetape4k.measured

import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader

class MeasuredBinaryCompatibilityTest {

    @Test
    fun `기존 특수 연산자로 컴파일한 consumer가 HEAD의 JVM ABI에 연결된다`() {
        val fixtureDir = File(requireNotNull(System.getProperty("bluetape4k.measured.compat.fixture")))
        val fixtureClass = File(fixtureDir, "io/bluetape4k/measured/MeasuredLegacyBinaryConsumerKt.class")

        fixtureClass.exists().shouldBeTrue()
        String(fixtureClass.readBytes(), Charsets.ISO_8859_1)
            .contains("binarySizeDivTimeToDataRate")
            .shouldBeTrue()

        URLClassLoader(arrayOf(fixtureDir.toURI().toURL()), javaClass.classLoader).use { loader ->
            val legacyConsumer = loader.loadClass("io.bluetape4k.measured.MeasuredLegacyBinaryConsumerKt")
            @Suppress("UNCHECKED_CAST")
            val rate = legacyConsumer.getDeclaredMethod("legacyBinaryRate").invoke(null) as Measure<DataRate>

            (rate `in` DataRate.megaBytesPerSecond).shouldBeNear(1.0, 1e-10)
        }
    }
}
