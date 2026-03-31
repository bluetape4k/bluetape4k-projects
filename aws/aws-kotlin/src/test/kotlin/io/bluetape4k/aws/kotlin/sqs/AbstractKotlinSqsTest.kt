package io.bluetape4k.aws.kotlin.sqs

import io.bluetape4k.aws.kotlin.AbstractAwsTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel

abstract class AbstractKotlinSqsTest: AbstractAwsTest() {

    companion object: KLoggingChannel() {
        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }
}
