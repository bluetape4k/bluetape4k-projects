package io.bluetape4k.feign

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.http.HttpbinServer

abstract class AbstractFeignTest {

    companion object: KLogging() {
        @JvmStatic
        protected val httpbinServer by lazy { HttpbinServer.Launcher.httpbin }

        @JvmStatic
        protected val httpbinBaseUrl: String by lazy { httpbinServer.url }
    }

    protected val testBaseUrl: String
        get() = httpbinBaseUrl

}
