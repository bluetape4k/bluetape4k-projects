package io.bluetape4k.examples.mutiny.backpressure

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.BackPressureStrategy
import io.smallrye.mutiny.subscription.MultiEmitter
import io.smallrye.mutiny.subscription.MultiSubscriber
import java.util.concurrent.Flow
import kotlin.concurrent.thread

fun main() {
    println("👀 Backpressure: Buffer")

    // subscription이 5 개만 받는다. 나머지 emit 되는 요소는 onOverlow 에서 buffering 된다.
    Multi.createFrom()
        .emitter<String>(
            { emitter -> emitTooFast(emitter) },
            BackPressureStrategy.ERROR
        )
        .onOverflow().invoke { _ -> print("🚨 ") }.buffer(32)  // 32개를 버퍼링하고, 버퍼를 넘으면 예외가 발생한다
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
            }

            override fun onCompletion() {
                print("\n✅")
            }
        })
}

private fun emitTooFast(emitter: MultiEmitter<in String>) {
    thread(start = true) {
        var count = 0
        while (count++ < 100) {
            emitter.emit("📦")
            Thread.sleep(100)
        }
        emitter.complete()
    }
}
