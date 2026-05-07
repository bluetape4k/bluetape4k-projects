package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.core5.concurrent.Cancellable
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class OperationsTest {

    companion object : KLogging()

    @Test
    fun `CompletableFuture toCancellable - Cancellable 인스턴스 검증`() {
        val future = CompletableFuture<String>()
        val cancellable = future.toCancellable()

        cancellable.shouldNotBeNull()
        cancellable shouldBeInstanceOf Cancellable::class
    }

    @Test
    fun `CompletableFuture toCancellable - cancel 호출 가능 검증`() {
        val future = CompletableFuture<String>()
        val cancellable = future.toCancellable()

        cancellable.shouldNotBeNull()
        // cancel() 호출 시 예외 없이 완료되어야 함
        cancellable.cancel()
    }

    @Test
    fun `이미 완료된 Future toCancellable - Cancellable 인스턴스 검증`() {
        val future = CompletableFuture.completedFuture("result")
        val cancellable = future.toCancellable()

        cancellable.shouldNotBeNull()
        cancellable shouldBeInstanceOf Cancellable::class
    }

    @Test
    fun `이미 완료된 Future toCancellable - cancel 호출해도 예외 없음`() {
        val future = CompletableFuture.completedFuture("done")
        val cancellable = future.toCancellable()

        cancellable.shouldNotBeNull()
        // 이미 완료된 future에 cancel해도 예외가 발생하지 않아야 함
        cancellable.cancel()
    }

    @Test
    fun `Integer Future toCancellable - 타입 파라미터 무관하게 Cancellable 반환`() {
        val future = CompletableFuture<Int>()
        val cancellable = future.toCancellable()

        cancellable.shouldNotBeNull()
        cancellable shouldBeInstanceOf Cancellable::class
    }
}
