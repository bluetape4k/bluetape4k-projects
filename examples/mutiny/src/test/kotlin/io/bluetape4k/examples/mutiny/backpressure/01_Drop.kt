package io.bluetape4k.examples.mutiny.backpressure

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.BackPressureStrategy
import io.smallrye.mutiny.subscription.MultiEmitter
import io.smallrye.mutiny.subscription.MultiSubscriber
import java.util.concurrent.Flow
import kotlin.concurrent.thread


fun main() {
    println("👀 Backpressure: Drop")

    // subscription이 최초 5 개만 받는다. 나머지 emit 되는 요소는 onOverlow 에서 drop 된다.
    //
    Multi.createFrom()
        .emitter<String>(
            { emitter -> emitTooFast(emitter) },
            BackPressureStrategy.ERROR
        )
        .onOverflow().invoke { _ -> print("🚨 ") }.drop()  // backpressure 에 걸릴 때, drop 한다
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
        while (count++ < 50) {
            emitter.emit("📦")
            Thread.sleep(100)
        }
        emitter.complete()
    }
}
