package io.bluetape4k.jackson3.binary

import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.jackson3.JacksonSerializer
import org.junit.jupiter.api.Test

class LegacySerializerCompatibilityTest {
    @Test
    fun `이전 serializer 클래스와 기본 생성자를 유지한다`() {
        listOf("CborJsonSerializer", "IonJsonSerializer", "SmileJsonSerializer").forEach { name ->
            Class.forName("io.bluetape4k.jackson3.binary.$name")
                .getConstructor().newInstance().shouldBeInstanceOf<JacksonSerializer>()
        }
    }
}
