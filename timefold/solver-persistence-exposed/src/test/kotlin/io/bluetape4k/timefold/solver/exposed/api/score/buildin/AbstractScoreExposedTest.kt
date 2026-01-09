package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractScoreExposedTest: AbstractExposedTest() {

    companion object: KLogging() {
        @JvmStatic
        protected val faker = Fakers.faker

        protected const val REPEAT_SIZE = 5
    }
}
