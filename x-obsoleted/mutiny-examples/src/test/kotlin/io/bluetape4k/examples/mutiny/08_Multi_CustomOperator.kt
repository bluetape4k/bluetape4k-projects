package io.bluetape4k.examples.mutiny

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor
import io.smallrye.mutiny.subscription.MultiSubscriber
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.random.Random

class CustomOperator {

    companion object: KLoggingChannel()

    @Test
    fun `01 Multi Custom Operator`() = runTest {
        log.debug { "👀 Custom operator, randomly drop items" }

        // 짝수이면 emit 시키고, 홀수이면 drop 시킨다.
        val list = Multi.createFrom().range(0, 20)
            .plug { RandomDropOperator(it) { item -> item % 2 == 0 } }
            .collect()
            .asList()
            .awaitSuspending()

        list shouldBeEqualTo (0..19).filter { it % 2 == 0 }.toList()
    }

    /**
     * upstream 에서 emit 된 요소를 랜덤하게 drop 시키는 Operator 입니다.
     *
     * @param upstream
     */
    class RandomDropOperator<T>(
        upstream: Multi<T>,
        private val predicate: (T) -> Boolean = { Random.nextBoolean() },
    ): AbstractMultiOperator<T, T>(upstream) {

        override fun subscribe(downstream: MultiSubscriber<in T>) {
            upstream.subscribe().withSubscriber(DropProcessor(downstream))
        }

        inner class DropProcessor(downstream: MultiSubscriber<in T>): MultiOperatorProcessor<T, T>(downstream) {
            override fun onItem(item: T) {
                if (predicate.invoke(item)) {
                    log.debug { "emit to downstream. item=$item" }
                    super.onItem(item)
                }
            }
        }
    }
}
