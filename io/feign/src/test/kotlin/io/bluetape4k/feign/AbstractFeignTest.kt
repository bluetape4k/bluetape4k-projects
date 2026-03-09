package io.bluetape4k.feign

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.http.HttpbinHttp2Server

abstract class AbstractFeignTest {

    companion object: KLogging() {
        @JvmStatic
        protected val httpbinServer by lazy { HttpbinHttp2Server.Launcher.httpbinHttp2 }

        @JvmStatic
        protected val httpbinBaseUrl: String by lazy { httpbinServer.url }
    }

    protected val testBaseUrl: String
        get() = httpbinBaseUrl

}
