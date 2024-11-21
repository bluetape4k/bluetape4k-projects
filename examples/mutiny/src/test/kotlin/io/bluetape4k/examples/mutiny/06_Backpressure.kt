package io.bluetape4k.examples.mutiny

import io.bluetape4k.logging.KLogging
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.BackPressureStrategy
import io.smallrye.mutiny.subscription.MultiEmitter
import io.smallrye.mutiny.subscription.MultiSubscriber
import kotlinx.atomicfu.atomic
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.Flow
import kotlin.concurrent.thread

class BackpressureExamples {

    companion object: KLogging()

    @Test
    fun `01 Backpressure Drop`() {
        println("👀 Backpressure: Drop")

        // subscription이 최초 5 개만 받는다. 나머지 emit 되는 요소는 onOverlow 에서 drop 된다.
        val isCompleted = atomic(false)

        Multi.createFrom()
            .emitter({ emitter -> emitTooFast(emitter) }, BackPressureStrategy.ERROR)
            .onOverflow().invoke { _ -> print("🚨 ") }.drop()
            .subscribe()
            .withSubscriber(object: MultiSubscriber<String> {
                override fun onSubscribe(s: Flow.Subscription) {
                    s.request(5)
                }

                override fun onItem(item: String) {
                    print("$item ")
                }

                override fun onFailure(failure: Throwable) {
                    println("\n🔥 ${failure.message}")
                    isCompleted.value = true
                }

                override fun onCompletion() {
                    print("\n✅")
                    isCompleted.value = true
                }
            })

        await atMost Duration.ofSeconds(10) until { isCompleted.value }
    }

    @Test
    fun `02 Backpressure Buffer`() {
        println("👀 Backpressure: Buffer")

        // subscription이 3 개만 받는다. 나머지 emit 되는 요소는 onOverlow 에서 buffering 된다.
        val isCompleted = atomic(false)
        Multi.createFrom()
            .emitter({ emitter -> emitTooFast(emitter) }, BackPressureStrategy.ERROR)
            .onOverflow().invoke { _ -> print("🚨 ") }.buffer(10)
            .subscribe()
            .withSubscriber(object: MultiSubscriber<String> {
                override fun onSubscribe(s: Flow.Subscription) {
                    s.request(5)
                }

                override fun onItem(item: String) {
                    print("$item ")
                }

                override fun onFailure(failure: Throwable) {
                    println("\n🔥 ${failure.message}")
                    isCompleted.value = true
                }

                override fun onCompletion() {
                    print("\n✅")
                    isCompleted.value = true
                }
            })

        await atMost Duration.ofSeconds(10) until { isCompleted.value }
    }

    private fun emitTooFast(emitter: MultiEmitter<in String>) {
        thread(start = true) {
            var count = 0
            while (count++ < 100) {
                emitter.emit("📦")
                Thread.sleep(1)
            }
            emitter.complete()
        }
    }
}
