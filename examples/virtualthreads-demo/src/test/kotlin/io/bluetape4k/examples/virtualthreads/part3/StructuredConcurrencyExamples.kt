package io.bluetape4k.examples.virtualthreads.part3

import io.bluetape4k.concurrent.virtualthread.StructuredSubtask
import io.bluetape4k.concurrent.virtualthread.structuredTaskScopeFailFast
import io.bluetape4k.concurrent.virtualthread.structuredTaskScopeFirstSuccess
import io.bluetape4k.examples.virtualthreads.AbstractVirtualThreadTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.StructuredTaskScope

class StructuredConcurrencyExamples: AbstractVirtualThreadTest() {

    companion object: KLoggingChannel()

    data class Pasta(val name: String = "Spaghetti")
    data class Sauce(val name: String = "Tomato")
    data class Dish(val pasta: Pasta, val sauce: Sauce)

    fun preparePasta(): Pasta {
        println("prepare Pasta")
        return Pasta()
    }

    fun makeSaurce(): Sauce {
        println("make Sauce")
        return Sauce()
    }

    fun serveDish(dish: Dish) {
        println("😍 hear you are, $dish")
    }

    /**
     * 비동기 코드를 병렬로 실행하고, 모든 작업이 성공적으로 완료되면 결과를 반환합니다.
     */
    fun prepareDish(): Dish = structuredTaskScopeFailFast { scope ->
        println("prepare Dish ...")
        val pasta: StructuredSubtask<Pasta> = scope.fork {
            Thread.sleep(100)
            preparePasta()
        }
        val sauce = scope.fork {
            Thread.sleep(200)
            makeSaurce()
        }
        Thread.sleep(5)

        scope.join().throwIfFailed()

        pasta.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.SUCCESS
        sauce.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.SUCCESS

        println("complete Dish")
        Dish(pasta.get(), sauce.get())
    }

    fun cookPasta() {
        val dish = prepareDish()
        serveDish(dish)
    }

    @Test
    fun `Structured Task Scope 안에서 병렬 작업`() {
        cookPasta()
    }

    @Test
    fun `structured task scope on success`() {
        // Subtask 들 중 하나라도 성공하면, 나머지 Subtask 들은 취소하고, 결과를 반환합니다.
        // 만약 성공한 것이 없다면 ExecutionException 을 반환합니다.
        val pasta = structuredTaskScopeFirstSuccess { scope ->

            val subtask1 = scope.fork {
                Thread.sleep(200)
                preparePasta()
            }

            val subtask2 = scope.fork {
                Thread.sleep(100)
                preparePasta()
            }

            Thread.sleep(5)
            subtask1.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.UNAVAILABLE
            subtask2.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.UNAVAILABLE

            scope.join()

            subtask1.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.UNAVAILABLE
            subtask2.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.SUCCESS

            scope.result { RuntimeException(it) }
        }
        println("pasta: $pasta")
    }
}
