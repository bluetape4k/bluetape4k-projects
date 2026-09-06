package io.bluetape4k.logging

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class MdcContextSnapshotTest {
    @BeforeEach
    fun setUp() {
        MDC.clear()
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `capture snapshot은 caller 변경과 분리된다`() {
        MDC.setContextMap(mapOf("request" to "before", "tenant" to "blue"))

        val snapshot = captureMdcContext()
        MDC.setContextMap(mapOf("request" to "after", "late" to "value"))

        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (snapshot as MutableMap<String, String>)["request"] = "mutated"
        }

        withMdcContext(snapshot) {
            MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("request" to "before", "tenant" to "blue")
        }

        snapshot shouldBeEqualTo mapOf("request" to "before", "tenant" to "blue")
        MDC.get("request") shouldBeEqualTo "after"
    }

    @Test
    fun `전달 map은 적용 전에 복사되어 block 안의 원본 변경과 분리된다`() {
        val context = linkedMapOf("request" to "before")

        withMdcContext(context) {
            context["request"] = "after"
            context["late"] = "value"

            MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("request" to "before")
        }

        context shouldBeEqualTo mapOf("request" to "after", "late" to "value")
    }

    @Test
    fun `빈 snapshot은 전체 context를 숨기고 원래 map을 복원한다`() {
        MDC.setContextMap(mapOf("seed" to "worker", "stale" to "value"))

        withMdcContext(emptyMap()) {
            MDC.getCopyOfContextMap().orEmpty() shouldBeEqualTo emptyMap<String, String>()
        }

        MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("seed" to "worker", "stale" to "value")
    }

    @Test
    fun `예외와 nested scope에서도 전체 context를 복원한다`() {
        MDC.setContextMap(mapOf("scope" to "outer", "seed" to "worker"))

        val failure = assertFailsWith<IllegalStateException> {
            withMdcContext(mapOf("scope" to "inner", "inner" to "yes")) {
                withMdcContext(mapOf("scope" to "nested")) {
                    MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("scope" to "nested")
                }
                MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("scope" to "inner", "inner" to "yes")
                error("expected")
            }
        }

        failure.message shouldBeEqualTo "expected"
        MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("scope" to "outer", "seed" to "worker")
    }
}
