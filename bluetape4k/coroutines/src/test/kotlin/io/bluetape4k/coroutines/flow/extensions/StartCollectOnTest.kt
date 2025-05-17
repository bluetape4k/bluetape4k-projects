package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class StartCollectOnTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    val four = newFixedThreadPoolContext(4, "four")
    val single = newSingleThreadContext("single")

    @AfterAll
    fun afterAll() {
        four.close()
        single.close()
    }

    @Test
    fun `use start collect on with custom dispatcher`() = runTest {
        flowRangeOf(1, 10)
            .log("four")
            .startCollectOn(four)
            .log("single")
            .flowOn(single)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    @Test
    fun `use start collect on with custom dispatcher and buffer`() = runTest {
        flowRangeOf(1, 10)
            .buffer(4)
            .log("four")
            .startCollectOn(four)
            .log("single")
            .flowOn(single)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }
}
