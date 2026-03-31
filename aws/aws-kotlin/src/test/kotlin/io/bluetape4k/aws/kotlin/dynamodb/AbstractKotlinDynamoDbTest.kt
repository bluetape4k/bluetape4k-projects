package io.bluetape4k.aws.kotlin.dynamodb

import io.bluetape4k.aws.kotlin.AbstractAwsTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel

abstract class AbstractKotlinDynamoDbTest: AbstractAwsTest() {

    companion object: KLoggingChannel() {
        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 16, max: Int = 2048): String =
            Fakers.randomString(min, max)
    }
}
