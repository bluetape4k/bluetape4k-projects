package io.bluetape4k.captcha

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractCaptchaTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker
    }
}
