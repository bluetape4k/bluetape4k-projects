package io.bluetape4k.concurrent

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import kotlin.time.Duration.Companion.seconds

class FutureCompatibilityTest {

    @Test
    fun `join은 기존 원인 예외를 그대로 전파한다`() {
        val future = CompletableFuture.failedFuture<Int>(IllegalStateException("원인"))
        assertFailsWith<IllegalStateException> { future.join(1.seconds) }
            .message shouldBeEqualTo "원인"
        assertFailsWith<IllegalStateException> { future.join(1.seconds, -1) }
            .message shouldBeEqualTo "원인"
    }

    @Test
    fun `join 기본값은 정상 완료된 null에도 적용된다`() {
        CompletableFuture.completedFuture<Int?>(null).join(1.seconds, -1) shouldBeEqualTo -1
    }

    @Test
    fun `timeout의 Java 오버로드 JVM 서명을 유지한다`() {
        val support = Class.forName("io.bluetape4k.support.TimeoutSupportKt")
        support.getMethod(
            "asyncRunWithTimeout", Long::class.javaPrimitiveType, Function0::class.java,
        ).shouldNotBeNull()
        support.getMethod(
            "asyncRunWithTimeout", Long::class.javaPrimitiveType,
            ExecutorService::class.java, Function0::class.java,
        ).shouldNotBeNull()
    }
}
