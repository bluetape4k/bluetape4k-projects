package io.bluetape4k.aws.kotlin.ses

import io.bluetape4k.aws.kotlin.AbstractAwsTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel

abstract class AbstractKotlinSesTest: AbstractAwsTest() {

    companion object: KLoggingChannel() {
        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }

        const val domain = "example.com"
        const val senderEmail = "from-user@example.com"
        const val receiverEmail = "to-use@example.com"
    }
}
