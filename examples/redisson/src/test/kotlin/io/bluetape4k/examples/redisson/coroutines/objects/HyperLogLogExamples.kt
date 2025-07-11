package io.bluetape4k.examples.redisson.coroutines.objects

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * 대용량 데이터의 Count 를 확률 방식으로 계산하도록 한다
 *
 * 참고: [HyperLogLog](https://github.com/redisson/redisson/wiki/6.-distributed-objects/#69-hyperloglog)
 */
class HyperLogLogExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `RHyperLogLog 사용 예제`() = runSuspendIO {

        val hyperLog1 = redisson.getHyperLogLog<Int>(randomName())

        hyperLog1.addAsync(1).suspendAwait()
        hyperLog1.addAsync(1).suspendAwait()
        hyperLog1.addAsync(2).suspendAwait()
        hyperLog1.addAsync(3).suspendAwait()

        // 중복된 것은 제외하고 [1,2,3] 이다.
        hyperLog1.countAsync().suspendAwait() shouldBeEqualTo 3

        hyperLog1.addAllAsync(listOf(10, 20, 10, 30)).suspendAwait()
        hyperLog1.countAsync().suspendAwait() shouldBeEqualTo 6

        val hyperLog2 = redisson.getHyperLogLog<Int>(randomName())
        hyperLog2.addAsync(3).suspendAwait()
        hyperLog2.addAsync(4).suspendAwait()
        hyperLog2.addAsync(5).suspendAwait()

        val hyperLog3 = redisson.getHyperLogLog<Int>(randomName())
        hyperLog3.addAsync(3).suspendAwait()
        hyperLog3.addAsync(4).suspendAwait()
        hyperLog3.addAsync(5).suspendAwait()

        // 두 Log의 요소들을 merge 한다
        hyperLog2.mergeWithAsync(hyperLog3.name).suspendAwait()
        hyperLog2.countAsync().suspendAwait() shouldBeEqualTo 3

        // [1,2,3,10,20,30] + [3,4,5]
        hyperLog1.countWithAsync(hyperLog2.name).suspendAwait() shouldBeEqualTo 8

        hyperLog3.deleteAsync().suspendAwait()
        hyperLog2.deleteAsync().suspendAwait()
        hyperLog1.deleteAsync().suspendAwait()
    }
}
