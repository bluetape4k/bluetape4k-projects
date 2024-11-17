package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.coroutines.tests.withSingleThread
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class ReplaySubjectTest {

    companion object: KLogging()

    @Test
    fun `replay collector`() = runTest {
        val subject = ReplaySubject<Int>(5)
        var replay1 = false
        var replay2 = false

        withSingleThread { dispatcher ->
            val job1 = launch(dispatcher) {
                subject.log("#1").collect()
            }.log("job1")

            val job2 = launch(dispatcher) {
                subject.log("#2").collect()
            }.log("job2")

            subject.emit(1)
            subject.emit(2)
            subject.emit(3)

            subject.complete()
            job1.join()
            job2.join()

            // replaySubject가 complete 되었습니다만, 모든 emit 된 요소 중 버퍼링된 요소를 replay 합니다.
            subject
                .onStart { replay1 = true }
                .log("#3")
                .collect()
            subject
                .onStart { replay2 = true }
                .log("#4")
                .collect()

            log.debug { "All Done." }
            replay1.shouldBeTrue()
            replay2.shouldBeTrue()
        }
    }
}
