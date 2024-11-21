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
        .onItem().invoke { it -> println(it) }
        .onOverflow().buffer(64)                    // 버퍼링된 것부터 소비, 버퍼링을 초과하면 예외 발생
        .subscribe()
        .withSubscriber(object: MultiSubscriber<String> {
            override fun onSubscribe(s: Flow.Subscription) {
                s.request(5)
                periodicallyRequest(s)
            }

            private fun periodicallyRequest(s: Flow.Subscription) {
                thread {
                    var count = 0
                    while (count++ < 5) {
                        Thread.sleep(5000)
                        println("\t\t 👋 request")
                        s.request(2)
                    }
                }
            }

            override fun onItem(item: String) {
                println("\t\t ➡️ $item ")
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
    thread {
        var count = 0
        while (count++ < 100 && !emitter.isCancelled) {
            emitter.emit("📦 $count")
            Thread.sleep(250)
        }
        emitter.complete()
    }
}
