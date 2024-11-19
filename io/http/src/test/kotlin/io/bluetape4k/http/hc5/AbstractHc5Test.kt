package io.bluetape4k.http.hc5

import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.logging.KLogging

abstract class AbstractHc5Test: AbstractHttpTest() {

    companion object: KLogging() {
        @JvmStatic
        val urisToGet = listOf(
            "http://hc.apache.org",
            "http://hc.apache.org/httpcomponents-core-ga/",
            "http://hc.apache.org/httpcomponents-client-ga/"
        )
    }
}
